package il.cshaifasweng.OCSFMediatorExample.server;

import Request.*;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ItemManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ComplaintManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.OrderManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

public class SimpleServer extends AbstractServer {
    private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();
    private final SessionFactory sessionFactory;

    // managers
    private ItemManager itemManager = null;
    private ComplaintManager complaintManager = null;
    private OrderManager orderManager = null;

    public SimpleServer(int port) {
        super(port);
        this.sessionFactory = DbConnector.getInstance().getSessionFactory();
        this.itemManager = new ItemManager(sessionFactory);
        this.complaintManager = new ComplaintManager(sessionFactory);
        this.orderManager = new OrderManager(sessionFactory);
    }

    private PublicUser toPublicUser(UserAccount u) {
        LocalDate exp = null;
        Object raw = u.getSubscriptionExpirationDate();
        if (raw instanceof LocalDate) exp = (LocalDate) raw;

        Integer branchId = (u.getBranch() != null) ? u.getBranch().getId() : null;

        return new PublicUser(
                u.getUserId(),
                u.getLogin(),
                u.getName(),
                u.getEmail(),
                u.getIdNumber(),
                u.getRole(),
                u.getUserBranchType(),
                branchId,
                u.isSubscriptionUser(),
                exp,
                u.getDefaultPaymentMethod()
        );
    }



    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        String msgString = msg.toString();

