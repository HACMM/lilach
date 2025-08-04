package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

public class OrderDetailController {
    @FXML private Label orderIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<MyOrdersController.OrderItem> itemsTable;
    @FXML private TableColumn<MyOrdersController.OrderItem, String> itemNameCol;
    @FXML private TableColumn<MyOrdersController.OrderItem, Integer> itemQtyCol;
    @FXML private TableColumn<MyOrdersController.OrderItem, Double> itemPriceCol;
    @FXML private Label totalLabel;
    @FXML private Button closeBtn;

    public void init(MyOrdersController.OrderRow order) {
        orderIdLabel.setText(order.orderId);
        dateLabel.setText(order.date);
        statusLabel.setText(order.status);
        double computedTotal = 0.0;

        itemNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        itemQtyCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().qty).asObject());
        itemPriceCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().price).asObject());

        itemsTable.setItems(FXCollections.observableArrayList(order.items));

        for (MyOrdersController.OrderItem it : order.items) {
            computedTotal += it.qty * it.price;
        }
        totalLabel.setText(String.format("$%.2f", computedTotal));
    }

    @FXML
    private void onClose(ActionEvent event) {
        Stage st = (Stage) closeBtn.getScene().getWindow();
        st.close();
    }
}
