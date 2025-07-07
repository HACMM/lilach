package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class AccountCreationController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField passwordField;

    @FXML
    private Button signupBtn;

    @FXML
    private TextField usernameField;

    @FXML
    void onSignUpClicked(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String name = nameField.getText();
        String email = emailField.getText();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty() || email.isEmpty()) {
            Warning warning = new Warning("must fill all fields!");
            EventBus.getDefault().post(new WarningEvent(warning));
        }
        User newUser = new User(username, password, name, email);

        try {
            client.sendToServer(new Message("sign up", newUser));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
