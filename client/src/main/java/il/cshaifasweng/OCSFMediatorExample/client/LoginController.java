package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;
import java.io.IOException;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class LoginController {

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label        warningLabel;
    @FXML private Label        signupLabel;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
    }


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
        try {
            client.sendToServer(new UserAccount(user, pass));
        } catch (IOException e) {
            e.printStackTrace();
            warningLabel.setText("⚠ Connection error. Please try again.");
        }
//        try {
//            App.setRoot("CatalogView");
//        } catch (IOException e) {
//            e.printStackTrace();
//            warningLabel.setText("⚠ Could not load next page.");
//        }
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


@Subscribe
public void onLoginResponse(LoginResponseEvent event) {
    Platform.runLater(() -> {
        if(event.isSuccess()) {
            try {
                App.setRoot("CatalogView");
            } catch (IOException e) {
                e.printStackTrace();
                EventBus.getDefault()
                        .post(new WarningEvent(new Warning("⚠ Could not load CatalogView.")));
            }
        } else {
            EventBus.getDefault()
                    .post(new WarningEvent(new Warning("⚠ Invalid username or password")));
        }
    });
}
}
