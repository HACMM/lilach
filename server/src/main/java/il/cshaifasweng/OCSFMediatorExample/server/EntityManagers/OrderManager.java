package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import Request.CustomOrder;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import org.hibernate.SessionFactory;
import Request.NewOrderRequest;

import java.time.LocalDateTime;
import java.util.List;

public class OrderManager extends BaseManager {

    public OrderManager(SessionFactory sf) { super(sf); }

    public Order getById(int id) {
        return read(s -> s.get(Order.class, id));
    }

    public Order create(Order o) {
        return write(s -> { s.persist(o); s.flush(); return o; });
    }

    public Order update(Order o) {
        return write(s -> (Order) s.merge(o));
    }

    /** List orders by UserAccount (NOT 'user'). */
    public List<Order> listByUserAccount(int userAccountId) {
        return read(s -> s.createQuery(
                        "select o from Order o where o.userAccount.id = :u",
                        Order.class
                )
                .setParameter("u", userAccountId)
                .getResultList());
    }

    /** List orders by Branch. */
    public List<Order> listByBranch(int branchId) {
        return read(s -> s.createQuery(
                        "select o from Order o where o.branch.id = :b",
                        Order.class
                )
                .setParameter("b", branchId)
                .getResultList());
    }

    /** Optional: list all orders. */
    public List<Order> listAll() {
        return read(s -> s.createQuery("from Order", Order.class).getResultList());
    }

    /** List all orders with minimal details for table view (fast). */
    public List<Order> listAllWithDetails() {
        return read(s -> {
            // Fetch Order, UserAccount, Branch, and OrderLines (but NOT Items)
            // This is much faster than fetching all Items
            List<Order> orders = s.createQuery(
                            "select distinct o from Order o " +
                                    "left join fetch o.userAccount ua " +
                                    "left join fetch o.branch b " +
                                    "left join fetch o.orderLines ol ",
                            Order.class
                    )
                    .getResultList();

            System.out.println("Found " + orders.size() + " total orders");

            // Initialize orderLines collections (already fetched, just ensure they're accessible)
            for (Order order : orders) {
                if (order.getOrderLines() == null) {
                    order.setOrderLines(new java.util.ArrayList<>());
                }
                // Don't initialize Items - we don't need them for the table view
            }

            return orders;
        });
    }

