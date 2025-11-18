package il.cshaifasweng.OCSFMediatorExample.client;

import Request.SignupRequest;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.client.Events.SignupResponseEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import il.cshaifasweng.OCSFMediatorExample.client.Events.WarningEvent;
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

    @FXML private TextField idField;
    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         warningLabel;
    @FXML private Button addPaymentBtn;
    @FXML private Button backBtn;
    @FXML private ComboBox<String> accountTypeCombo;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private Label branchLabel;
    //@FXML private Label subscriptionNote;
    @FXML private Button ChooseCard;
    @FXML private Button checkoutBtn;



    private PaymentMethod selectedPaymentMethod;
    // existing fields and methods...


    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        accountTypeCombo.getItems().addAll("Branch Account","Network Account","Yearly Subscription");
        accountTypeCombo.setValue("Branch Account");
        accountTypeCombo.setOnAction(e -> {
            String selected = accountTypeCombo.getValue();
            ChooseCard.setVisible("Yearly Subscription".equals(selected));
            checkoutBtn.setVisible("Yearly Subscription".equals(selected));

            boolean isBranchAccount = "Branch Account".equals(selected);
            branchLabel.setVisible(isBranchAccount);
            branchLabel.setManaged(isBranchAccount);
            branchCombo.setVisible(isBranchAccount);
            branchCombo.setManaged(isBranchAccount);
        });
        accountTypeCombo.getOnAction().handle(null);

        try {
            client.sendToServer("#getAllBranches");
        } catch (IOException e) {
            e.printStackTrace();
            warningLabel.setText("Failed to load branches from server.");
        }
        branchCombo.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Branch item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        branchCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Branch item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }


    // Call this when leaving the view (e.g., onBack or when you navigate away)
    private void unregisterBus() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    @FXML
    private void onBack(ActionEvent event) {
        unregisterBus();
        try { App.setRoot("Login"); } catch (IOException e) { e.printStackTrace(); }
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
        String idNumber = idField.getText().trim();

        // make sure nothing’s blank
        if (name.isEmpty() || email.isEmpty() ||
                username.isEmpty() || password.isEmpty() || idNumber.isEmpty()) {
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

        Integer branchId = null;
        if (branchType == UserBranchType.BRANCH) {
            Branch selectedBranch = branchCombo.getValue();
            if (selectedBranch == null) {
                warningLabel.setText("Please select a branch.");
                return;
            }
            branchId = selectedBranch.getId();
        } else {
            // Network/subscription users arent tied to a specific branch
            branchId = null;
        }

        try {
            client.sendToServer(new SignupRequest(username,password,name,email, idNumber,selectedPaymentMethod, branchType, branchId));
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
            
            // If user already added a payment method in this session, show it as saved
            if (selectedPaymentMethod != null) {
                java.util.List<PaymentMethod> savedCards = new java.util.ArrayList<>();
                savedCards.add(selectedPaymentMethod);
                controller.loadSavedCards(savedCards);
                System.out.println("Loaded previously added payment method from signup session");
            }

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

    @Subscribe
    public void onBranchListEvent(BranchListEvent event) {
        javafx.application.Platform.runLater(() -> {
            branchCombo.getItems().setAll(event.getBranches());
            if (!event.getBranches().isEmpty()) {
                branchCombo.getSelectionModel().selectFirst();
            }
        });
    }


}