/**
 * Sample Skeleton for 'init.fxml' Controller Class
 */


package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.io.IOException;
import static il.cshaifasweng.OCSFMediatorExample.client.App.setRoot;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class InitController {
    @FXML // fx:id="host"
    private TextField host; // Value injected by FXMLLoader

    @FXML // fx:id="port"
    private TextField port; // Value injected by FXMLLoader


    @FXML
    void initialize() {
        EventBus.getDefault().register(this);
    }

    @FXML
    void ready(ActionEvent event) {
        int portNumber;
        if (host.getText() == null || host.getText().isEmpty() || port.getText() == null || port.getText().isEmpty()) {
            Warning warning = new Warning("must fill all fields!");
            EventBus.getDefault().post(new WarningEvent(warning));
        } else {
            try {
                portNumber = Integer.parseInt(port.getText());
                client = SimpleClient.getClient(host.getText(), portNumber);
                try {
                    client.openConnection();
                    client.sendToServer("add client");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Failed to open connection or send to server.");
                }
            } catch (NumberFormatException e) {
                Warning warning = new Warning("Invalid port");
                EventBus.getDefault().post(new WarningEvent(warning));
            }
        }
    }

    private boolean isCatalogVisible() {
        Scene scene = Stage.getWindows().stream()
                .filter(Window::isShowing)
                .findFirst()
                .map(Window::getScene)
                .orElse(null);

        if (scene == null) return false;

        // Use an ID or node check to detect if it's the catalog
        Node root = scene.getRoot();
        return root.lookup("#catalogTable") != null;  // or any node unique to CatalogView
    }


    @Subscribe
    public void ShowCatalog(String event) {
        if (event.equals("showCatalog")) {
            Platform.runLater(() -> {
                if (!isCatalogVisible()) {
                    try {
                        setRoot("CatalogView");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    System.out.println("[INFO] Already in CatalogView — not reloading.");
                }
            });
        }
    }

}

