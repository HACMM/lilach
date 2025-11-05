package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import il.cshaifasweng.OCSFMediatorExample.entities.OrderLine;

public class OrderDetailController {

    @FXML private Label orderIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<OrderLine> itemsTable;
    @FXML private TableColumn<OrderLine, String> itemNameCol;
    @FXML private TableColumn<OrderLine, Integer> itemQtyCol;
    @FXML private TableColumn<OrderLine, Double> itemPriceCol;
    @FXML private Label totalLabel;
    @FXML private Button closeBtn;

    /** מקבל את ההזמנה מהמסך הקודם ומציג אותה */
    public void init(MyOrdersController.OrderRow order) {
        orderIdLabel.setText(order.orderId);
        dateLabel.setText(order.date);
        statusLabel.setText(order.status);

        double computedTotal = 0.0;

        // קישור עמודות לשדות של OrderLine
        itemNameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));
        itemQtyCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getQuantity()).asObject());
        itemPriceCol.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getUnitPrice()).asObject());

        // מילוי הטבלה בפריטים של ההזמנה
        itemsTable.setItems(FXCollections.observableArrayList(order.items));

        // חישוב הסכום הכולל
        for (OrderLine line : order.items) {
            computedTotal += line.getQuantity() * line.getUnitPrice();
        }

        totalLabel.setText(String.format("$%.2f", computedTotal));
    }

    @FXML
    private void onClose(ActionEvent event) {
        Stage st = (Stage) closeBtn.getScene().getWindow();
        st.close();
    }
}