        if (msgString.startsWith("#warning")) {
            Warning warning = new Warning("Warning from server!");
            try {
                client.sendToClient(warning);
                System.out.format("Sent warning to client %s\n", client.getInetAddress().getHostAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (msgString.startsWith("add client")) {
            SubscribedClient connection = new SubscribedClient(client);
            SubscribersList.add(connection);
            try {
                client.sendToClient("showCatalog");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else if (msgString.startsWith("getCatalog")) {
            List<Item> items = itemManager.GetItemList(new ArrayList<>());
            System.out.println("Catalog received");
            System.out.println(items);
            try {
                client.sendToClient(items);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (msgString.startsWith("#getAllBranches") || msgString.equals("getAllBranches")) {
            System.out.println("Received #getAllBranches request");
            try (org.hibernate.Session session = sessionFactory.openSession()) {
                java.util.List<Branch> branches =
                        session.createQuery("from Branch", Branch.class)
                                .getResultList();

                System.out.println("Found " + branches.size() + " branches in database");
                if (branches.isEmpty()) {
                    System.out.println("WARNING: No branches found in database! Branches may need to be initialized.");
                }

                client.sendToClient(new Message("branch list", branches));
                System.out.println("Sent branch list to client");
            } catch (Exception e) {
                System.err.println("Error fetching branches: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("branch list error", e.getMessage())); }
                catch (Exception ignore) {}
            }

        } else if (msg instanceof Item) {
            Item updatedItem = (Item) msg;
            System.out.println("Received updated item: " + updatedItem.getName() + " | New price: " + updatedItem.getPrice());
            boolean success = itemManager.EditItem(updatedItem);
            if (success) {
                System.out.println("Item edited successfully");
                List<Item> updatedCatalog = itemManager.GetItemList(new ArrayList<>());
                try {
                    // Send updated catalog to the client that made the change
                    client.sendToClient(updatedCatalog);
                    // Broadcast updated catalog to all subscribed clients
                    sendToAllClients(updatedCatalog);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Item could not be edited");
            }

        } else if (msg instanceof UserAccount) {
            // (no-op / future handling)

        }
        else if (msg instanceof ResolveComplaint) {
            ResolveComplaint req = (ResolveComplaint) msg;
            
            try (org.hibernate.Session s = sessionFactory.openSession()) {
                org.hibernate.Transaction tx = s.beginTransaction();

                Complaint c = s.get(Complaint.class, req.complaintId);
                if (c == null) {
                    tx.rollback();
                    try { client.sendToClient(new Message("resolveComplaintError", "Complaint not found")); }
                    catch (IOException ignored) {}
                    return;
                }

                // Set manager account if managerUserId is provided
                if (req.managerUserId != null) {
                    UserAccount manager = s.get(UserAccount.class, req.managerUserId);
                    if (manager != null) {
                        c.setManagerAccount(manager);
                    }
                }

                c.setResponse(req.response);
                c.setCompensation(req.compensation != null ? req.compensation : 0.0);
                c.setRespondedAt(java.time.LocalDateTime.now());
                
                // Update status based on whether complaint is resolved
                // If compensation > 0, approve; otherwise reject
                if (req.compensation != null && req.compensation > 0) {
                    c.setStatus(ComplaintStatus.Approved);
                    c.addEvent(ComplaintStatus.Approved, req.response, c.getManagerAccount());
                } else {
                    c.setStatus(ComplaintStatus.Rejected);
                    c.addEvent(ComplaintStatus.Rejected, req.response, c.getManagerAccount());
                }

                s.update(c);
                tx.commit();

                // server-side email to the customer
                try {
                    EmailSender.sendEmail(
                            "Your complaint has been resolved 💐",
                            "Hello " + c.getClientName() + ",\n\n" +
                                    "We responded to your complaint:\n\"" + c.getDescription() + "\"\n\n" +
                                    "Response: " + c.getResponse() + "\n" +
                                    ((c.getCompensation() != null && c.getCompensation() > 0)
                                            ? "Compensation: " + String.format("%.2f₪", c.getCompensation()) + "\n\n"
                                            : "\n") +
                                    "Thank you for your patience.\nFlowerShop Team",
                            c.getClientEmail()
                    );
                } catch (Exception mailEx) {
                    // log only; don't fail the request because of email issues
                    mailEx.printStackTrace();
                }

                // Send success message and refresh complaints list
                try { 
                    client.sendToClient(new Message("complaintResolved", c));
                    // Also send updated list
                    List<Complaint> allComplaints = complaintManager.listAll();
                    client.sendToClient(allComplaints);
                } catch (IOException ignored) {}

            } catch (Exception e) {
                e.printStackTrace();
                try { client.sendToClient(new Message("resolveComplaintError", "Server error")); }
                catch (IOException ignored) {}
            }

        } else if (msg instanceof NewComplaint) {
            NewComplaint req = (NewComplaint) msg;
            System.out.println("Received NewComplaint: branchId=" + req.branchId + ", clientName=" + req.clientName);
            
            try (org.hibernate.Session s = sessionFactory.openSession()) {
                org.hibernate.Transaction tx = s.beginTransaction();
                
                try {
                    // Load branch
                    Branch branch = null;
                    if (req.branchId != null) {
                        branch = s.get(Branch.class, req.branchId);
                        if (branch == null) {
                            System.err.println("ERROR: Branch with id " + req.branchId + " not found!");
                            tx.rollback();
                            try { client.sendToClient(new Message("newComplaintError", "Branch not found")); }
                            catch (IOException ignored) {}
                            return;
                        }
                        System.out.println("Loaded branch: " + branch.getName());
                    }
                    
                    // Create complaint
                    Complaint complaint = new Complaint(branch, req.orderNumber, req.clientName, 
                                                        req.clientEmail, req.description);
                    
                    System.out.println("Created complaint object, persisting...");
                    
                    // Save complaint (persist within this transaction)
                    s.persist(complaint);
                    s.flush();
                    System.out.println("Complaint persisted, flushing...");
                    tx.commit();
                    System.out.println("Transaction committed. Complaint ID: " + complaint.getComplaintId());
                    
                    // Send confirmation email
                    try {
                        EmailSender.sendEmail(
                                "Complaint Received 💐",
                                "Dear " + req.clientName + ",\n\n" +
                                        "We have received your complaint and will respond within 24 hours.\n\n" +
                                        "Thank you,\nFlowerShop Team",
                                req.clientEmail
                        );
                    } catch (Exception mailEx) {
                        System.err.println("Email sending failed (non-critical): " + mailEx.getMessage());
                        mailEx.printStackTrace();
                    }
                    
                    // Send success message
                    try { client.sendToClient(new Message("newComplaintOk", "Complaint submitted successfully")); }
                    catch (IOException ignored) {}
                    
                } catch (Exception e) {
                    System.err.println("ERROR in NewComplaint handler: " + e.getMessage());
                    e.printStackTrace();
                    if (tx != null && tx.isActive()) {
                        tx.rollback();
                        System.out.println("Transaction rolled back due to error");
                    }
                    throw e;
                }
                
            } catch (Exception e) {
                System.err.println("FATAL ERROR in NewComplaint handler: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("newComplaintError", "Failed to submit complaint: " + e.getMessage())); }
                catch (IOException ignored) {}
            }

        } else if (msgString != null && msgString.equals("getComplaints")) {
            try (org.hibernate.Session s = sessionFactory.openSession()) {
                List<Complaint> complaints = complaintManager.listAll();
                try { client.sendToClient(complaints); }
                catch (IOException e) { e.printStackTrace(); }
            } catch (Exception e) {
                e.printStackTrace();
                try { client.sendToClient(new Message("getComplaintsError", "Failed to fetch complaints")); }
                catch (IOException ignored) {}
            }

        } else if (msgString != null && msgString.startsWith("getOrdersForUser:")) {
            try {
                String userIdStr = msgString.substring("getOrdersForUser:".length());
                int userId = Integer.parseInt(userIdStr);
                System.out.println("Received getOrdersForUser request for user ID: " + userId);
                
                List<Order> orders;
                // Load orders within a session to initialize lazy collections
                try (org.hibernate.Session s = sessionFactory.openSession()) {
                    // DEBUG: Check all orders in database
                    List<Order> allOrders = s.createQuery("from Order", Order.class).getResultList();
                    System.out.println("DEBUG: Total orders in database: " + allOrders.size());
                    for (Order o : allOrders) {
                        UserAccount ua = o.getUserAccount();
                        int orderUserId = (ua != null) ? ua.getUserId() : -1;
                        System.out.println("DEBUG: Order ID=" + o.getId() + ", User ID=" + orderUserId + ", Status=" + o.getStatus());
                    }
                    
                    // Load orders in the same session using JOIN FETCH to eagerly load orderLines and branch
                    orders = s.createQuery(
                        "select distinct o from Order o " +
                        "left join fetch o.orderLines " +
                        "left join fetch o.userAccount " +
                        "left join fetch o.branch " +
                        "where o.userAccount.userId = :u", 
                        Order.class
                    )
                    .setParameter("u", userId)
                    .getResultList();
                    
                    System.out.println("Found " + orders.size() + " orders for user " + userId);
                    
                    // Force initialization of nested lazy collections
                    for (Order order : orders) {
                        // orderLines should already be loaded due to JOIN FETCH
                        if (order.getOrderLines() != null) {
                            // Initialize each OrderLine's item reference
                            for (var line : order.getOrderLines()) {
                                if (line.getItem() != null) {
                                    line.getItem().getName(); // Force load item
                                }
                            }
                        }
                    }
                } // Session closed here, but entities are detached (but collections are initialized)
                
                try { 
                    client.sendToClient(orders);
                    System.out.println("Sent " + orders.size() + " orders to client");
                } catch (IOException e) { 
                    e.printStackTrace(); 
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid user ID in getOrdersForUser request: " + msgString);
                try { client.sendToClient(new Message("getOrdersError", "Invalid user ID")); }
                catch (IOException ignored) {}
            } catch (Exception e) {
                e.printStackTrace();
                try { client.sendToClient(new Message("getOrdersError", "Failed to fetch orders: " + e.getMessage())); }
                catch (IOException ignored) {}
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("newOrder")) {
            Message m = (Message) msg;
            NewOrderRequest req = (NewOrderRequest) m.getData();

            System.out.println("Received newOrder request from user ID: " + req.userId);

            org.hibernate.Transaction tx = null;
            try (org.hibernate.Session s = sessionFactory.openSession()) {
                tx = s.beginTransaction();

                try {
                    // 1) Load the user (entity) by id coming from the PublicUser
                    UserAccount user = s.get(UserAccount.class, req.userId);
                    if (user == null) {
                        System.err.println("ERROR: User with ID " + req.userId + " not found!");
                        tx.rollback();
                        client.sendToClient(new Message("newOrderError", "User not found"));
                        return;
                    }
                    System.out.println("Loaded user: " + user.getLogin());

                    // 2) Create the Order entity and populate core fields
                    Order order = new Order(user);
                    order.setGreeting(req.greeting);
                    order.setDeliveryType(req.deliveryType);
                    order.setDeliveryDateTime(req.deliveryDateTime != null ? req.deliveryDateTime : LocalDateTime.now());
                    order.setStatus("Pending"); // Set initial status

                    // Store the chosen payment method snapshot
                    if (req.paymentMethod != null) {
                        order.setPaymentMethod(req.paymentMethod);
                    }

                    // 3) Branch (if provided) - try user's branch if not provided
                    Branch branchToSet = null;
                    if (req.branchId != null) {
                        branchToSet = s.get(Branch.class, req.branchId);
                        if (branchToSet != null) {
                            System.out.println("Set branch from request: " + branchToSet.getName());
                        }
                    } else if (user.getBranch() != null) {
                        // Use user's default branch if no branch specified
                        branchToSet = user.getBranch();
                        System.out.println("Using user's default branch: " + branchToSet.getName());
                    }
                    
                    // If still no branch, get the first available branch as fallback
                    if (branchToSet == null) {
                        List<Branch> branches = s.createQuery("from Branch", Branch.class).setMaxResults(1).getResultList();
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

                    // 5) Lines
                    System.out.println("Adding " + req.lines.size() + " order lines...");
                    for (NewOrderRequest.Line line : req.lines) {
                        // Convert long itemId to int (Item.id is int)
                        int itemIdInt = (int) line.itemId;
                        Item item = s.get(Item.class, itemIdInt);
                        if (item == null) {
                            System.err.println("WARNING: Item with ID " + line.itemId + " (as int: " + itemIdInt + ") not found, skipping...");
                            continue;
                        }
                        int qty = Math.max(1, line.qty);
                        double unitPrice = line.unitPrice; // snapshot from client
                        order.addLine(item, qty, unitPrice);
                        System.out.println("  - Added: " + item.getName() + " x" + qty + " @ " + unitPrice);
                    }

                    if (order.getOrderLines().isEmpty()) {
                        System.err.println("ERROR: No valid order lines!");
                        tx.rollback();
                        client.sendToClient(new Message("newOrderError", "No valid items in order"));
                        return;
                    }

                    // 6) Recompute total if needed
                    order.recomputeTotal();
                    System.out.println("Order total: " + order.getTotalPrice());

                    // 7) Persist order (this will automatically add it to user's orderSet via bidirectional relationship)
                    s.persist(order);
                    s.flush(); // Ensure order gets an ID
                    System.out.println("Order persisted with ID: " + order.getId() + " for user ID: " + user.getUserId());

                    // 8) Refresh user to ensure orderSet is updated
                    s.refresh(user);
                    System.out.println("User now has " + user.getOrderSet().size() + " orders");

                    tx.commit();
                    System.out.println("Order transaction committed successfully");

                    // 9) Reply with the official order id so client can send the second email
                    client.sendToClient(new Message("newOrderOk", order.getId()));

                } catch (Exception e) {
                    System.err.println("ERROR in newOrder handler: " + e.getMessage());
                    e.printStackTrace();
                    if (tx != null && tx.isActive()) {
                        tx.rollback();
                        System.out.println("Transaction rolled back due to error");
                    }
                    throw e;
                }

            } catch (Exception e) {
                System.err.println("FATAL ERROR in newOrder handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("newOrderError", "Failed creating order: " + e.getMessage()));
                } catch (IOException ignored) {}
            }

        } else if (msg instanceof LoginRequest) {
            LoginRequest req = (LoginRequest) msg;
            try (Session s = sessionFactory.openSession()) {
                UserAccount user = s.createQuery(
                                "from UserAccount where login = :login", UserAccount.class)
                        .setParameter("login", req.getLogin())
                        .uniqueResult();

                boolean ok = (user != null) && user.verifyPassword(req.getPassword());

                if(ok){
                    client.sendToClient(LoginResult.ok(toPublicUser(user)));
                } else {
                    client.sendToClient(LoginResult.notFound("Invalid username or password"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new LoginResult(LoginResult.Status.ERROR, "Server error"));
                } catch (IOException ignored) {}
            }

        } else if (msg instanceof SignupRequest) {
            SignupRequest req = (SignupRequest) msg;

            try (Session s = sessionFactory.openSession()) {
                Transaction tx = null;
                try {
                    Long cnt = s.createQuery(
                            "select count(u) from UserAccount u where u.login = :login",
                            Long.class
                    ).setParameter("login", req.getUsername()).uniqueResult();

                    if (cnt != null && cnt > 0) {
                        try { client.sendToClient(SignupResult.usernameTaken()); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // create & save
                    tx = s.beginTransaction();
                    UserAccount ua = new UserAccount(req.getUsername(), req.getPassword(), req.getName(), req.getEmail(), 
                                                     req.getPayment(), req.getBranchType());
                    // Set idNumber if provided
                    if (req.getIdNumber() != null && !req.getIdNumber().isEmpty()) {
                        ua.setIdNumber(req.getIdNumber());
                    }
                    s.save(ua);
                    tx.commit();
                    System.out.println("User created: " + ua.getLogin() + " with payment method: " + 
                                     (ua.getDefaultPaymentMethod() != null ? "saved" : "none"));

                    try { client.sendToClient(SignupResult.ok()); }
                    catch (IOException ignored) {}
                }
                catch (ConstraintViolationException ex) {
                    if (tx != null) tx.rollback();
                    try { client.sendToClient(SignupResult.usernameTaken()); }
                    catch (IOException ignored) {}
                }
                catch (Exception ex) {
                    if (tx != null) tx.rollback();
                    ex.printStackTrace();
                    try { client.sendToClient(SignupResult.error()); }
                    catch (IOException ignored) {}
                }
            }

        } else if (msg instanceof UpdateUserDetailsRequest) {
            UpdateUserDetailsRequest req = (UpdateUserDetailsRequest) msg;
            System.out.println("Received UpdateUserDetailsRequest for user ID: " + req.getUserId());

            try (org.hibernate.Session s = sessionFactory.openSession()) {
                org.hibernate.Transaction tx = s.beginTransaction();
                try {
                    UserAccount user = s.get(UserAccount.class, req.getUserId());
                    if (user == null) {
                        System.err.println("ERROR: User with ID " + req.getUserId() + " not found!");
                        tx.rollback();
                        try { client.sendToClient(new Message("updateUserDetailsError", "User not found")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // Update user details
                    user.setName(req.getName());
                    user.setEmail(req.getEmail());
                    user.setIdNumber(req.getIdNumber());

                    s.update(user);
                    tx.commit();
                    System.out.println("User details updated successfully for user: " + user.getLogin());

                    try { client.sendToClient(new Message("updateUserDetailsOk", "Details updated successfully")); }
                    catch (IOException ignored) {}
                } catch (Exception e) {
                    if (tx != null && tx.isActive()) tx.rollback();
                    System.err.println("ERROR updating user details: " + e.getMessage());
                    e.printStackTrace();
                    try { client.sendToClient(new Message("updateUserDetailsError", "Failed to update details: " + e.getMessage())); }
                    catch (IOException ignored) {}
                }
            }

        } else if (msg instanceof UpdatePaymentMethodRequest) {
            UpdatePaymentMethodRequest req = (UpdatePaymentMethodRequest) msg;
            System.out.println("Received UpdatePaymentMethodRequest for user ID: " + req.getUserId());

            try (org.hibernate.Session s = sessionFactory.openSession()) {
                org.hibernate.Transaction tx = s.beginTransaction();
                try {
                    UserAccount user = s.get(UserAccount.class, req.getUserId());
                    if (user == null) {
                        System.err.println("ERROR: User with ID " + req.getUserId() + " not found!");
                        tx.rollback();
                        try { client.sendToClient(new Message("updatePaymentMethodError", "User not found")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // Update payment method
                    user.setDefaultPaymentMethod(req.getPaymentMethod());

                    s.update(user);
                    tx.commit();
                    System.out.println("Payment method updated successfully for user: " + user.getLogin());

                    try { client.sendToClient(new Message("updatePaymentMethodOk", "Payment method updated successfully")); }
                    catch (IOException ignored) {}
                } catch (Exception e) {
                    if (tx != null && tx.isActive()) tx.rollback();
                    System.err.println("ERROR updating payment method: " + e.getMessage());
                    e.printStackTrace();
                    try { client.sendToClient(new Message("updatePaymentMethodError", "Failed to update payment method: " + e.getMessage())); }
                    catch (IOException ignored) {}
                }
            }

        } else if (msg instanceof PurchaseSubscriptionRequest) {
            PurchaseSubscriptionRequest req = (PurchaseSubscriptionRequest) msg;
            System.out.println("Received PurchaseSubscriptionRequest for user ID: " + req.getUserId());

            try (org.hibernate.Session s = sessionFactory.openSession()) {
                org.hibernate.Transaction tx = s.beginTransaction();
                try {
                    UserAccount user = s.get(UserAccount.class, req.getUserId());
                    if (user == null) {
                        System.err.println("ERROR: User with ID " + req.getUserId() + " not found!");
                        tx.rollback();
                        try { client.sendToClient(new Message("purchaseSubscriptionError", "User not found")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // Save payment method
                    user.setDefaultPaymentMethod(req.getPaymentMethod());
                    
                    // Activate subscription
                    user.activateSubscription();

                    s.update(user);
                    tx.commit();
                    System.out.println("Subscription purchased and payment method saved for user: " + user.getLogin());

                    try { client.sendToClient(new Message("purchaseSubscriptionOk", "Subscription purchased successfully!")); }
                    catch (IOException ignored) {}
                } catch (Exception e) {
                    if (tx != null && tx.isActive()) tx.rollback();
                    System.err.println("ERROR purchasing subscription: " + e.getMessage());
                    e.printStackTrace();
                    try { client.sendToClient(new Message("purchaseSubscriptionError", "Failed to purchase subscription: " + e.getMessage())); }
                    catch (IOException ignored) {}
                }
            }

        } else if (msg instanceof RenewSubscriptionRequest) {
            RenewSubscriptionRequest req = (RenewSubscriptionRequest) msg;
            System.out.println("Received RenewSubscriptionRequest for user ID: " + req.getUserId());

            try (org.hibernate.Session s = sessionFactory.openSession()) {
                org.hibernate.Transaction tx = s.beginTransaction();
                try {
                    UserAccount user = s.get(UserAccount.class, req.getUserId());
                    if (user == null) {
                        System.err.println("ERROR: User with ID " + req.getUserId() + " not found!");
                        tx.rollback();
                        try { client.sendToClient(new Message("renewSubscriptionError", "User not found")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // Renew subscription (extend by 1 year from current expiration or from now)
                    if (user.getSubscriptionExpirationDate() != null && 
                        user.getSubscriptionExpirationDate().isAfter(LocalDate.now())) {
                        // Extend from current expiration date
                        user.setSubscriptionExpirationDate(user.getSubscriptionExpirationDate().plusYears(1));
                    } else {
                        // Start new subscription from today
                        user.activateSubscription();
                    }

                    s.update(user);
                    tx.commit();
                    System.out.println("Subscription renewed for user: " + user.getLogin());

                    try { client.sendToClient(new Message("renewSubscriptionOk", "Subscription renewed successfully!")); }
                    catch (IOException ignored) {}
                } catch (Exception e) {
                    if (tx != null && tx.isActive()) tx.rollback();
                    System.err.println("ERROR renewing subscription: " + e.getMessage());
                    e.printStackTrace();
                    try { client.sendToClient(new Message("renewSubscriptionError", "Failed to renew subscription: " + e.getMessage())); }
                    catch (IOException ignored) {}
                }
            }

        } else if (msg instanceof CustomOrder) {
            CustomOrder req = (CustomOrder) msg;
            System.out.println("Received CustomOrder request from user ID: " + req.getUserId());

            org.hibernate.Transaction tx = null;
            try (org.hibernate.Session s = sessionFactory.openSession()) {
                tx = s.beginTransaction();
                try {
                    // 1) Load the user
                    UserAccount user = s.get(UserAccount.class, req.getUserId());
                    if (user == null) {
                        System.err.println("ERROR: User with ID " + req.getUserId() + " not found!");
                        tx.rollback();
                        try { client.sendToClient(new Message("customOrderError", "User not found")); }
                        catch (IOException ignored) {}
                        return;
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
                    order.setGreeting(greetingBuilder.toString());

                    // 3) Set branch (use user's branch or first available)
                    Branch branchToSet = null;
                    if (user.getBranch() != null) {
                        branchToSet = user.getBranch();
                    } else {
                        List<Branch> branches = s.createQuery("from Branch", Branch.class).setMaxResults(1).getResultList();
                        if (!branches.isEmpty()) {
                            branchToSet = branches.get(0);
                        }
                    }
                    if (branchToSet != null) {
                        order.setBranch(branchToSet);
                    }

                    // 4) Set payment method if user has one
                    if (user.getDefaultPaymentMethod() != null) {
                        order.setPaymentMethod(user.getDefaultPaymentMethod());
                    }

                    // 5) Create a placeholder order line for custom design
                    // We'll create a special item or use a placeholder
                    // For now, we'll set a default price based on min/max price
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

                    // Create a CustomItem entity to store the custom design details
                    CustomItem customItem = new CustomItem(user, req.getItemType(), req.getMinPrice(), 
                                                          req.getMaxPrice(), req.getColor(), req.getNotes());
                    s.persist(customItem);
                    System.out.println("Created CustomItem with ID: " + customItem.getId());

                    // Create a placeholder order line using the first available item from catalog
                    // This is needed because Order requires at least one OrderLine
                    List<Item> items = s.createQuery("from Item", Item.class).setMaxResults(1).getResultList();
                    if (!items.isEmpty()) {
                        Item placeholderItem = items.get(0);
                        // Use estimated price or placeholder item price
                        double linePrice = estimatedPrice > 0 ? estimatedPrice : placeholderItem.getPrice();
                        order.addLine(placeholderItem, 1, linePrice);
                        System.out.println("Added placeholder order line: " + placeholderItem.getName() + " @ " + linePrice);
                    } else {
                        // If no items exist, we'll need to handle this differently
                        // For now, set total price and let the order be created without lines
                        order.setTotalPrice(estimatedPrice);
                        System.out.println("WARNING: No items in catalog, creating order without order lines");
                    }

                    // Recompute total
                    order.recomputeTotal();
                    System.out.println("Custom design order total: " + order.getTotalPrice());

                    // 6) Persist order
                    s.persist(order);
                    s.flush();
                    System.out.println("Custom design order persisted with ID: " + order.getId() + " for user ID: " + user.getUserId());

                    s.refresh(user);
                    tx.commit();
                    System.out.println("Custom design order transaction committed successfully");

                    try { client.sendToClient(new Message("customOrderOk", "Custom design order submitted successfully")); }
                    catch (IOException ignored) {}

                } catch (Exception e) {
                    System.err.println("ERROR in CustomOrder handler: " + e.getMessage());
                    e.printStackTrace();
                    if (tx != null && tx.isActive()) {
                        tx.rollback();
                        System.out.println("Transaction rolled back due to error");
                    }
                    try { client.sendToClient(new Message("customOrderError", "Failed to submit custom design order: " + e.getMessage())); }
                    catch (IOException ignored) {}
                }
            } catch (Exception e) {
                System.err.println("FATAL ERROR in CustomOrder handler: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("customOrderError", "Failed to submit custom design order: " + e.getMessage())); }
                catch (IOException ignored) {}
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("AddItem")) {
            // use imageData (byte[]) instead of imageLink; carry over flowerType if present
            Message message = (Message) msg;
            Item in = (Item) message.getData();

            // Prefer setters to avoid constructor signature mismatches
            Item item = new Item();
            item.setName(in.getName());
            item.setType(in.getType());
            item.setDescription(in.getDescription());
            item.setPrice(in.getPrice());
            item.setColor(in.getColor());
            // new binary image field
            item.setImageData(in.getImageData());
            // optional flowerType if your entity has it
            try {
                // reflect to avoid compile error if method doesn’t exist
                Item.class.getMethod("setFlowerType", String.class).invoke(item, in.getFlowerType());
            } catch (Exception ignore) {
                // ignore if flowerType doesn't exist
            }

            var sf = DbConnector.getInstance().getSessionFactory();
            org.hibernate.Transaction tx = null;
            try (org.hibernate.Session session = sf.openSession()) {
                tx = session.beginTransaction();
                session.persist(item);
                tx.commit();

                // Send success message to the client that added the item
                client.sendToClient(new Message("item added successfully", item));
                
                // Broadcast updated catalog to all subscribed clients
                List<Item> updatedCatalog = itemManager.GetItemList(new ArrayList<>());
                sendToAllClients(updatedCatalog);
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                try { client.sendToClient(new Message("item add error", e.getMessage())); }
                catch (IOException ignored) {}
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("removeItem")) {
            Message message = (Message) msg;
            Item itemToRemove = (Item) message.getData();
            
            System.out.println("Received removeItem request for item ID: " + itemToRemove.getId());
            
            try {
                // Remove the item using ItemManager
                boolean success = itemManager.RemoveItem(itemToRemove);
                
                if (success) {
                    System.out.println("Item removed successfully: " + itemToRemove.getName());
                    
                    // Send success message to the client that removed the item
                    try {
                        client.sendToClient(new Message("item removed successfully", itemToRemove));
                    } catch (IOException e) {
                        System.err.println("Failed to send success message to client");
                    }
                    
                    // Broadcast updated catalog to all subscribed clients
                    List<Item> updatedCatalog = itemManager.GetItemList(new ArrayList<>());
                    sendToAllClients(updatedCatalog);
                    System.out.println("Broadcasted updated catalog to all clients");
                } else {
                    System.err.println("Failed to remove item: " + itemToRemove.getName());
                    try {
                        client.sendToClient(new Message("item remove error", "Failed to remove item from database"));
                    } catch (IOException ignored) {}
                }
            } catch (Exception e) {
                System.err.println("ERROR in removeItem handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("item remove error", "Error removing item: " + e.getMessage()));
                } catch (IOException ignored) {}
            }

        } else if (msg instanceof CancelOrderRequest) {
            CancelOrderRequest req = (CancelOrderRequest) msg;
            System.out.println("Received CancelOrderRequest for order ID: " + req.getOrderId() + ", user ID: " + req.getUserId());

            org.hibernate.Transaction tx = null;
            try (org.hibernate.Session s = sessionFactory.openSession()) {
                tx = s.beginTransaction();
                try {
                    // 1) Load the order
                    Order order = s.get(Order.class, req.getOrderId());
                    if (order == null) {
                        System.err.println("ERROR: Order with ID " + req.getOrderId() + " not found!");
                        tx.rollback();
                        try { client.sendToClient(new Message("cancelOrderError", "Order not found")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // 2) Verify the order belongs to the user
                    if (order.getUserAccount().getUserId() != req.getUserId()) {
                        System.err.println("ERROR: Order " + req.getOrderId() + " does not belong to user " + req.getUserId());
                        tx.rollback();
                        try { client.sendToClient(new Message("cancelOrderError", "You don't have permission to cancel this order")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // 3) Check if order is already cancelled or delivered
                    if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
                        System.err.println("ERROR: Order " + req.getOrderId() + " is already cancelled");
                        tx.rollback();
                        try { client.sendToClient(new Message("cancelOrderError", "Order is already cancelled")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    if ("Delivered".equalsIgnoreCase(order.getStatus())) {
                        System.err.println("ERROR: Order " + req.getOrderId() + " is already delivered");
                        tx.rollback();
                        try { client.sendToClient(new Message("cancelOrderError", "Cannot cancel an order that has already been delivered")); }
                        catch (IOException ignored) {}
                        return;
                    }

                    // 4) Calculate refund based on time until delivery
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime deliveryTime = order.getDeliveryDateTime();
                    
                    if (deliveryTime == null) {
                        // If no delivery time set, use created time + 1 day as fallback
                        deliveryTime = order.getCreatedAt() != null ? order.getCreatedAt().plusDays(1) : now.plusDays(1);
                    }

                    long hoursUntilDelivery = java.time.Duration.between(now, deliveryTime).toHours();
                    double orderTotal = order.getTotalPrice();
                    double refundAmount = 0.0;
                    String refundMessage = "";

                    if (hoursUntilDelivery >= 3) {
                        // Full credit (100% refund)
                        refundAmount = orderTotal;
                        refundMessage = "Full credit (100%): " + String.format("%.2f", refundAmount) + " ₪";
                        System.out.println("Order cancelled >= 3 hours before delivery: Full refund");
                    } else if (hoursUntilDelivery >= 1) {
                        // 50% credit
                        refundAmount = orderTotal * 0.5;
                        refundMessage = "Partial credit (50%): " + String.format("%.2f", refundAmount) + " ₪";
                        System.out.println("Order cancelled 1-3 hours before delivery: 50% refund");
                    } else {
                        // No credit (0% refund)
                        refundAmount = 0.0;
                        refundMessage = "No credit (cancelled < 1 hour before delivery): 0.00 ₪";
                        System.out.println("Order cancelled < 1 hour before delivery: No refund");
                    }

                    // 5) Update order status
                    order.setStatus("Cancelled");
                    s.update(order);

                    // 6) Add credit to user's account
                    UserAccount user = order.getUserAccount();
                    user.addStoreCredit(refundAmount);
                    s.update(user);

                    System.out.println("Order " + req.getOrderId() + " cancelled. Refund: " + refundAmount + " ₪. User's new credit balance: " + user.getStoreCredit() + " ₪");

                    tx.commit();

                    // 7) Send email confirmation to customer (optional - don't fail if it doesn't work)
                    boolean emailSent = false;
                    if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                        String emailSubject = "Order #" + req.getOrderId() + " Cancellation Confirmation - FlowerShop 🌸";
                        String emailBody = String.format("""
                            Hello %s,
                            
                            We have received your cancellation request for order #%d.
                            
                            Cancellation Details:
                            • Order ID: #%d
                            • Order Total: %.2f ₪
                            • Refund Amount: %.2f ₪
                            • Refund Type: %s
                            
                            Your refund has been added to your store account as credit.
                            Current credit balance: %.2f ₪
                            
                            You can use this credit for future purchases.
                            
                            Thank you for shopping with us 💐
                            
                            Best regards,
                            FlowerShop Team
                            """, 
                            user.getName() != null ? user.getName() : "Valued Customer",
                            req.getOrderId(),
                            req.getOrderId(),
                            orderTotal,
                            refundAmount,
                            hoursUntilDelivery >= 3 ? "Full credit (100%)" : 
                            (hoursUntilDelivery >= 1 ? "Partial credit (50%)" : "No credit (cancelled < 1 hour before delivery)"),
                            user.getStoreCredit());
                        
                        try {
                            EmailSender.sendEmail(emailSubject, emailBody, user.getEmail());
                            emailSent = true;
                            System.out.println("✅ Cancellation confirmation email sent successfully to: " + user.getEmail());
                        } catch (Exception e) {
                            System.err.println("⚠️ Failed to send cancellation email to " + user.getEmail() + ": " + e.getMessage());
                            // Don't fail the cancellation if email fails - it's optional
                        }
                    }

                    // 8) Send success response with refund information
                    String successMessage = "Order cancelled successfully!\n" + refundMessage + 
                                          "\nCredit added to your store account.\n" +
                                          "Your current credit balance: " + String.format("%.2f", user.getStoreCredit()) + " ₪";
                    if (emailSent) {
                        successMessage += "\n\nA confirmation email has been sent to your email address.";
                    }
                    try {
                        client.sendToClient(new Message("cancelOrderOk", successMessage));
                    } catch (IOException ignored) {}

                } catch (Exception e) {
                    System.err.println("ERROR in CancelOrder handler: " + e.getMessage());
                    e.printStackTrace();
                    if (tx != null && tx.isActive()) {
                        tx.rollback();
                        System.out.println("Transaction rolled back due to error");
                    }
                    try {
                        client.sendToClient(new Message("cancelOrderError", "Failed to cancel order: " + e.getMessage()));
                    } catch (IOException ignored) {}
                }
            } catch (Exception e) {
                System.err.println("FATAL ERROR in CancelOrder handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("cancelOrderError", "Failed to cancel order: " + e.getMessage()));
                } catch (IOException ignored) {}
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("show branches")) {
            var sessionf = DbConnector.getInstance().getSessionFactory();

            try (org.hibernate.Session session = sessionf.openSession()) {
                java.util.List<Branch> branches =
                        session.createQuery("from Branch", Branch.class)
                                .getResultList();

                client.sendToClient(new Message("branch list", branches));
            } catch (Exception e) {
                try { client.sendToClient(new Message("branch list error", e.getMessage())); }
                catch (Exception ignore) {}
            }

        } else if (msgString.startsWith("remove client")) {
            if (!SubscribersList.isEmpty()) {
                for (SubscribedClient subscribedClient : SubscribersList) {
                    if (subscribedClient.getClient().equals(client)) {
                        SubscribersList.remove(subscribedClient);
                        break;
                    }
                }
            }
        }
    }

    public void sendToAllClients(Object message) {
        if (SubscribersList.isEmpty()) {
            System.out.println("Warning: No subscribed clients to broadcast to");
            return;
        }
        
        System.out.println("Broadcasting to " + SubscribersList.size() + " subscribed clients");
        
        // Use a copy to avoid ConcurrentModificationException
        ArrayList<SubscribedClient> clientsToRemove = new ArrayList<>();
        
        for (SubscribedClient subscribedClient : SubscribersList) {
            try {
                subscribedClient.getClient().sendToClient(message);
            } catch (IOException e) {
                System.err.println("Failed to send to client: " + e.getMessage());
                // Mark disconnected clients for removal
                clientsToRemove.add(subscribedClient);
            }
        }
        
        // Remove disconnected clients
        SubscribersList.removeAll(clientsToRemove);
        if (!clientsToRemove.isEmpty()) {
            System.out.println("Removed " + clientsToRemove.size() + " disconnected clients from subscribers list");
        }
    }
}
