package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import org.greenrobot.eventbus.EventBus;
import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
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
        } else if (msg instanceof List<?>) {
            System.out.println("111111111111");
            List<?> list = (List<?>) msg;
            System.out.println("222222");
            System.out.println("List size: " + list.size());
            System.out.println("List content: " + list);

            if (!list.isEmpty() && list.get(0) instanceof Item) {
                List<Item> items = (List<Item>) list;
                System.out.println("Received catalog with " + items.size() + " items.");
                EventBus.getDefault().post(items);
            }
        } else {
            String message = msg.toString();
            System.out.println("Server: " + message);

            if (message.startsWith("showCatalog")) {
                EventBus.getDefault().post("showCatalog");

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
