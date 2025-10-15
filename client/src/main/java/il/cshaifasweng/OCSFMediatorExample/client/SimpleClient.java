package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import Request.LoginResult;
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
            } else if (msg instanceof LoginResult) {
                LoginResult loginResult = (LoginResult) msg;
                EventBus.getDefault().post(new LoginResponseEvent(loginResult.isSuccess(), null));
            } else if (msg instanceof List<?>) {
                List<?> list = (List<?>) msg;
                if (!list.isEmpty() && list.get(0) instanceof Item) {
                    List<Item> items = (List<Item>) list;
                    System.out.println("Received catalog with " + items.size() + " items.");
                    EventBus.getDefault().post(items);
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
