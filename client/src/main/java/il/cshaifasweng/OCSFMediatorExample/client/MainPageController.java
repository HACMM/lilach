package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class MainPageController {

    @FXML private Button LogInBtn;
    @FXML private Button reportsBtn;
    @FXML private Button manageEmployeesBtn;
    @FXML private Button newsletterBtn;
    @FXML private Button manageCustomersBtn;


    @FXML
    private void initialize() {
        PublicUser user = AppSession.getCurrentUser();
        if (user == null || (user.getRole() != Role.MANAGER && user.getRole() != Role.NETWORK_MANAGER)) {
            manageEmployeesBtn.setVisible(false);
            newsletterBtn.setVisible(false);
            manageCustomersBtn.setVisible(false);
            reportsBtn.setVisible(false);
        }

        if (user != null) {
            LogInBtn.setVisible(false);
        }
    }

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
        PublicUser user = AppSession.getCurrentUser();
        if (user.getRole() == Role.CUSTOMER) {
            switchTo("MyOrdersView");
        } else {
            switchTo("OrderManagementView");
        }
    }

    @FXML
    private void onComplaintsClicked(ActionEvent event) {

        try {
            PublicUser currentUser = AppSession.getCurrentUser();

            if (currentUser == null) {
                // אם אין משתמש מחובר
                new Alert(Alert.AlertType.WARNING, "Please log in first.").showAndWait();
                return;
            }

            Role userRole = currentUser.getRole();

            if (userRole == Role.CUSTOMER) {
                App.setRoot("ComplaintView");
            } else if (userRole == Role.EMPLOYEE || userRole == Role.MANAGER || userRole == Role.NETWORK_MANAGER) {
                App.setRoot("ComplaintManagementView");
            } else {
                new Alert(Alert.AlertType.ERROR, "Unknown user role: " + userRole).showAndWait();
            }

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load complaints page.").showAndWait();
        }
    }

    @FXML
    private void onReportsClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportView.fxml"));
            Parent root = loader.load();

            Stage reportStage = new Stage();
            reportStage.setTitle("Reports");
            reportStage.setScene(new Scene(root));
            reportStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open Reports window.").showAndWait();
        }
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
            App.setRoot("PersonalDetailsView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void onManageEmployeesClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("EmployeeManagementView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onNewsletterClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("NewsletterView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onManageCustomersClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("CustomerManagementView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

