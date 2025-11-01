package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class MainPageController {

    @FXML
    private void onLoginClicked(ActionEvent event) {
        switchTo("Login");
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


    @FXML
    private void onProfileClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/PersonalDetailsView.fxml"));
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

