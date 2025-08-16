package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.App.setRoot;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.getClient;

public class InitController {
    @FXML private TextField host;
    @FXML private TextField port;

    @FXML
    void initialize() {
        // no EventBus subscription here
    }

    @FXML
    void ready(ActionEvent event) {
        String hostText = host.getText().trim();
        String portText = port.getText().trim();
        if (hostText.isEmpty() || portText.isEmpty()) {
            EventBus.getDefault()
                    .post(new WarningEvent(new Warning("Must fill all fields!")));
            return;
        }

        int portNumber;
        try {
            portNumber = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            EventBus.getDefault()
                    .post(new WarningEvent(new Warning("Invalid port number!")));
            return;
        }

        // 1) Create & open the client
        client = getClient(hostText, portNumber);
        try {
            client.openConnection();
            client.sendToServer("add client");
        } catch (IOException e) {
            e.printStackTrace();
            // optionally post a WarningEvent here
        }

        // 2) Now swap to the Login screen
        try {
            setRoot("MainPage");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
