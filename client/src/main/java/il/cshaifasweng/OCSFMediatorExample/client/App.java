package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.client.Events.WarningEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class App extends Application implements Serializable {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        EventBus.getDefault().register(this);
        scene = new Scene(loadFXML("Init"), 700, 500);

        var resource = Thread.currentThread()
                .getContextClassLoader()
                .getResource("styles.css");
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        }

        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        // Try both possible locations given your current layout
        String[] candidates = {
                "/il/cshaifasweng/OCSFMediatorExample/" + fxml + ".fxml",
                "/il/cshaifasweng/OCSFMediatorExample/client/" + fxml + ".fxml"
        };

        URL resource = null;
        StringBuilder tried = new StringBuilder();
        for (String path : candidates) {
            System.out.println("Checking FXML path: " + path); // debug
            resource = App.class.getResource(path);
            tried.append(path).append(" ");
            if (resource != null) {
                System.out.println("Found FXML at: " + path + " -> " + resource); // success log
                break;
            }
        }

        if (resource == null) {
            String msg = "FXML resource not found: tried paths: " + tried.toString().trim();
            System.err.println(msg); // <--- added logging before throwing
            throw new IOException(msg);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        return loader.load();
    }


    @Override
    public void stop() throws Exception {
        EventBus.getDefault().unregister(this);
        if (client != null) {
            client.sendToServer("remove client");
            client.closeConnection();
        }
        super.stop();
    }

    @org.greenrobot.eventbus.Subscribe
    public void onWarningEvent(WarningEvent event) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.WARNING,
                    String.format("Message: %s\nTimestamp: %s\n",
                            event.getWarning().getMessage(),
                            event.getWarning().getTime().toString())
            );
            alert.show();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
