package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public class LoginController {

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label        warningLabel;
    @FXML private Label        signupLabel;

    @FXML
    private void onBackClicked(ActionEvent event) {
        // go back to the main dashboard
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLoginClicked(ActionEvent event) {
        warningLabel.setText("");
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();
        if (user.isEmpty() || pass.isEmpty()) {
            warningLabel.setText("⚠ Must fill all fields!");
            return;
        }
        // TODO: send login request
        try {
            App.setRoot("CatalogView");
        } catch (IOException e) {
            e.printStackTrace();
            warningLabel.setText("⚠ Could not load next page.");
        }
    }

    @FXML
    private void onSignUpClicked(MouseEvent event) {
        try {
            App.setRoot("AccountCreationView");
        } catch (IOException e) {
            e.printStackTrace();
            warningLabel.setText("⚠ Could not load signup page.");
        }
    }
}
