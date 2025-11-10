package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;


public class AddEmployeeController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private Button saveBtn;

    @FXML
    public void initialize() {
        // טען תפקידים זמינים (לרוב רק EMPLOYEE)
        roleCombo.getItems().setAll(Role.EMPLOYEE, Role.MANAGER);

        // בקשה לשרת להביא סניפים
        try {
            client.sendToServer("show branches");
        } catch (IOException e) {
            e.printStackTrace();
        }

        EventBus.getDefault().register(this);
    }

    /** מאזין לרשימת הסניפים מהשרת */
    @Subscribe
    public void onBranchesList(BranchListEvent event) {
        Platform.runLater(() -> branchCombo.getItems().setAll(event.getBranches()));
    }

    /** שמירה של עובד חדש */
    @FXML
    private void onSaveClicked() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        Role role = roleCombo.getValue();
        Branch branch = branchCombo.getValue();

        if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || role == null || branch == null) {
            new Alert(Alert.AlertType.WARNING, "Please fill in all fields.").showAndWait();
            return;
        }

        UserAccount newEmployee = new UserAccount(username, password, name, email);
        newEmployee.setRole(role);
        newEmployee.setBranch(branch);

        try {
            client.sendToServer(new Message("addEmployee", newEmployee));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** מאזין לתשובת השרת */
    @Subscribe
    public void onEmployeeAdded(Message msg) {
        if ("addEmployeeOk".equals(msg.getType())) {
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.INFORMATION, "Employee added successfully!").showAndWait();
                try {
                    App.setRoot("EmployeeManagementView");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if ("addEmployeeError".equals(msg.getType())) {
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Failed to add employee: " + msg.getData()).showAndWait()
            );
        }
    }

    /** כפתור חזרה */
    @FXML
    private void onBackClicked() {
        try {
            App.setRoot("EmployeeManagementView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