    public Order createFromRequest(NewOrderRequest req) {
        return write(s -> {
            // 1) Load user
            System.out.println("OrderManager: Creating order for userId=" + req.userId);
            UserAccount user = s.get(UserAccount.class, req.userId);
            if (user == null) {
                System.err.println("ERROR: User with ID " + req.userId + " not found!");
                throw new RuntimeException("User not found");
            }
            System.out.println("OrderManager: Loaded user: " + user.getLogin() + " (userId=" + user.getUserId() + ")");

            // 2) Create order and set core fields
            Order order = new Order(user);
            order.setGreeting(req.greeting);
            order.setDeliveryType(req.deliveryType);
            order.setDeliveryDateTime(
                    req.deliveryDateTime != null ? req.deliveryDateTime : LocalDateTime.now()
            );
            order.setStatus("Pending");

            if (req.paymentMethod != null) {
                order.setPaymentMethod(req.paymentMethod);
            }

            // 3) Branch selection
            Branch branchToSet = null;
            if (req.branchId != null) {
                branchToSet = s.get(Branch.class, req.branchId);
                if (branchToSet != null) {
                    System.out.println("Set branch from request: " + branchToSet.getName());
                }
            } else if (user.getBranch() != null) {
                // Use user's default branch if not explicitly provided
                branchToSet = user.getBranch();
                System.out.println("Using user's default branch: " + branchToSet.getName());
            }

            // Fallback: first branch in DB
            if (branchToSet == null) {
                List<Branch> branches = s.createQuery("from Branch", Branch.class)
                        .setMaxResults(1)
                        .getResultList();
                if (!branches.isEmpty()) {
                    branchToSet = branches.get(0);
                    System.out.println("Using fallback branch: " + branchToSet.getName());
                } else {
                    System.err.println("WARNING: No branches available in database!");
                }
            }

            if (branchToSet != null) {
                order.setBranch(branchToSet);
            }

            // 4) Delivery details
            if ("Delivery".equalsIgnoreCase(req.deliveryType)) {
                Address addr = new Address(req.city, req.street, req.building);
                order.setDeliveryAddress(addr);
                order.setRecipientName(req.recipientName);
                order.setRecipientPhone(req.recipientPhone);
                order.setDeliveryFee(req.deliveryFee != null ? req.deliveryFee : 20.0);
            } else {
                order.setDeliveryFee(0.0);
            }

            // 5) Order lines
            System.out.println("Adding " + req.lines.size() + " order lines...");
            for (NewOrderRequest.Line line : req.lines) {
                int itemIdInt = (int) line.itemId;  // Item.id is int
                Item item = s.get(Item.class, itemIdInt);
                if (item == null) {
                    System.err.println("WARNING: Item with ID " + line.itemId +
                            " (as int: " + itemIdInt + ") not found, skipping...");
                    continue;
                }
                int qty = Math.max(1, line.qty);
                double unitPrice = line.unitPrice; // snapshot from client
                order.addLine(item, qty, unitPrice);
                System.out.println("  - Added: " + item.getName() + " x" + qty + " @ " + unitPrice);
            }

            if (order.getOrderLines().isEmpty()) {
                System.err.println("ERROR: No valid order lines!");
                throw new RuntimeException("No valid items in order");
            }

            // 6) Total + persist
            order.recomputeTotal();
            System.out.println("Order total: " + order.getTotalPrice());

            s.persist(order);
            s.flush();      // ensure ID is generated
            s.refresh(user); // update user's orderSet if needed

            System.out.println("Order persisted with ID: " + order.getId() +
                    " for user ID: " + user.getUserId());

            // BaseManager.write(...) will commit the transaction
            return order;
        });
    }

    public List<Order> listWithDetailsByUser(int userAccountId) {
        return read(s -> {
            // DEBUG: show all orders in DB (optional, can remove later)
            List<Order> allOrders = s.createQuery("from Order", Order.class).getResultList();
            System.out.println("DEBUG: Total orders in database: " + allOrders.size());
            for (Order o : allOrders) {
                UserAccount ua = o.getUserAccount();
                int orderUserId = (ua != null) ? ua.getUserId() : -1;
                System.out.println("DEBUG: Order ID=" + o.getId() +
                        ", User ID=" + orderUserId +
                        ", Status=" + o.getStatus());
            }

            // main query: fetch everything we need in one shot
            List<Order> orders = s.createQuery(
                            "select distinct o from Order o " +
                                    "left join fetch o.orderLines " +
                                    "left join fetch o.userAccount " +
                                    "left join fetch o.branch " +
                                    "where o.userAccount.userId = :u",
                            Order.class
                    )
                    .setParameter("u", userAccountId)
                    .getResultList();

            System.out.println("Found " + orders.size() + " orders for user " + userAccountId);

            // Make sure nested lazy stuff is initialized (items on each line)
            for (Order order : orders) {
                if (order.getOrderLines() != null) {
                    order.getOrderLines().forEach(line -> {
                        if (line.getItem() != null) {
                            // touch a simple property to force initialization
                            line.getItem().getName();
                        }
                    });
                }
            }

            return orders;
        });
    }


