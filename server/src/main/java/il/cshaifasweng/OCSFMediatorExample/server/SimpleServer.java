package il.cshaifasweng.OCSFMediatorExample.server;

import Request.*;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.*;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.ItemSale;
import il.cshaifasweng.OCSFMediatorExample.entities.SaleStatus;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;


import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;


public class SimpleServer extends AbstractServer {
    private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();
    private final SessionFactory sessionFactory;

    // managers
    private ItemManager itemManager = null;
    private ComplaintManager complaintManager = null;
    private OrderManager orderManager = null;
    private il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ReportManager reportManager = null;
    private BranchManager branchManager = null;
    private UserAccountManager userAccountManager = null;
    private CategoryManager categoryManager = null;
    private SaleManager saleManager = null;


    public SimpleServer(int port) {
        super(port);
        this.sessionFactory = DbConnector.getInstance().getSessionFactory();
        this.itemManager = new ItemManager(sessionFactory);
        this.complaintManager = new ComplaintManager(sessionFactory);
        this.orderManager = new OrderManager(sessionFactory);
        this.reportManager = new il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ReportManager(sessionFactory);
        this.branchManager = new BranchManager(sessionFactory);
        this.userAccountManager = new UserAccountManager(sessionFactory);
        this.categoryManager = new CategoryManager(sessionFactory);
        this.saleManager = new SaleManager(sessionFactory);
        this.categoryManager.createDefaultCategories();


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

        } else if (msg instanceof Message && "getCatalogForBranch".equals(((Message) msg).getType())) {
            Message m = (Message) msg;
            Integer branchId = (Integer) m.getData();
            System.out.println("Server: getCatalogForBranch, branchId = " + branchId);

            try {
                List<Item> items = itemManager.GetItemListForBranch(branchId);
                System.out.println("Catalog for branch " + branchId + " has " + items.size() + " items");
                client.sendToClient(items);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "getCatalogError",
                            "Failed to load catalog for branch " + branchId
                    ));
                } catch (IOException ignored) {}
            }

        } else if (msgString.equals("getCatalog")) {
            List<Item> items = itemManager.GetItemList(new ArrayList<>());
            System.out.println("Catalog received");
            System.out.println(items);
            try {
                client.sendToClient(items);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (msgString.equals("getCategories")) {
            try {
                List<Category> categories = categoryManager.listAll();
                client.sendToClient(categories);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("categoriesError", "Failed to load categories"));
                } catch (IOException ignored) {}
            }

        } else if ("#getSales".equals(msgString)) {
            try {
                List<Sale> sales = saleManager.listAll();
                client.sendToClient(sales);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("getSalesError", "Failed to load sales"));
                } catch (IOException ignored) {
                }
            }



        } else if (msg instanceof Message && "#create-sale".equals(((Message) msg).getType())) {
            Message m = (Message) msg;

            try {
                @SuppressWarnings("unchecked")
                List<ItemSale> itemSalesList = (List<ItemSale>) m.getData();

                if (itemSalesList == null || itemSalesList.isEmpty()) {
                    client.sendToClient(new Message("createSaleError", "Sale has no items"));
                    return;
                }

                // All ItemSale objects should share the same Sale reference from the client
                Sale sale = itemSalesList.get(0).getSale();
                if (sale == null) {
                    client.sendToClient(new Message("createSaleError", "Sale object is missing"));
                    return;
                }

                // Make sure each ItemSale points back to the sale
                for (ItemSale is : itemSalesList) {
                    is.setSale(sale);
                }

                // Convert List<ItemSale> -> Set<ItemSale>
                Set<ItemSale> itemSalesSet = new LinkedHashSet<>(itemSalesList);
                sale.setItemSales(itemSalesSet);

                // If somehow null, default to Announced
                if (sale.getStatus() == null) {
                    sale.setStatus(SaleStatus.Announced);
                }

                // Persist sale + itemSales
                saleManager.create(sale);

                // ACK back to client
                client.sendToClient(new Message("createSaleOk", sale));

                // send updated sales list so UI can refresh
                List<Sale> sales = saleManager.listAll();
                client.sendToClient(sales);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "createSaleError",
                            "Server error while creating sale: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }

        } else if (msgString.startsWith("#endSale")) {
            try {
                String[] parts = msgString.split(" ");
                if (parts.length < 2) {
                    client.sendToClient(new Message("endSaleError", "Missing sale id"));
                    return;
                }

                int saleId = Integer.parseInt(parts[1]);
                System.out.println("Received #endSale for sale id=" + saleId);

                // Do the status change inside one transaction/session
                saleManager.stashSale(saleId);

                // Explicit OK so UI can react if it wants
                client.sendToClient(new Message("endSaleOk", saleId));

                // Send updated sales list
                List<Sale> sales = saleManager.listAll();
                System.out.println("endSale: sending updated sales list with " + sales.size() + " entries");
                client.sendToClient(sales);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "endSaleError",
                            "Server error while ending sale: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }


        } else if (msg instanceof Message && ((Message) msg).getType().equals("getCatalogByCategory")) {

            int categoryId = (int) ((Message) msg).getData();

            System.out.println("SERVER: getCatalogByCategory for category " + categoryId);

            List<Item> items = itemManager.getItemsByCategory(categoryId);
            System.out.println("SERVER DEBUG: found " + items.size() + " items for category " + categoryId);


            try {
                client.sendToClient(items);
            } catch (IOException ignored) {}


        } else if (msg instanceof Message && "#getItems".equals(((Message) msg).getType())) {
            try {
                List<Item> items = itemManager.GetItemList(new ArrayList<>());
                client.sendToClient(items);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("getItemsError", "Failed to load items"));
                } catch (IOException ignored) {
                }
            }


        } else if (msgString.startsWith("#getAllBranches") || msgString.equals("getAllBranches")) {
            System.out.println("Received #getAllBranches request");
            try {
                List<Branch> branches = branchManager.listAll();

                System.out.println("Found " + branches.size() + " branches in database");
                //
                for (Branch b : branches) {
                    System.out.println("Branch: id=" + b.getId() + ", name=" + b.getName());
                }
                //
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

            try {
                Complaint c = complaintManager.resolveComplaint(
                        req.complaintId,
                        req.managerUserId,
                        req.response,
                        req.compensation
                );

                if (c != null) {
                    client.sendToClient(new Message("resolveComplaintError", "Complaint not found"));
                    return;
                }

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
                client.sendToClient(new Message("complaintResolved", c));
                List<Complaint> allComplaints = complaintManager.listAll();
                client.sendToClient(allComplaints);

            } catch (Exception e) {
                e.printStackTrace();
                try { client.sendToClient(new Message("resolveComplaintError", "Server error: " )); }
                catch (IOException ignored) {}
            }

        } else if (msg instanceof NewComplaint) {
            NewComplaint req = (NewComplaint) msg;
            System.out.println("Received NewComplaint: branchId=" + req.branchId + ", clientName=" + req.clientName
                                + ", orderNumber=" + req.orderNumber);

            try {
                // Load branch via branch manager if provided
                Branch branch = null;
                if (req.branchId != null) {
                    branch = branchManager.getById(req.branchId);
                    if (branch == null) {
                        System.err.println("ERROR: Branch with id " + req.branchId + " not found!");
                        client.sendToClient(new Message("newComplaintError", "Branch not found"));
                        return;
                    }
                    System.out.println("Loaded branch: " + branch.getName());
                }

                // Validate order number and load order
                if (req.orderNumber == null || req.orderNumber.trim().isEmpty()) {
                    System.err.println("Error: Complaint submitted without order number");
                    client.sendToClient(new Message("newComplaintError", "You must enter a valid order number"));
                    return;
                }

                int orderId = Integer.parseInt(req.orderNumber.trim());
                Order order = orderManager.getById(Integer.parseInt(req.orderNumber));
                if (order == null) {
                    System.err.println("ERROR: Order with ID " + orderId + " not found!");
                    client.sendToClient(new Message("newComplaintError", "Order not found"));
                    return;
                }
                System.out.println("Loaded order: " + order.getId() + " for complaint");

                // Create complaint
                Complaint complaint = new Complaint(branch, String.valueOf(orderId), req.clientName,
                                                    req.clientEmail, req.description);

                complaint.setOrder(order);
                System.out.println("Set order on complaint, order id = " +
                        (complaint.getOrder() != null ? complaint.getOrder().getId() : "null"));

                // Attach the user who owns the order to the complaint
                if (order.getUserAccount() != null) {
                    complaint.setUserAccount(order.getUserAccount());
                    System.out.println("Set user on complaint, user id = " + order.getUserAccount().getUserId());
                } else {
                    System.err.println("WARNING: order " + order.getId() + " has no userAccount; cannot set complaint.userAccount");
                    // If DB requires user_id NOT NULL, we should not continue
                    client.sendToClient(new Message("newComplaintError",
                            "This order is not associated with a user, cannot submit complaint."));
                    return;
                }

                System.out.println("Created complaint object, persisting...");

                // Save complaint (persist via manager)
                complaintManager.submit(complaint);
                System.out.println("Complaint persisted with ID: " + complaint.getComplaintId());

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
                client.sendToClient(new Message("newComplaintOk", "Complaint submitted successfully"));


            } catch (Exception e) {
                System.err.println("FATAL ERROR in NewComplaint handler: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("newComplaintError", "Failed to submit complaint: " + e.getMessage())); }
                catch (IOException ignored) {}
            }

        } else if (msgString != null && msgString.equals("getComplaints")) {
            try {
                List<Complaint> complaints = complaintManager.listAll();
                client.sendToClient(complaints);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("getComplaintsError", "Failed to fetch complaints"));
                } catch (IOException ignored) {
                }
            }

        } else if ("getCustomers".equals(msgString)) {
            try {
                System.out.println("Received getCustomers request");
                List<UserAccount> customers = userAccountManager.listCustomers();
                System.out.println("Sending " + customers.size() + " customers to client");
                client.sendToClient(new Message("customersList", customers));
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("getCustomersError",
                            "Failed to fetch customers: " + e.getMessage()));
                } catch (IOException ignored) {
                }
            }

        } else if (msg instanceof Message && "newsletterSend".equals(((Message) msg).getType())) {
            Message m = (Message) msg;
            String[] data = (String[]) m.getData();
            String subject = data[0];
            String body = data[1];

            System.out.println("Received newsletterSend request. Subject=" + subject);

            try {
                List<UserAccount> recipients = userAccountManager.listNewsletterSubscribers();
                System.out.println("Sending newsletter to " + recipients.size() + " subscribers");

                int successCount = 0;
                int failCount = 0;

                for (UserAccount ua : recipients) {
                    String to = ua.getEmail();
                    if (to == null || to.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        EmailSender.sendEmail(subject, body, to);
                        System.out.println("✅ Newsletter sent to: " + to);
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to send newsletter to " + to + ": " + e.getMessage());
                        failCount++;
                    }
                }

                String summary = String.format(
                        "Newsletter sent. Success: %d, Failed: %d, Total recipients: %d",
                        successCount, failCount, recipients.size()
                );

                try {
                    client.sendToClient(new Message("newsletterSendOk", summary));
                } catch (IOException e) {
                    System.err.println("Failed to send newsletterSendOk to client: " + e.getMessage());
                }

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "newsletterSendError",
                            "Failed to send newsletter: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }


        } else if (msg instanceof Message && "removeCustomer".equals(((Message) msg).getType())) {
            Message m = (Message) msg;
            Integer userId = (Integer) m.getData();

            try {
                System.out.println("Received removeCustomer request for userId=" + userId);

                // delete from DB
                userAccountManager.deleteById(userId);

                // send updated customer list back
                List<UserAccount> customers = userAccountManager.listCustomers();
                client.sendToClient(new Message("customersList", customers));

                client.sendToClient(new Message("removeCustomerOk", "Customer removed successfully"));
            } catch (IllegalStateException ise) {
                // our own "has orders" exception
                System.err.println("Cannot delete customer " + userId + ": " + ise.getMessage());
                try {
                    client.sendToClient(new Message(
                            "removeCustomerError",
                            ise.getMessage()
                    ));
                } catch (IOException ignored) {}

            } catch (Exception e) {
                System.err.println("ERROR in removeCustomer handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "removeCustomerError",
                            "Failed to remove customer: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }

        } else if ("getAllEmployees".equals(msgString)) {
            try {
                System.out.println("Received getAllEmployees request");
                List<UserAccount> employees = userAccountManager.listEmployees();
                System.out.println("Sending " + employees.size() + " employees to client");
                client.sendToClient(new Message("employeesList", employees));
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("getAllEmployeesError",
                            "Failed to fetch employees: " + e.getMessage()));
                } catch (IOException ignored) {}
            }


        } else if (msgString != null && msgString.startsWith("getOrdersForUser:")) {
            try {
                String userIdStr = msgString.substring("getOrdersForUser:".length());
                int userId = Integer.parseInt(userIdStr);
                System.out.println("Received getOrdersForUser request for user ID: " + userId);

                List<Order> orders = orderManager.listWithDetailsByUser(userId);

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

            try {
                // Create the order with “base” prices (no discount in the entity itself)
                Order order = orderManager.createFromRequest(req);

                // Apply active sales to the order lines and persist
                applySalesDiscounts(order);

                // Success -> send order ID back to client
                client.sendToClient(new Message("newOrderOk", order.getId()));

            } catch (RuntimeException e) {
                System.err.println("ERROR in newOrder handler: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("newOrderError", e.getMessage())); }
                catch (IOException ignored) {}
            } catch (Exception e) {
                System.err.println("FATAL ERROR in newOrder handler: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("newOrderError", "Failed creating order: " + e.getMessage())); }
                catch (IOException ignored) {}
            }



        } else if (msg instanceof LoginRequest) {
            LoginRequest req = (LoginRequest) msg;
            try {
                UserAccount user = userAccountManager.findByLogin(req.getLogin());

                boolean ok = (user != null) && user.verifyPassword(req.getPassword());

                if(ok){
                    client.sendToClient(LoginResult.ok(toPublicUser(user)));
                    EmailSender.sendEmail("Test Email", "Hello from Lilach System!", "youremail@gmail.com");

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

            System.out.println("Received SignupRequest: " + req);

            try {
                SignupResult result = userAccountManager.signup(req);
                client.sendToClient(result);
            } catch (Exception ex) {
                ex.printStackTrace();
                try { client.sendToClient(SignupResult.error()); }
                catch (IOException ignored) {}
            }

        } else if (msg instanceof UpdateUserDetailsRequest) {
            UpdateUserDetailsRequest req = (UpdateUserDetailsRequest) msg;
            System.out.println("Received UpdateUserDetailsRequest for user ID: " + req.getUserId());

            try {
                userAccountManager.updateDetails(
                        req.getUserId(),
                        req.getName(),
                        req.getEmail(),
                        req.getIdNumber()
                );
                client.sendToClient(new Message("updateUserDetailsOk", "Details updated successfully"));
            } catch (RuntimeException e) {
                // e.g., "User not found"
                System.err.println("ERROR updating user details: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("updateUserDetailsError", e.getMessage())); }
                catch (IOException ignored) {}
            } catch (Exception e) {
                System.err.println("FATAL ERROR updating user details: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("updateUserDetailsError",
                        "Failed to update details: " + e.getMessage())); }
                catch (IOException ignored) {}
            }


        } else if (msg instanceof UpdatePaymentMethodRequest) {
            UpdatePaymentMethodRequest req = (UpdatePaymentMethodRequest) msg;
            System.out.println("Received UpdatePaymentMethodRequest for user ID: " + req.getUserId());

            try {
                UserAccount updated = userAccountManager.updatePaymentMethod(req.getUserId(), req.getPaymentMethod());
                PublicUser publicUser = toPublicUser(updated);
                client.sendToClient(new Message("updatePaymentMethodOk", publicUser));
            } catch (RuntimeException e) {
                // e.g., "User not found"
                System.err.println("ERROR updating payment method: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("updatePaymentMethodError", e.getMessage())); }
                catch (IOException ignored) {}
            } catch (Exception e) {
                System.err.println("FATAL ERROR updating payment method: " + e.getMessage());
                e.printStackTrace();
                try { client.sendToClient(new Message("updatePaymentMethodError",
                        "Failed to update payment method: " + e.getMessage())); }
                catch (IOException ignored) {}
            }


        } else if (msg instanceof PurchaseSubscriptionRequest) {
            PurchaseSubscriptionRequest req = (PurchaseSubscriptionRequest) msg;
            System.out.println("Received PurchaseSubscriptionRequest for user ID: " + req.getUserId());

            try {
                UserAccount updated = userAccountManager.purchaseSubscription(
                        req.getUserId(),
                        req.getPaymentMethod()
                );
                System.out.println("Subscription purchased for user: " + updated.getLogin());

                client.sendToClient(new Message(
                        "purchaseSubscriptionOk",
                        "Subscription purchased successfully!"
                ));
            } catch (RuntimeException e) {
                System.err.println("ERROR purchasing subscription: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "purchaseSubscriptionError",
                            e.getMessage()
                    ));
                } catch (IOException ignored) {}
            } catch (Exception e) {
                System.err.println("FATAL ERROR purchasing subscription: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "purchaseSubscriptionError",
                            "Failed to purchase subscription: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }


        } else if (msg instanceof RenewSubscriptionRequest) {
            RenewSubscriptionRequest req = (RenewSubscriptionRequest) msg;
            System.out.println("Received RenewSubscriptionRequest for user ID: " + req.getUserId());

            try {
                UserAccount updated = userAccountManager.renewSubscription(req.getUserId());
                System.out.println("Subscription renewed for user: " + updated.getLogin());

                client.sendToClient(new Message(
                        "renewSubscriptionOk",
                        "Subscription renewed successfully!"
                ));
            } catch (RuntimeException e) {
                System.err.println("ERROR renewing subscription: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "renewSubscriptionError",
                            e.getMessage()
                    ));
                } catch (IOException ignored) {}
            } catch (Exception e) {
                System.err.println("FATAL ERROR renewing subscription: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "renewSubscriptionError",
                            "Failed to renew subscription: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }


        } else if (msg instanceof CustomOrder) {
            CustomOrder req = (CustomOrder) msg;
            System.out.println("Received CustomOrder request from user ID: " + req.getUserId());

            try {
                Order order = orderManager.createCustomOrder(req);
                System.out.println("Custom design order created with ID: " + order.getId());

                client.sendToClient(new Message(
                        "customOrderOk",
                        "Custom design order submitted successfully"
                ));
            } catch (RuntimeException e) {
                System.err.println("ERROR in CustomOrder handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "customOrderError",
                            e.getMessage()
                    ));
                } catch (IOException ignored) {}
            } catch (Exception e) {
                System.err.println("FATAL ERROR in CustomOrder handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "customOrderError",
                            "Failed to submit custom design order: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }


        } else if (msg instanceof Message && "addEmployee".equals(((Message) msg).getType())) {
            Message message = (Message) msg;
            UserAccount newEmployee = (UserAccount) message.getData();

            System.out.println("Received addEmployee request for username=" + newEmployee.getLogin());

            try {

                UserAccount saved = userAccountManager.create(newEmployee);
                System.out.println("Employee saved with ID: " + saved.getUserId());

                // send success back to client
                try {
                    client.sendToClient(new Message("addEmployeeOk", saved));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // optional: refresh employees list for that client
                try {
                    List<UserAccount> employees = userAccountManager.listEmployees();
                    client.sendToClient(new Message("employeesList", employees));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                System.err.println("ERROR in addEmployee handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("addEmployeeError", e.getMessage()));
                } catch (IOException ignored) {
                }
            }

        } else if (msg instanceof Message && "removeEmployee".equals(((Message) msg).getType())) {
            Message m = (Message) msg;
            Integer userId = (Integer) m.getData();

            try {
                System.out.println("Received removeEmployee request for userId=" + userId);

                // delete in DB
                userAccountManager.deleteById(userId);

                // send updated list back to client (EmployeeManagementController already listens to "employeesList")
                List<UserAccount> employees = userAccountManager.listEmployees();
                client.sendToClient(new Message("employeesList", employees));

                 client.sendToClient(new Message("removeEmployeeOk", "Employee removed successfully"));

            } catch (Exception e) {
                System.err.println("ERROR in removeEmployee handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "removeEmployeeError",
                            "Failed to remove employee: " + e.getMessage()
                    ));
                } catch (IOException ignored) {
                }
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("AddItem")) {
            Message message = (Message) msg;
            Item in = (Item) message.getData();

            Item item = new Item();
            item.setName(in.getName());
            item.setType(in.getType());
            item.setDescription(in.getDescription());
            item.setPrice(in.getPrice());
            item.setColor(in.getColor());
            item.setImagePath(in.getImagePath());
            try {
                Item.class.getMethod("setFlowerType", String.class).invoke(item, in.getFlowerType());
            } catch (Exception ignore) { }

            try {
                boolean ok = itemManager.AddItem(item);
                if (!ok) {
                    client.sendToClient(new Message("item add error", "Failed to persist item"));
                    return;
                }

                client.sendToClient(new Message("item added successfully", item));

                List<Item> updatedCatalog = itemManager.GetItemList(new ArrayList<>());
                sendToAllClients(updatedCatalog);
            } catch (Exception e) {
                e.printStackTrace();
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
            System.out.println("Received CancelOrderRequest for order ID: " +
                    req.getOrderId() + ", user ID: " + req.getUserId());

            try {
                OrderManager.CancelOrderResult res =
                        orderManager.cancelOrder(req.getOrderId(), req.getUserId());

                Order order = res.getOrder();
                UserAccount user = res.getUser();
                double orderTotal = res.getOrderTotal();
                double refundAmount = res.getRefundAmount();
                double newCredit = res.getNewCreditBalance();
                String refundType = res.getRefundType();
                long hoursUntilDelivery = res.getHoursUntilDelivery();

                // 7) Send email confirmation (optional)
                boolean emailSent = false;
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String emailSubject =
                            "Order #" + order.getId() + " Cancellation Confirmation - FlowerShop 🌸";

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
                            order.getId(),
                            order.getId(),
                            orderTotal,
                            refundAmount,
                            refundType,
                            newCredit
                    );

                    try {
                        EmailSender.sendEmail(emailSubject, emailBody, user.getEmail());
                        emailSent = true;
                        System.out.println("✅ Cancellation confirmation email sent successfully to: " + user.getEmail());
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to send cancellation email to " +
                                user.getEmail() + ": " + e.getMessage());
                    }
                }

                // 8) Success message back to client
                String successMessage =
                        "Order cancelled successfully!\n" +
                                refundType + ": " + String.format("%.2f", refundAmount) + " ₪\n" +
                                "Credit added to your store account.\n" +
                                "Your current credit balance: " + String.format("%.2f", newCredit) + " ₪";

                if (emailSent) {
                    successMessage += "\n\nA confirmation email has been sent to your email address.";
                }

                try {
                    client.sendToClient(new Message("cancelOrderOk", successMessage));
                } catch (IOException ignored) {}

            } catch (RuntimeException e) {
                // Domain / validation errors (order not found, no permission, already cancelled, delivered)
                System.err.println("ERROR in CancelOrder handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message("cancelOrderError", e.getMessage()));
                } catch (IOException ignored) {}
            } catch (Exception e) {
                // Unexpected errors
                System.err.println("FATAL ERROR in CancelOrder handler: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient(new Message(
                            "cancelOrderError",
                            "Failed to cancel order: " + e.getMessage()
                    ));
                } catch (IOException ignored) {}
            }


        } else if (msg instanceof Message && ((Message) msg).getType().equals("getComplaintsReport")) {
            try {
                @SuppressWarnings("unchecked")
                List<Object> data = (List<Object>) ((Message) msg).getData();
                Integer branchId = (Integer) data.get(0);
                java.time.LocalDate fromLd = (java.time.LocalDate) data.get(1);
                java.time.LocalDate toLd   = (java.time.LocalDate) data.get(2);

                java.util.Date from = java.util.Date.from(fromLd.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                java.util.Date to   = java.util.Date.from(toLd.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

                var list = reportManager.complaintsInRange(from, to,
                        (branchId == null ? Request.reports.ReportScope.NETWORK : Request.reports.ReportScope.BRANCH),
                        branchId);

                client.sendToClient(new il.cshaifasweng.OCSFMediatorExample.entities.ComplaintsReportEvent(list));
            } catch (Exception e) {
                try { client.sendToClient(new Message("complaintsReportError", e.getMessage())); } catch (IOException ignored) {}
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("getOrdersReport")) {
            try {
                @SuppressWarnings("unchecked")
                List<Object> data = (List<Object>) ((Message) msg).getData();
                Integer branchId = (Integer) data.get(0);
                java.time.LocalDate fromLd = (java.time.LocalDate) data.get(1);
                java.time.LocalDate toLd   = (java.time.LocalDate) data.get(2);

                java.util.Date from = java.util.Date.from(fromLd.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                java.util.Date to   = java.util.Date.from(toLd.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

                var orders = reportManager.ordersInRange(from, to,
                        (branchId == null ? Request.reports.ReportScope.NETWORK : Request.reports.ReportScope.BRANCH),
                        branchId);

                client.sendToClient(new il.cshaifasweng.OCSFMediatorExample.entities.OrdersReportEvent(orders));
            } catch (Exception e) {
                try { client.sendToClient(new Message("ordersReportError", e.getMessage())); } catch (IOException ignored) {}
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("getRevenueReport")) {
            try {
                @SuppressWarnings("unchecked")
                List<Object> data = (List<Object>) ((Message) msg).getData();
                Integer branchId = (Integer) data.get(0);
                java.time.LocalDate fromLd = (java.time.LocalDate) data.get(1);
                java.time.LocalDate toLd   = (java.time.LocalDate) data.get(2);

                java.util.Date from = java.util.Date.from(fromLd.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                java.util.Date to   = java.util.Date.from(toLd.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

                var orders = reportManager.ordersInRange(from, to,
                        (branchId == null ? Request.reports.ReportScope.NETWORK : Request.reports.ReportScope.BRANCH),
                        branchId);

                client.sendToClient(new il.cshaifasweng.OCSFMediatorExample.entities.RevenueReportEvent(orders));
            } catch (Exception e) {
                try { client.sendToClient(new Message("revenueReportError", e.getMessage())); } catch (IOException ignored) {}
            }

        } else if (msg instanceof Message && ((Message) msg).getType().equals("show branches")) {
            try {
                List<Branch> branches = branchManager.listAll();
                client.sendToClient(new Message("branch list", branches));
            } catch (Exception e) {
                try { client.sendToClient(new Message("branch list error", e.getMessage())); }
                catch (IOException ignore) {}
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

    /**
     * Apply all currently active sales to the given order’s lines.
     * This keeps Order/OrderLine “dumb”: they just store final prices.
     */
    private void applySalesDiscounts(Order order) {
        if (order == null) {
            return;
        }

        // Pull all active sales (date-wise)
        List<Sale> activeSales = saleManager.listActiveNowVisible();
        if (activeSales == null || activeSales.isEmpty()) {
            return; // nothing to apply
        }

        for (OrderLine line : order.getOrderLines()) {
            if (line == null) continue;

            Item item = line.getItem();
            if (item == null) continue;

            double basePrice = line.getUnitPrice(); // what OrderManager set
            double bestPrice = basePrice;

            for (Sale sale : activeSales) {
                // ignore hidden/stashed sales
                if (sale.getStatus() == SaleStatus.Stashed) {
                    continue;
                }

                // is this item part of the sale?
                boolean inSale = false;
                for (ItemSale is : sale.getItemSales()) {
                    if (is.getItem() != null && is.getItem().getId() == item.getId()) {
                        inSale = true;
                        break;
                    }
                }
                if (!inSale) continue;

                DiscountType type = sale.getDiscountType();
                double value = sale.getDiscountValue();

                double discounted = basePrice;
                if (type == DiscountType.PercentDiscount) {
                    discounted = basePrice * (1.0 - (value / 100.0));
                } else if (type == DiscountType.FlatDiscount) {
                    discounted = Math.max(0.0, basePrice - value);
                }

                if (discounted < bestPrice) {
                    bestPrice = discounted;
                }
            }

            // If we found a better (lower) price – set it on the line
            if (bestPrice != basePrice) {
                line.setUnitPrice(bestPrice);
            }
        }

        // Recalculate total and persist updated order
        order.recomputeTotal();
        orderManager.update(order);
    }

}
