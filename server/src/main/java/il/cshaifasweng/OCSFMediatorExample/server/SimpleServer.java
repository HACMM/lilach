package il.cshaifasweng.OCSFMediatorExample.server;

import Request.*;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ItemManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ComplaintManager;
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

    public SimpleServer(int port) {
        super(port);
        this.sessionFactory = DbConnector.getInstance().getSessionFactory();
        this.itemManager = new ItemManager(sessionFactory);
        this.complaintManager = new ComplaintManager(sessionFactory);
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

        } else if (msg instanceof Message && ((Message) msg).getType().equals("newOrder")) {
            Message m = (Message) msg;
            NewOrderRequest req = (NewOrderRequest) m.getData();

            org.hibernate.Transaction tx = null;
            try (org.hibernate.Session s = sessionFactory.openSession()) {
                tx = s.beginTransaction();

                // 1) Load the user (entity) by id coming from the PublicUser
                UserAccount user = s.get(UserAccount.class, req.userId);
                if (user == null) {
                    tx.rollback();
                    client.sendToClient(new Message("newOrderError", "User not found"));
                    return;
                }

                // 2) Create the Order entity and populate core fields
                Order order = new Order(user);
                order.setGreeting(req.greeting);
                order.setDeliveryType(req.deliveryType);
                order.setDeliveryDateTime(req.deliveryDateTime != null ? req.deliveryDateTime : LocalDateTime.now());

                // Optional: store the chosen payment method snapshot
                // order.setPaymentMethod(req.paymentMethod);

                // 3) Branch (if provided)
                if (req.branchId != null) {
                    Branch br = s.get(Branch.class, req.branchId);
                    if (br != null) {
                        order.setBranch(br);
                    }
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
                for (NewOrderRequest.Line line : req.lines) {
                    Item item = s.get(Item.class, line.itemId);
                    if (item == null) continue; // or collect and fail
                    int qty = Math.max(1, line.qty);
                    double unitPrice = line.unitPrice; // snapshot from client
                    order.addLine(item, qty, unitPrice);
                }

                // 6) Recompute total if needed
                order.recomputeTotal();

                // 7) Persist
                s.persist(order);
                tx.commit();

                // 8) Reply with the official order id so client can send the second email
                client.sendToClient(new Message("newOrderOk", order.getId()));

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    if (tx != null) tx.rollback();
                    client.sendToClient(new Message("newOrderError", "Failed creating order"));
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
                    UserAccount ua = new UserAccount(req.getUsername(), req.getPassword(), req.getName(), req.getEmail());
                    s.save(ua);
                    tx.commit();

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