    public Order createCustomOrder(CustomOrder req) {
        return write(s -> {
            // 1) Load the user
            UserAccount user = s.get(UserAccount.class, req.getUserId());
            if (user == null) {
                System.err.println("ERROR: User with ID " + req.getUserId() + " not found!");
                throw new RuntimeException("User not found");
            }
            System.out.println("Loaded user: " + user.getLogin());

            // 2) Create a regular Order for the custom design request
            Order order = new Order(user);
            order.setStatus("Pending");
            order.setDeliveryType("Pickup"); // Default for custom orders
            order.setDeliveryDateTime(LocalDateTime.now().plusDays(7)); // Default: 7 days from now
            order.setDeliveryFee(0.0);

            // Store custom design details in the greeting field
            StringBuilder greetingBuilder = new StringBuilder();
            greetingBuilder.append("🎨 CUSTOM DESIGN REQUEST 🎨\n");
            greetingBuilder.append("Type: ").append(req.getItemType());
            if (req.getColor() != null && !req.getColor().isEmpty()) {
                greetingBuilder.append("\nColor: ").append(req.getColor());
            }
            if (req.getMinPrice() != null || req.getMaxPrice() != null) {
                greetingBuilder.append("\nPrice range: ");
                if (req.getMinPrice() != null) greetingBuilder.append(req.getMinPrice());
                greetingBuilder.append(" - ");
                if (req.getMaxPrice() != null) greetingBuilder.append(req.getMaxPrice());
                greetingBuilder.append(" ₪");
            }
            if (req.getNotes() != null && !req.getNotes().isEmpty()) {
                greetingBuilder.append("\n\nSpecial Notes:\n").append(req.getNotes());
            }

            String greeting = greetingBuilder.toString();

            // Safety: truncate to fit DB column (adjust if your column is different)
            final int MAX_GREETING_LENGTH = 255;

            if (greeting.length() > MAX_GREETING_LENGTH) {
                System.out.println("Greeting too long (" + greeting.length() +
                        "), truncating to " + MAX_GREETING_LENGTH + " characters");
                greeting = greeting.substring(0, MAX_GREETING_LENGTH);
            }

            order.setGreeting(greeting);


            // 3) Set branch (use user's branch or first available)
            Branch branchToSet = null;
            if (user.getBranch() != null) {
                branchToSet = user.getBranch();
            } else {
                List<Branch> branches = s.createQuery("from Branch", Branch.class)
                        .setMaxResults(1)
                        .getResultList();
                if (!branches.isEmpty()) {
                    branchToSet = branches.get(0);
                }
            }
            if (branchToSet != null) {
                order.setBranch(branchToSet);
                System.out.println("Set branch on custom order: " + branchToSet.getName());
            }

            // 4) Set payment method if user has one
            if (user.getDefaultPaymentMethod() != null) {
                order.setPaymentMethod(user.getDefaultPaymentMethod());
            }

            // 5) Estimate price
            double estimatedPrice = 0.0;
            if (req.getMinPrice() != null && req.getMaxPrice() != null) {
                estimatedPrice = (req.getMinPrice() + req.getMaxPrice()) / 2.0;
            } else if (req.getMinPrice() != null) {
                estimatedPrice = req.getMinPrice();
            } else if (req.getMaxPrice() != null) {
                estimatedPrice = req.getMaxPrice();
            } else {
                estimatedPrice = 50.0; // Default price for custom design
            }

            // 6) Persist CustomItem entity with the design details
            CustomItem customItem = new CustomItem(
                    user,
                    req.getItemType(),
                    req.getMinPrice(),
                    req.getMaxPrice(),
                    req.getColor(),
                    req.getNotes()
            );
            s.persist(customItem);
            System.out.println("Created CustomItem with ID: " + customItem.getId());

            // 7) Create a placeholder order line using the first available item from catalog
            List<Item> items = s.createQuery("from Item", Item.class)
                    .setMaxResults(1)
                    .getResultList();

            if (!items.isEmpty()) {
                Item placeholderItem = items.get(0);
                double linePrice = estimatedPrice > 0 ? estimatedPrice : placeholderItem.getPrice();
                order.addLine(placeholderItem, 1, linePrice);
                System.out.println("Added placeholder order line: " +
                        placeholderItem.getName() + " @ " + linePrice);
            } else {
                // If no items exist, just set total directly
                order.setTotalPrice(estimatedPrice);
                System.out.println("WARNING: No items in catalog, creating order without order lines");
            }

            // 8) Recompute total and persist order
            order.recomputeTotal();
            System.out.println("Custom design order total: " + order.getTotalPrice());

            s.persist(order);
            s.flush();
            s.refresh(user);
            System.out.println("Custom design order persisted with ID: " + order.getId() +
                    " for user ID: " + user.getUserId());

            return order;
        });
    }


