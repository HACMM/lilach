package il.cshaifasweng.OCSFMediatorExample.client;

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

    private UserAccount currentUser;

    @FXML
    public void initialize() {
        currentUser = AppSession.getCurrentUser();
        if (currentUser != null) {
            nameField.setText(currentUser.getName());
            emailField.setText(currentUser.getEmail());
            idField.setText(currentUser.getIdNumber());
            accountTypeLbl.setText(String.valueOf(currentUser.getUserBranchType()));
            subscriptionExpiryLbl.setText(
                    currentUser.getSubscriptionExpirationDate() != null
                            ? currentUser.getSubscriptionExpirationDate().toString()
                            : "—"
            );
            renewSubBtn.setVisible(currentUser.isSubscriptionUser());
            purchaseSubBtn.setVisible(!currentUser.isSubscriptionUser());
        } else {
            // No user logged in → lock the form and show a hint
            nameField.setDisable(true);
            emailField.setDisable(true);
            idField.setDisable(true);
            renewSubBtn.setDisable(true);
            purchaseSubBtn.setDisable(true);
            statusLabel.setText("Please log in to view and edit personal details.");
        }
    }


    @FXML
    private void onSaveChanges() {
        if (currentUser == null) {
            statusLabel.setText("You must log in before saving changes.");
            return;
        }

        currentUser.setName(nameField.getText());
        currentUser.setEmail(emailField.getText());
        currentUser.setIdNumber(idField.getText());

        try {
            client.sendToServer(new Message("update user details", currentUser));
            statusLabel.setText("Details updated successfully!");
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/PaymentMethodView.fxml"));
            Stage st = new Stage();
            st.setScene(new Scene(loader.load()));
            st.setTitle("Change Payment Method");
            st.showAndWait();

            PaymentMethodController controller = loader.getController();
            PaymentMethod newPayment = controller.getPaymentMethod();

            if (newPayment != null) {
                currentUser.setDefaultPaymentMethod(newPayment);
                client.sendToServer(new Message("update payment method", currentUser));
                statusLabel.setText("💳 Payment method updated!");
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
        currentUser.activateSubscription();
        subscriptionExpiryLbl.setText(currentUser.getSubscriptionExpirationDate().toString());
        statusLabel.setText("🔁 Subscription renewed!");
    }

    public void onViewOrders(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/MyOrdersView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Personal Details");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
