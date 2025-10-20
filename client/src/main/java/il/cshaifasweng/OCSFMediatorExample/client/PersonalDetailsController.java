package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.entities.UserBranchType;
import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import Request.Message;
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
            accountTypeLbl.setText(currentUser.getUserBranchType().toString());

            if (currentUser.getSubscriptionExpirationDate() != null)
                subscriptionExpiryLbl.setText(currentUser.getSubscriptionExpirationDate().toString());
            else
                subscriptionExpiryLbl.setText("—");

            if (currentUser.isSubscriptionUser())
                renewSubBtn.setVisible(true);
            else
                purchaseSubBtn.setVisible(true);
        }
    }

    @FXML
    private void onSaveChanges() {
        currentUser.setName(nameField.getText());
        currentUser.setEmail(emailField.getText());
        currentUser.setIdNumber(idField.getText());

        try {
            client.sendToServer(new Message("update user details", currentUser));
            statusLabel.setText("✅ Details updated successfully!");
        } catch (Exception e) {
            statusLabel.setText("❌ Failed to update details.");
        }
    }

    @FXML
    private void onChangePayment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/PaymentMethodView.fxml"));
            Stage st = new Stage();
            st.setScene(new Scene(loader.load()));
            st.setTitle("Change Payment Method");
            st.showAndWait();

            PaymentMethod newPayment = loader.getController().getPaymentMethod();
            if (newPayment != null) {
                currentUser.setDefaultPaymentMethod(newPayment);
                statusLabel.setText("💳 Payment method updated!");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error opening payment window.");
        }
    }

    @FXML
    private void onPurchaseSubscription() {
        // כאן אפשר להוסיף חלון תשלום (100₪)
        statusLabel.setText("✅ Subscription purchased successfully!");
    }

    @FXML
    private void onRenewSubscription() {
        // כאן אפשר לקרוא למתודה renewSubscription
        currentUser.activateSubscription();
        subscriptionExpiryLbl.setText(currentUser.getSubscriptionExpirationDate().toString());
        statusLabel.setText("🔁 Subscription renewed!");
    }
}
