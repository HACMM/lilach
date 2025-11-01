package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.SignupRequest;
import il.cshaifasweng.OCSFMediatorExample.client.Events.SignupResponseEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import il.cshaifasweng.OCSFMediatorExample.client.Events.WarningEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import Request.Warning;
import il.cshaifasweng.OCSFMediatorExample.entities.UserBranchType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class AccountCreationController {

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         warningLabel;
    @FXML private Button addPaymentBtn;
    @FXML private Button backBtn;
    @FXML private ComboBox<String> accountTypeCombo;
    //@FXML private Label subscriptionNote;
    @FXML private Button ChooseCard;
    @FXML private Button checkoutBtn;


    private PaymentMethod selectedPaymentMethod;
    // existing fields and methods...


    @FXML
    public void initialize() {
        accountTypeCombo.getItems().addAll(
                "Branch Account",
                "Network Account",
                "Yearly Subscription"
        );
        accountTypeCombo.setValue("Branch Account");

        accountTypeCombo.setOnAction(e -> {
            String selected = accountTypeCombo.getValue();
            //subscriptionNote.setVisible("Yearly Subscription".equals(selected));
            ChooseCard.setVisible("Yearly Subscription".equals(selected));
            checkoutBtn.setVisible("Yearly Subscription".equals(selected));
        });
    }


    @FXML
    private void onBack(ActionEvent event) {
        try {
            // go back to login view
            App.setRoot("Login"); // adjust name if your FXML basename differs
        } catch (IOException e) {
            e.printStackTrace();
            // optionally show user feedback
        }
    }

    @FXML
    private void onAddPaymentClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/PaymentMethodView.fxml"));
            Parent root = loader.load();

            PaymentMethodController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Add Payment Method");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            selectedPaymentMethod = controller.getPaymentMethod();
        } catch (IOException e) {
            e.printStackTrace();
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

        if (selectedPaymentMethod == null) {
            warningLabel.setText("Please add a payment method before signing up.");
            return;
        }

        //make sure email is valid
        if (!email.contains("@") || !email.contains(".")) {
            EventBus.getDefault().post(
                    new WarningEvent(new Warning("Invalid email address!"))
            );
            return;
        }

        UserBranchType branchType = switch (accountTypeCombo.getValue()) {
            case "Branch Account" -> UserBranchType.BRANCH;
            case "Network Account" -> UserBranchType.ALL_BRANCHES;
            case "Yearly Subscription" -> UserBranchType.SUBSCRIPTION;
            default -> UserBranchType.ALL_BRANCHES;
        };


        // send data off to server
        // UserAccount newUser = new UserAccount(username, password, name, email, selectedPaymentMethod, branchType);

        try {
            client.sendToServer(new SignupRequest(username,password,name,email,selectedPaymentMethod, branchType));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** when you click “Log In” at the bottom */
    @FXML
    private void onLoginLinkClicked(MouseEvent event) {
        try {
            App.setRoot("Login");
        } catch (IOException ex) {
            ex.printStackTrace();
            warningLabel.setText("⚠ Couldn’t open login page.");
        }
    }

    public void onChooseCardClicked(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/PaymentMethodView.fxml"));
            Parent root = loader.load();

            PaymentMethodController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Choose Payment Method");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            selectedPaymentMethod = controller.getPaymentMethod();

            if (selectedPaymentMethod != null) {
                warningLabel.setText("Payment method selected successfully.");
            } else {
                warningLabel.setText("No payment method selected.");
            }

        } catch (IOException e) {
            e.printStackTrace();
            warningLabel.setText("Error opening payment window.");
        }
    }

    public void onCheckoutClicked(ActionEvent actionEvent) {
        if (selectedPaymentMethod == null) {
            warningLabel.setText("Please choose a payment method before checkout.");
            return;
        }

        Alert paymentAlert = new Alert(Alert.AlertType.INFORMATION);
        paymentAlert.setTitle("Payment Successful");
        paymentAlert.setHeaderText("Yearly Subscription Activated");
        paymentAlert.setContentText("Your yearly subscription has been paid successfully (100₪).");
        paymentAlert.showAndWait();
    }

    @Subscribe
    public void onSignupResponseEvent(SignupResponseEvent ev) {
        javafx.application.Platform.runLater(() -> {
            if (ev.isOk()) {
                warningLabel.setText("Signup successful!");
                try { App.setRoot("Login"); } catch (Exception ignored) {}
                return;
            }
            if (ev.isUsernameTaken()) {
                warningLabel.setText("Username is already taken.");
                return;
            }
            String msg = (ev.getErrorMessage() != null && !ev.getErrorMessage().isBlank())
                    ? ev.getErrorMessage()
                    : "Signup failed. Please try again.";
            warningLabel.setText(msg);
        });
    }

}