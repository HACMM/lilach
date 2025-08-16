package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

public class ReportViewController {

    @FXML
    private TextArea reportText;

    @FXML
    private void onSubmit(ActionEvent event) {
        String report = reportText.getText().trim();
        if (report.isEmpty()) {
            showAlert("Report cannot be empty.");
            return;
        }

        // TODO: integrate with your existing client messaging (like ComplaintController does)
        // Example placeholder feedback:
        showAlert("Report submitted.");
        reportText.clear();
    }

    @FXML
    private void onBack(ActionEvent event) {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to go back.");
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
