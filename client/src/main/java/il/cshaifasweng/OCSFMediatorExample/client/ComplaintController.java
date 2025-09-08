package il.cshaifasweng.OCSFMediatorExample.client;


import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import il.cshaifasweng.OCSFMediatorExample.entities.ComplaintEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Message;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

import java.io.IOException;
import java.time.LocalDateTime;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ComplaintController {
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Button backBtn;
    @FXML private Button sendButton; // if you give the send button fx:id in FXML, e.g., fx:id="sendButton"

    @FXML
    public void initialize() {
        // initial setup if needed
    }

    @FXML
    private void onSendComplaint() {
        String type = typeCombo.getValue();
        String desc = descriptionArea.getText().trim();
        if (type == null || desc.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select type and enter description.");
            alert.showAndWait();
            return;
        }

        Complaint complaint = new Complaint(type, desc);
        try {
            if (sendButton != null) sendButton.setDisable(true);
                LocalDateTime timeNow = LocalDateTime.now(); // Get the current time
                //ComplaintEvent complaintEvent = new Complaint(complaintTybe,textFieldName.getText(),textFieldEmail.getText(),textAreaTellUs.getText(),datePicker.getValue(),timeNow,restaurant_chosen,"Do",response,textFieldOrderNum.getText(),refundVal);
                //SimpleClient client;
                //client = SimpleClient.getClient();
                client.sendToServer(complaint);
                //checkLabel.setText("Sent Successfully");


            //client.sendToServer(new Message("newComplaint", complaint));
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Complaint submitted!");
            ok.showAndWait();
            // reset fields
            typeCombo.getSelectionModel().clearSelection();
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
}
