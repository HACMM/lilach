package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ComplaintManagementController {

    @FXML private TableView<Complaint> complaintTable;
    @FXML private TableColumn<Complaint, Integer> idCol;
    @FXML private TableColumn<Complaint, String> clientCol;
    @FXML private TableColumn<Complaint, String> emailCol;
    @FXML private TableColumn<Complaint, String> dateCol;
    @FXML private TableColumn<Complaint, String> statusCol;

    @FXML private TextArea descriptionArea;
    @FXML private TextArea responseArea;
    @FXML private TextField compensationField;
    @FXML private Label messageLabel;

    private Complaint selectedComplaint;
    private final ObservableList<Complaint> complaints = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);

        idCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getComplaintId()));
        clientCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getClientName()));
        emailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getClientEmail()));
        dateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(
                d.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        statusCol.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(
                d.getValue().isResolved() ? "✅" : "❌"));

        complaintTable.setItems(complaints);

        complaintTable.setOnMouseClicked(e -> {
            selectedComplaint = complaintTable.getSelectionModel().getSelectedItem();
            if (selectedComplaint != null) {
                descriptionArea.setText(selectedComplaint.getDescription());
                responseArea.clear();
                compensationField.clear();
                messageLabel.setVisible(false);
            }
        });

        requestComplaints();
    }

    /** בקשה לשרת לקבלת רשימת תלונות */
    private void requestComplaints() {
        try {
            client.sendToServer("getComplaints");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** מאזין לתשובת השרת עם רשימת התלונות */
    @Subscribe
    public void onComplaintsReceived(List<Complaint> list) {
        Platform.runLater(() -> {
            complaints.setAll(list);
        });
    }

    @FXML
    private void onSendResponse() {
        if (selectedComplaint == null) {
            showMessage("Please select a complaint first.", false);
            return;
        }

        String responseText = responseArea.getText().trim();
        if (responseText.isEmpty()) {
            showMessage("Response cannot be empty.", false);
            return;
        }

        double compensation = 0.0;
        if (!compensationField.getText().isEmpty()) {
            try {
                compensation = Double.parseDouble(compensationField.getText());
            } catch (NumberFormatException e) {
                showMessage("Compensation must be a number.", false);
                return;
            }
        }

        // 🔹 Only IDs / primitives from client
        Integer managerId = (AppSession.getCurrentUser() != null)
                ? AppSession.getCurrentUser().getUserId()
                : null;

        if (managerId == null) {
            showMessage("You must be logged in to resolve complaints.", false);
            return;
        }

        // Payload = [complaintId, responseText, compensation, resolved, managerUserId]
        var payload = java.util.List.of(
                selectedComplaint.getComplaintId(),
                responseText,
                compensation,
                Boolean.TRUE,
                managerId
        );

        try {
            client.sendToServer(new Message("resolveComplaint", payload));

            //send a local email to the customer for UX
            EmailSender.sendEmail(
                    "Response to your complaint 💐",
                    "Dear " + selectedComplaint.getClientName() + ",\n\n" +
                            "We have reviewed your complaint:\n\"" + selectedComplaint.getDescription() + "\"\n\n" +
                            "Response: " + responseText + "\n" +
                            (compensation > 0 ? "Compensation: " + compensation + "₪\n\n" : "\n") +
                            "Thank you for your patience 🌸\nFlowerShop Team",
                    selectedComplaint.getClientEmail()
            );

            showMessage("Response sent successfully!", true);
            requestComplaints(); // refresh list
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to send response.", false);
        }
    }


    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success
                ? "-fx-text-fill: green; -fx-font-weight: bold;"
                : "-fx-text-fill: #b14e75; -fx-font-weight: bold;");
        messageLabel.setVisible(true);
    }
}
