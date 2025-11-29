package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;


public class CustomerManagementController {

    @FXML private TableView<UserAccount> customerTable;
    @FXML private TableColumn<UserAccount, Integer> idCol;
    @FXML private TableColumn<UserAccount, String> nameCol;
    @FXML private TableColumn<UserAccount, String> emailCol;
    @FXML private TableColumn<UserAccount, String> branchCol;
    @FXML private TableColumn<UserAccount, String> subscribedCol;
    @FXML private Button removeCustomerBtn;
    @FXML private Label messageLabel;

    private final ObservableList<UserAccount> customers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);

        idCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getUserId()));
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        branchCol.setCellValueFactory(c -> {
            if (c.getValue().getBranch() != null)
                return new SimpleStringProperty(c.getValue().getBranch().getName());
            return new SimpleStringProperty("-");
        });
        subscribedCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isSubscriptionUser() ? "Yes" : "No"));

        customerTable.setItems(customers);

        // בקשת רשימת לקוחות מהשרת
        requestCustomers();
    }

    private void requestCustomers() {
        try {
            client.sendToServer("getCustomers");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** מאזין לרשימת לקוחות */
    @Subscribe
    public void onCustomersReceived(Message msg) {
        if ("customersList".equals(msg.getType())) {
            @SuppressWarnings("unchecked")
            List<UserAccount> list = (List<UserAccount>) msg.getData();

            Platform.runLater(() -> {
                System.out.println("CustomerManagementController: got " + list.size() + " customers");
                customers.setAll(list);
                customerTable.refresh();
            });
        }
    }



    @FXML
    private void onChangeDetailsClicked() {
        UserAccount selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a customer first.").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/PersonalDetailsView.fxml")
            );
            Parent root = loader.load();

            PersonalDetailsController ctrl = loader.getController();
            ctrl.setEditableUser(selected); // פונקציה חדשה שנוסיף לשליטה במצב "עריכת משתמש אחר"

            Stage stage = new Stage();
            stage.setTitle("Edit Customer Details");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** הסרת לקוח נבחר */
    @FXML
    private void onRemoveCustomerClicked() {
        UserAccount selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a customer to remove.").showAndWait();
            return;
        }
        try {
            client.sendToServer(new Message("removeCustomer", selected.getUserId()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onRemoveCustomerError(Message msg) {
        if ("removeCustomerError".equals(msg.getType())) {
            String error = (String) msg.getData();
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.ERROR, error).showAndWait();
            });
        }
    }


    /** רענון ידני */
    @FXML
    private void onRefreshClicked() {
        requestCustomers();
    }

    /** חזרה למסך הראשי */
    @FXML
    private void onBackClicked() {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onRefreshCustomers(Message msg) {
        if ("refreshCustomers".equals(msg.getType())) {
            Platform.runLater(() -> {
                requestCustomers();   // קורא שוב לשרת
            });
        }
    }

}
