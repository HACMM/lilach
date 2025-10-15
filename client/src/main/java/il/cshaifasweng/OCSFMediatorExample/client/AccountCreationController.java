package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import Request.Warning;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class AccountCreationController {

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         warningLabel;


    @FXML private Button backBtn;

    // existing fields and methods...

    @FXML
    private void onBack(ActionEvent event) {
        try {
            // go back to login view
            App.setRoot("LoginView"); // adjust name if your FXML basename differs
        } catch (IOException e) {
            e.printStackTrace();
            // optionally show user feedback
        }
    }
    /** when you hit “Sign me Up!” */
    @FXML
    private void onSignUpClicked(ActionEvent event) {
        // grab what you typed
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // make sure nothing’s blank
        if (name.isEmpty() || email.isEmpty() ||
                username.isEmpty() || password.isEmpty()) {
            EventBus.getDefault().post(
                    new WarningEvent(new Warning("Must fill all fields!"))
            );
            return;
        }

        // send data off to server
        User newUser = new User(username, password, name, email);
        try {
            client.sendToServer(new Message("sign up", newUser));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** when you click “Log In” at the bottom */
    @FXML
    private void onLoginLinkClicked(MouseEvent event) {
        try {
            App.setRoot("LoginView");
        } catch (IOException ex) {
            ex.printStackTrace();
            warningLabel.setText("⚠ Couldn’t open login page.");
        }
    }
}
