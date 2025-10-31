package il.cshaifasweng.OCSFMediatorExample.server;

import Request.*;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ItemManager;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.IOException;
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

        } else if (msg instanceof LoginRequest) {
            LoginRequest req = (LoginRequest) msg;
            try (Session s = sessionFactory.openSession()) {
                UserAccount user = s.createQuery(
                                "from UserAccount where login = :login", UserAccount.class)
                        .setParameter("login", req.getLogin())
                        .uniqueResult();

                boolean ok = (user != null) && user.verifyPassword(req.getPassword());

                client.sendToClient(
                        ok
                                ? new LoginResult(LoginResult.Status.USER_FOUND, "OK")
                                : new LoginResult(LoginResult.Status.USER_NOT_FOUND, "Invalid username or password")
                );
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
