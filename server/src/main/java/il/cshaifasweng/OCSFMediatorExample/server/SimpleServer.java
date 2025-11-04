package il.cshaifasweng.OCSFMediatorExample.server;

import Request.*;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ItemManager;
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

    public SimpleServer(int port) {
        super(port);
        this.sessionFactory = DbConnector.getInstance().getSessionFactory();
        this.itemManager = new ItemManager(sessionFactory);
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

        } else if (msg instanceof Item) {
            Item updatedItem = (Item) msg;
            System.out.println("Received updated item: " + updatedItem.getName() + " | New price: " + updatedItem.getPrice());
            boolean success = itemManager.EditItem(updatedItem);
            if (success) {
                System.out.println("Item edited successfully");
                List<Item> updatedCatalog = itemManager.GetItemList(new ArrayList<>());
                try {
                    // TODO: broadcast to all clients if needed
                    client.sendToClient(updatedCatalog);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Item could not be edited");
            }

        } else if (msg instanceof UserAccount) {
            // (no-op / future handling)

        } else if (msg instanceof Message && ((Message) msg).getType().equals("resolveComplaint")) {
            Message m = (Message) msg;
            @SuppressWarnings("unchecked")
            java.util.List<?> data = (java.util.List<?>) m.getData();

            Integer complaintId = (Integer) data.get(0);
            String responseText = (String) data.get(1);
            Double compensation = (Double) data.get(2);
            Boolean resolved = (Boolean) data.get(3);
            Integer managerUserId = (Integer) data.get(4);

            try (org.hibernate.Session s = sessionFactory.openSession()) {
                org.hibernate.Transaction tx = s.beginTransaction();

                Complaint c = s.get(Complaint.class, complaintId);
                if (c == null) {
                    tx.rollback();
                    client.sendToClient(new Message("resolveComplaintError", "Complaint not found"));
                    return;
                }

                c.setResponse(responseText);
                c.setCompensation(compensation != null ? compensation : 0.0);
                c.setResolved(Boolean.TRUE.equals(resolved));
                c.setRespondedAt(java.time.LocalDateTime.now());

                if (managerUserId != null) {
                    UserAccount manager = s.get(UserAccount.class, managerUserId);
                    if (manager != null) {
                        c.setManagerAccount(manager); //  set entity server-side
                    }
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

                // send updated complaint back to refresh client UI
                client.sendToClient(new Message("complaintResolved", c));

            } catch (Exception e) {
                e.printStackTrace();
                try { client.sendToClient(new Message("resolveComplaintError", "Server error")); }
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

                client.sendToClient(new Message("item added successfully", item));
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

    public void sendToAllClients(String message) {
        try {
            for (SubscribedClient subscribedClient : SubscribersList) {
                subscribedClient.getClient().sendToClient(message);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
