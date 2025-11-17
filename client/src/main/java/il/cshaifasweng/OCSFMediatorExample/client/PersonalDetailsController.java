package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;
import Request.RenewSubscriptionRequest;
import Request.PurchaseSubscriptionRequest;
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
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
        // Register for EventBus to receive update responses
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

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
            Parent root = loader.load();
            PaymentMethodController controller = loader.getController();

            // Load user's saved payment method if available
            if (currentUser.getDefaultPaymentMethod() != null) {
                java.util.List<PaymentMethod> savedCards = new java.util.ArrayList<>();
                savedCards.add(currentUser.getDefaultPaymentMethod());
                controller.loadSavedCards(savedCards);
                System.out.println("Loaded saved payment method for user: " + currentUser.getLogin());
            }

            Stage st = new Stage();
            st.setScene(new Scene(root));
            st.setTitle("Change Payment Method");
            st.showAndWait();

            PaymentMethod newPayment = controller.getPaymentMethod();

            if (newPayment != null) {
                client.sendToServer(new UpdatePaymentMethodRequest(
                        currentUser.getUserId(), newPayment
                ));
                // update the in-memory user so UI will use the new card from now on
                currentUser.setDefaultPaymentMethod(newPayment);
                AppSession.setCurrentUser(currentUser);
                statusLabel.setText("💳 Payment method update requested.");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error opening payment window.");
            e.printStackTrace();
        }
    }


    @FXML
    private void onPurchaseSubscription() {
        if (currentUser == null) {
            statusLabel.setText("Log in to purchase a subscription.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/PaymentMethodView.fxml"));
            Parent root = loader.load();
            PaymentMethodController controller = loader.getController();

            // Load user's saved payment method if available
            if (currentUser.getDefaultPaymentMethod() != null) {
                java.util.List<PaymentMethod> savedCards = new java.util.ArrayList<>();
                savedCards.add(currentUser.getDefaultPaymentMethod());
                controller.loadSavedCards(savedCards);
                System.out.println("Loaded saved payment method for subscription purchase");
            }

            Stage st = new Stage();
            st.setScene(new Scene(root));
            st.setTitle("Select Payment Method for Subscription");
            st.showAndWait();

            PaymentMethod selectedPayment = controller.getPaymentMethod();

            if (selectedPayment != null) {
                // Save payment method and purchase subscription
                try {
                    client.sendToServer(new PurchaseSubscriptionRequest(
                            currentUser.getUserId(), selectedPayment));
                    statusLabel.setText("Processing subscription purchase...");
                } catch (Exception e) {
                    statusLabel.setText("❌ Error sending subscription request.");
                    e.printStackTrace();
                }
            } else {
                statusLabel.setText("Payment method selection was cancelled.");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Error opening payment window.");
            e.printStackTrace();
        }
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

    @Subscribe
    public void onUpdateUserDetailsResponse(Message msg) {
        javafx.application.Platform.runLater(() -> {
            if (msg.getType().equals("updateUserDetailsOk")) {
                statusLabel.setText("✅ Details updated successfully!");
                // Refresh user data from session (it should be updated on next login, but we can update locally)
                if (currentUser != null) {
                    currentUser.setName(nameField.getText().trim());
                    currentUser.setEmail(emailField.getText().trim());
                    currentUser.setIdNumber(idField.getText().trim());
                }
            } else if (msg.getType().equals("updateUserDetailsError")) {
                statusLabel.setText("❌ Failed to update details: " + msg.getData());
            }
        });
    }

    @Subscribe
    public void onUpdatePaymentMethodResponse(Message msg) {
        javafx.application.Platform.runLater(() -> {
            if (msg.getType().equals("updatePaymentMethodOk")) {
                statusLabel.setText("✅ Payment method updated successfully!");
            } else if (msg.getType().equals("updatePaymentMethodError")) {
                statusLabel.setText("❌ Failed to update payment method: " + msg.getData());
            }
        });
    }

    @Subscribe
    public void onPurchaseSubscriptionResponse(Message msg) {
        javafx.application.Platform.runLater(() -> {
            if (msg.getType().equals("purchaseSubscriptionOk")) {
                statusLabel.setText("✅ Subscription purchased successfully! Payment method saved.");
                // Note: Payment method and subscription are saved in database
                // The user will see updated data on next login, but we update local data for immediate feedback
                if (currentUser != null) {
                    currentUser.setSubscriptionUser(true);
                    currentUser.setSubscriptionExpirationDate(java.time.LocalDate.now().plusYears(1));
                    // Refresh UI
                    purchaseSubBtn.setVisible(false);
                    renewSubBtn.setVisible(true);
                    subscriptionExpiryLbl.setText(currentUser.getSubscriptionExpirationDate().toString());
                }
            } else if (msg.getType().equals("purchaseSubscriptionError")) {
                statusLabel.setText("❌ Failed to purchase subscription: " + msg.getData());
            }
        });
    }

    @Subscribe
    public void onRenewSubscriptionResponse(Message msg) {
        javafx.application.Platform.runLater(() -> {
            if (msg.getType().equals("renewSubscriptionOk")) {
                statusLabel.setText("✅ Subscription renewed successfully!");
            } else if (msg.getType().equals("renewSubscriptionError")) {
                statusLabel.setText("❌ Failed to renew subscription: " + msg.getData());
            }
        });
    }
}

