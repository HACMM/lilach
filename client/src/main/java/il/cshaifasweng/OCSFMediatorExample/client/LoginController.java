package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.getClient;


public class LoginController {

    @FXML
    private Label SignUp;

    @FXML
    private Button loginBtn;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField usernameField;

    @FXML
    void OnSignUpClicked(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/AccountCreationView.fxml")
            );
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("create account");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void onLoginClicked(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Warning warning = new Warning("must fill all fields!");
            EventBus.getDefault().post(new WarningEvent(warning));
            return;
        }

        try {
            String loginRequest = "login:" + username + "," + password;
            client.sendToServer(loginRequest);
        } catch (Exception e) {
            System.out.println("Failed to connect to server.");
        }
    }
}


