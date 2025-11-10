package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;


public class OrderManagementController {

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> idColumn;
    @FXML private TableColumn<Order, String> customerColumn;
    @FXML private TableColumn<Order, String> branchColumn;
    @FXML private TableColumn<Order, String> statusColumn;
    @FXML private TableColumn<Order, Double> totalColumn;
    @FXML private TableColumn<Order, String> createdColumn;

    private ObservableList<Order> orders = FXCollections.observableArrayList();

    @FXML
    public void initialize() throws IOException {
        idColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        customerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUserAccount().getEmail()));
        branchColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBranch().getName()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        totalColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotalPrice()));
        createdColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCreatedAt().toString()));

        // בקשה לשרת
        try {
            client.sendToServer("#getAllOrders");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onApproveClicked() throws IOException {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                client.sendToServer("#updateOrderStatus " + selected.getId() + " APPROVED");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    private void onShipClicked() throws IOException {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                client.sendToServer("#updateOrderStatus " + selected.getId() + " SHIPPED");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FXML
    private void onBackClicked() {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
