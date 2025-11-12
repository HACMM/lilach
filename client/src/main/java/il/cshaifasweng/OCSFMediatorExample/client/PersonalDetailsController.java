package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;
import Request.RenewSubscriptionRequest;
import Request.UpdatePaymentMethodRequest;
import Request.UpdateUserDetailsRequest;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.entities.UserBranchType;
import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import Request.Message;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class PersonalDetailsController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField idField;
    @FXML private Label accountTypeLbl;
    @FXML private Label subscriptionExpiryLbl;
    @FXML private Label statusLabel;
    @FXML private Button renewSubBtn;
    @FXML private Button purchaseSubBtn;
    @FXML private Button loginReminderBtn;

    private PublicUser currentUser;
    private UserAccount editingUser;


    @FXML
    public void initialize() {
        if (editingUser != null) {
            populateUserData(editingUser);
            return;
        }

        currentUser = AppSession.getCurrentUser();
        if (currentUser != null) {
            populateUserData(currentUser);
            loginReminderBtn.setVisible(false);
        } else {
            disableAllFields();
        }
    }


    @FXML
    private void onSaveChanges() {
        if (currentUser == null) {
            statusLabel.setText("You must log in before saving changes.");
            return;
        }
        try {
            client.sendToServer(new UpdateUserDetailsRequest(
                    currentUser.getUserId(),
                    nameField.getText().trim(),
                    emailField.getText().trim(),
                    idField.getText().trim()
            ));
            statusLabel.setText("Details update requested…");
        } catch (Exception e) {
            statusLabel.setText("Failed to update details.");
        }
    }


    @FXML
    private void onChangePayment() {
        if (currentUser == null) {
            statusLabel.setText("Log in to change your payment method.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/PaymentMethodView.fxml"));
            Stage st = new Stage();
            st.setScene(new Scene(loader.load()));
            st.setTitle("Change Payment Method");
            st.showAndWait();

            PaymentMethodController controller = loader.getController();
            PaymentMethod newPayment = controller.getPaymentMethod();

            if (newPayment != null) {
                client.sendToServer(new UpdatePaymentMethodRequest(
                        currentUser.getUserId(), newPayment
                ));
                statusLabel.setText("💳 Payment method update requested.");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error opening payment window.");
        }
    }


    @FXML
    private void onPurchaseSubscription() {
        if (currentUser == null) {
            statusLabel.setText("Log in to purchase a subscription.");
            return;
        }
        statusLabel.setText("✅ Subscription purchased successfully!");
    }

    @FXML
    private void onRenewSubscription() {
        if (currentUser == null) {
            statusLabel.setText("Log in to renew a subscription.");
            return;
        }
        try {
            client.sendToServer(new RenewSubscriptionRequest(currentUser.getUserId()));
            statusLabel.setText("🔁 Subscription renewal requested.");
        } catch (Exception e) {
            statusLabel.setText("Failed to renew subscription.");
        }
    }

    public void onViewOrders(ActionEvent actionEvent) {
        try {
            App.setRoot("MyOrdersView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void populateUserData(PublicUser user) {
        if (user == null) return;

        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        idField.setText(user.getIdNumber());
        accountTypeLbl.setText(String.valueOf(user.getBranchType()));

        subscriptionExpiryLbl.setText(
                user.getSubscriptionExpirationDate() != null
                        ? user.getSubscriptionExpirationDate().toString()
                        : "—"
        );

        renewSubBtn.setVisible(user.isSubscriptionUser());
        purchaseSubBtn.setVisible(!user.isSubscriptionUser());
    }

    // טעינת נתונים ללקוח מסוג UserAccount (עבור מנהלת)
    private void populateUserData(UserAccount user) {
        if (user == null) return;

        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        idField.setText(user.getIdNumber());
        accountTypeLbl.setText(String.valueOf(user.getUserBranchType()));

        subscriptionExpiryLbl.setText(
                user.getSubscriptionExpirationDate() != null
                        ? user.getSubscriptionExpirationDate().toString()
                        : "—"
        );

        // מצב ניהול → לא מציגים כפתורי רכישה/חידוש
        renewSubBtn.setVisible(false);
        purchaseSubBtn.setVisible(false);
        loginReminderBtn.setVisible(false);
    }

    public void setEditableUser(UserAccount user) {
        this.editingUser = user;
        this.currentUser = null; // כדי שהמערכת לא תטען את המשתמש המחובר
        populateUserData(user);
    }


    public void onLoginRedirect(ActionEvent actionEvent) {
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onBackClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disableAllFields() {
        nameField.setDisable(true);
        emailField.setDisable(true);
        idField.setDisable(true);
        renewSubBtn.setDisable(true);
        purchaseSubBtn.setDisable(true);
        statusLabel.setText("");
    }
}

