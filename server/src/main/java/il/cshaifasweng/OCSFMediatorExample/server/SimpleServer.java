package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

public class SimpleServer extends AbstractServer {
    private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

    // TODO: 1) declare instance of the manager you need for a request handling
    private ItemManager itemManager = null;

    public SimpleServer(int port) {
        super(port);
        var sessionFactory = DbConnector.getInstance().getSessionFactory();
        // TODO: 2) create an instance of the manager you need
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
           // try {
            //List<Item> catalog = dbConnector.GetItemList(new ArrayList<>());
                //TODO : send catalog to client
               // client.sendToClient("showCatalog");
          //  } catch (IOException e) {
              // throw new RuntimeException(e);
           // }

            DbConnector db = DbConnector.getInstance();
            List<Item> items = db.GetItemList(new ArrayList<>());
            System.out.println("Catalog received");
            System.out.println(items);
            try {
                client.sendToClient(items);
            } catch (Exception e) { e.printStackTrace(); }
        } else if (msg instanceof Item) {
            Item updatedItem = (Item) msg;
            System.out.println("Received updated item: " + updatedItem.getName() + " | New price: " + updatedItem.getPrice());
            //TODO : send a reply to user?
            boolean success =  DbConnector.getInstance().EditItem(updatedItem);
            if (success){
                System.out.println("Item edited successfully");
                List<Item> updatedCatalog = DbConnector.getInstance().GetItemList(new ArrayList<>());
                try {
                    client.sendToClient(updatedCatalog); // Updating the view
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else{
                System.out.println("Item could not be edited");
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