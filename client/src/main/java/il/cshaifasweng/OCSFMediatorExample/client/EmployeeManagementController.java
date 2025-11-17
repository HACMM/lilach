package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;


public class EmployeeManagementController {

    @FXML private TableView<UserAccount> employeeTable;
    @FXML private TableColumn<UserAccount, Integer> idCol;
    @FXML private TableColumn<UserAccount, String> nameCol;
    @FXML private TableColumn<UserAccount, String> emailCol;
    @FXML private TableColumn<UserAccount, Role> roleCol;
    @FXML private TableColumn<UserAccount, String> branchCol;

    @FXML private Button addEmployeeBtn;
    @FXML private Button removeEmployeeBtn;

    private final ObservableList<UserAccount> employees = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        idCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUserId()));
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        roleCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getRole()));
        branchCol.setCellValueFactory(c -> {
            if (c.getValue().getBranch() != null) {
                return new SimpleStringProperty(c.getValue().getBranch().getName());
            } else {
                return new SimpleStringProperty("-");
            }
        });

        employeeTable.setItems(employees);

        // בקשה לשרת להביא את רשימת העובדים
        try {
            client.sendToServer("getAllEmployees");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** כשלוחצים על "Add Employee" */
    @FXML
    private void onAddEmployeeClicked() {
        try {
            App.setRoot("AddEmployeeView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** כשלוחצים על "Remove Selected" */
    @FXML
    private void onRemoveEmployeeClicked() {
        UserAccount selected = employeeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select an employee to remove.").showAndWait();
            return;
        }

        try {
            client.sendToServer(new Message("removeEmployee", selected.getUserId()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** כפתור חזרה */
    @FXML
    private void onBackClicked() {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** מאזין לאירוע שמחזיר את רשימת העובדים */
    @Subscribe
    public void onEmployeesListReceived(Message msg) {
        if ("employeesList".equals(msg.getType())) {
            @SuppressWarnings("unchecked")
            List<UserAccount> list = (List<UserAccount>) msg.getData();

            javafx.application.Platform.runLater(() -> {
                System.out.println("EmployeeManagementController: got " + list.size() + " employees");
                employees.setAll(list);
                employeeTable.refresh();
            });
        }
    }




}