    public static class CancelOrderResult {
        private final Order order;
        private final UserAccount user;
        private final double orderTotal;
        private final double refundAmount;
        private final double newCreditBalance;
        private final String refundType;
        private final long hoursUntilDelivery;

        public CancelOrderResult(Order order,
                                 UserAccount user,
                                 double orderTotal,
                                 double refundAmount,
                                 double newCreditBalance,
                                 String refundType,
                                 long hoursUntilDelivery) {
            this.order = order;
            this.user = user;
            this.orderTotal = orderTotal;
            this.refundAmount = refundAmount;
            this.newCreditBalance = newCreditBalance;
            this.refundType = refundType;
            this.hoursUntilDelivery = hoursUntilDelivery;
        }

        public Order getOrder() { return order; }
        public UserAccount getUser() { return user; }
        public double getOrderTotal() { return orderTotal; }
        public double getRefundAmount() { return refundAmount; }
        public double getNewCreditBalance() { return newCreditBalance; }
        public String getRefundType() { return refundType; }
        public long getHoursUntilDelivery() { return hoursUntilDelivery; }
    }


    public CancelOrderResult cancelOrder(int orderId, int userId) {
        return write(session -> {
            // 1) Load the order
            Order order = session.get(Order.class, orderId);
            if (order == null) {
                throw new RuntimeException("Order not found");
            }

            // 2) Verify the order belongs to the user
            if (order.getUserAccount() == null ||
                    order.getUserAccount().getUserId() != userId) {
                throw new RuntimeException("You don't have permission to cancel this order");
            }

            // 3) Check status
            if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
                throw new RuntimeException("Order is already cancelled");
            }
            if ("Delivered".equalsIgnoreCase(order.getStatus())) {
                throw new RuntimeException("Cannot cancel an order that has already been delivered");
            }

            // 4) Calculate refund
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deliveryTime = order.getDeliveryDateTime();
            if (deliveryTime == null) {
                deliveryTime = order.getCreatedAt() != null
                        ? order.getCreatedAt().plusDays(1)
                        : now.plusDays(1);
            }

            long hoursUntilDelivery = java.time.Duration
                    .between(now, deliveryTime)
                    .toHours();

            double orderTotal = order.getTotalPrice();
            double refundAmount;
            String refundType;

            if (hoursUntilDelivery >= 3) {
                refundAmount = orderTotal;
                refundType = "Full credit (100%)";
            } else if (hoursUntilDelivery >= 1) {
                refundAmount = orderTotal * 0.5;
                refundType = "Partial credit (50%)";
            } else {
                refundAmount = 0.0;
                refundType = "No credit (cancelled < 1 hour before delivery)";
            }

            // 5) Update order + user
            order.setStatus("Cancelled");
            session.update(order);

            UserAccount user = order.getUserAccount();
            user.addStoreCredit(refundAmount);
            session.update(user);

            System.out.println("Order " + orderId + " cancelled. Refund: " +
                    refundAmount + " ₪. New credit balance: " + user.getStoreCredit() + " ₪");

            return new CancelOrderResult(
                    order,
                    user,
                    orderTotal,
                    refundAmount,
                    user.getStoreCredit(),
                    refundType,
                    hoursUntilDelivery
            );
        });
    }


}


