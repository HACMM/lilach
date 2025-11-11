package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.SignupResult;
import il.cshaifasweng.OCSFMediatorExample.client.Events.*;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import Request.LoginResult;
import jdk.jfr.Event;
import org.greenrobot.eventbus.EventBus;
import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import Request.Warning;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.List;

public class SimpleClient extends AbstractClient {

    public static SimpleClient client;

    private SimpleClient(String host, int port) {
        super(host, port);
    }


    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg.getClass().equals(Warning.class)) {
            EventBus.getDefault().post(new WarningEvent((Warning) msg));
        } else {
            String message = msg.toString();
            System.out.println("Server: " + message);

            if (message.startsWith("showCatalog")) {
                EventBus.getDefault().post("showCatalog");
            }
            else if (msg instanceof LoginResult) {
                LoginResult loginResult = (LoginResult) msg;

                EventBus.getDefault().post(new LoginResponseEvent(loginResult.isSuccess(),
                        loginResult.getMessage(), loginResult.getUser()));

            } else if (msg instanceof SignupResult) {
                SignupResult signupResult = (SignupResult) msg;
               EventBus.getDefault().post(SignupResponseEvent.from(signupResult));

            } else if (msg instanceof Message && ((Message) msg).getType().equals("item added successfully")) {
                Item item = (Item) ((Message) msg).getData();
                long itemId = item.getId();
                EventBus.getDefault().post(new AddItemEvent(itemId, item));

            } else if (msg instanceof Message && ((Message) msg).getType().equals("branch list")) {
                @SuppressWarnings("unchecked")
                List<Branch> branches = (List<Branch>) ((Message) msg).getData();
                System.out.println("SimpleClient: Received branch list with " + branches.size() + " branches");
                EventBus.getDefault().post(new BranchListEvent(branches));

            } else if (msg instanceof Message && ((Message) msg).getType().equals("newOrderOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("newComplaintOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("newComplaintError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("complaintResolved")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("resolveComplaintError")) {
                EventBus.getDefault().post(msg);
            }
            else if (msg instanceof List<?>) {
                List<?> list = (List<?>) msg;
                if (!list.isEmpty()) {
                    if (list.get(0) instanceof Item) {
                        List<Item> items = (List<Item>) list;
                        System.out.println("Received catalog with " + items.size() + " items.");
                        EventBus.getDefault().post(items);
                    } else if (list.get(0) instanceof il.cshaifasweng.OCSFMediatorExample.entities.Complaint) {
                        @SuppressWarnings("unchecked")
                        List<il.cshaifasweng.OCSFMediatorExample.entities.Complaint> complaints = 
                            (List<il.cshaifasweng.OCSFMediatorExample.entities.Complaint>) list;
                        EventBus.getDefault().post(complaints);
                    }
                }
            }
        }
    }

    public static SimpleClient getClient(String host, int port) {
        if (client == null) {
            client = new SimpleClient(host, port);
        }
        return client;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}
