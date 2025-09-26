package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.io.IOException;

public class MainPageController {

    @FXML
    private void onLoginClicked(ActionEvent event) {
        switchTo("LoginView");
    }

    @FXML
    private void onBrowseClicked(ActionEvent event) {
        switchTo("CatalogView");
    }

    @FXML
    private void onOrdersClicked(ActionEvent event) {
        switchTo("MyOrdersView");
    }

    @FXML
    private void onCartClicked(ActionEvent event) {
        switchTo("CartView");
    }

    @FXML
    private void onComplaintsClicked(ActionEvent event) {
        switchTo("ComplaintView");
    }

    @FXML
    private void onReportsClicked(ActionEvent event) {
        switchTo("ReportView");
    }

    private void switchTo(String fxml) {
        try {
            App.setRoot(fxml);
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement ste : e.getStackTrace()) {
                sb.append(ste.toString()).append("\n");
            }
            new Alert(Alert.AlertType.ERROR, "Failed to load view: " + fxml + "\n" + e + "\n" + sb).showAndWait();
            e.printStackTrace();
        }
    }
}
