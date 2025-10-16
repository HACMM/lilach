package il.cshaifasweng.OCSFMediatorExample.client;


import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import Request.Message;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ComplaintController {
    @FXML
    private ComboBox<Branch> BranchCombo;

    @FXML
    private TextField EmailTextField;

    @FXML
    private TextField NameTextField;

    @FXML
    private TextField OrderNumberTextField;

    @FXML
    private Button backBtn;

    @FXML
    private Button sendButton;

    @FXML
    private TextArea descriptionArea;


    @FXML
    public void initialize() {
            EventBus.getDefault().register(this);
            try {
                client.sendToServer("#getAllBranches");
            } catch (IOException e) {
                System.err.println("Failed to request branch list: " + e.getMessage());
            }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    @FXML
    private void onSendComplaint() {
        //String type = typeCombo.getValue();
        String desc = descriptionArea.getText().trim();
        String name = NameTextField.getText().trim();
        String order = OrderNumberTextField.getText().trim();
        String email = EmailTextField.getText().trim();
        Branch branch = BranchCombo.getSelectionModel().getSelectedItem();
        if (desc.isEmpty() || branch == null || name.isEmpty() || order.isEmpty() || email.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please fill all fields");
            alert.showAndWait();
            return;
        } else { if (!isValidEmail(email)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter a valid email address");
            alert.showAndWait();
            return;
            }
        }

        try {
            if (sendButton != null) sendButton.setDisable(true);
            {
                LocalDateTime timeNow = LocalDateTime.now(); // Get the current time
                Complaint complaint = new Complaint(branch, order, name, email, desc);
                client.sendToServer(new Message("newComplaint", complaint));
            }

            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Complaint submitted!");
            ok.showAndWait();
            // reset fields
            BranchCombo.getSelectionModel().clearSelection();
            NameTextField.clear();
            OrderNumberTextField.clear();
            EmailTextField.clear();
            descriptionArea.clear();
        } catch (IOException e) {
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to send to server.");
            err.showAndWait();
        } finally {
            if (sendButton != null) sendButton.setDisable(false);
        }
    }

    @FXML
    private void onBack(ActionEvent event) {
        try {
            App.setRoot("MainPage"); // or whichever view should be the previous screen
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not navigate back.");
            alert.showAndWait();
        }
    }

    @Subscribe
    public void onBranchListReceived(BranchListEvent event) {
        Platform.runLater(() -> {
            BranchCombo.getItems().setAll(event.getBranches());
        });
    }
}
