package il.cshaifasweng.OCSFMediatorExample.client;

import Request.CancelOrderRequest;
import Request.Message;
import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.client.OrderRow;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import il.cshaifasweng.OCSFMediatorExample.entities.OrderLine;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class OrderDetailController {

    @FXML private Label orderIdLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;
    @FXML private Label branchLabel;
    @FXML private TableView<OrderLine> itemsTable;
    @FXML private TableColumn<OrderLine, String> itemNameCol;
    @FXML private TableColumn<OrderLine, Integer> itemQtyCol;
    @FXML private TableColumn<OrderLine, Double> itemPriceCol;
    @FXML private Label totalLabel;
    @FXML private Button closeBtn;
    @FXML private Button cancelOrderBtn;

    private OrderRow currentOrder;

    @FXML
    private void initialize() {
        // Register for EventBus to receive server responses
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    /** מקבל את ההזמנה מהמסך הקודם ומציג אותה */
    public void init(OrderRow order) {
        this.currentOrder = order;
        orderIdLabel.setText(order.getOrderId());
        dateLabel.setText(order.getDate());
        statusLabel.setText(order.getStatus());

        if (branchLabel != null) {
            branchLabel.setText(order.getBranchName() != null ? order.getBranchName() : "-");
        }

        // Show/hide cancel button based on order status
        if (cancelOrderBtn != null) {
            boolean canCancel = !"Cancelled".equalsIgnoreCase(order.getStatus()) &&
                               !"Delivered".equalsIgnoreCase(order.getStatus());
            cancelOrderBtn.setVisible(canCancel);
            cancelOrderBtn.setDisable(!canCancel);
        }

        double computedTotal = 0.0;

        // קישור עמודות לשדות של OrderLine
        itemNameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getItem().getName()));
        itemQtyCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getQuantity()).asObject());
        itemPriceCol.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getUnitPrice()).asObject());

        // מילוי הטבלה בפריטים של ההזמנה
        itemsTable.setItems(FXCollections.observableArrayList(order.getItems()));

        // חישוב הסכום הכולל
        for (OrderLine line : order.getItems()) {
            computedTotal += line.getQuantity() * line.getUnitPrice();
        }

        totalLabel.setText(String.format("$%.2f", computedTotal));
    }



    @FXML
    private void onClose(ActionEvent event) {
        Stage st = (Stage) closeBtn.getScene().getWindow();
        st.close();
    }

    @FXML
    private void onCancelOrder(ActionEvent event) {
        if (currentOrder == null) return;

        PublicUser currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            new Alert(Alert.AlertType.ERROR, "Please log in to cancel an order").showAndWait();
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to cancel this order?\n\n" +
                "Cancellation policy:\n" +
                "• 3+ hours before delivery: Full credit (100%)\n" +
                "• 1-3 hours before delivery: Partial credit (50%)\n" +
                "• Less than 1 hour before delivery: No credit (0%)",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Cancel Order #" + currentOrder.getOrderId());
        confirm.setTitle("Confirm Cancellation");
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            try {
                int orderId = Integer.parseInt(currentOrder.getOrderId());
                CancelOrderRequest request = new CancelOrderRequest(currentUser.getUserId(), orderId);
                client.sendToServer(request);
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Invalid order ID").showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to contact server").showAndWait();
            }
        }
    }

    @Subscribe
    public void onCancelOrderResponse(Message msg) {
        javafx.application.Platform.runLater(() -> {
            if (msg.getType().equals("cancelOrderOk")) {
                Alert success = new Alert(Alert.AlertType.INFORMATION, (String) msg.getData());
                success.setHeaderText("Order Cancelled");
                success.setTitle("Success");
                success.showAndWait();
                
                // Update status label and hide cancel button
                statusLabel.setText("Cancelled");
                if (cancelOrderBtn != null) {
                    cancelOrderBtn.setVisible(false);
                    cancelOrderBtn.setDisable(true);
                }
                
                // Refresh orders list (trigger refresh in MyOrdersController)
                // The actual order status will be updated from the server
                EventBus.getDefault().post(new Message("refreshOrders", null));
                
            } else if (msg.getType().equals("cancelOrderError")) {
                Alert error = new Alert(Alert.AlertType.ERROR, (String) msg.getData());
                error.setHeaderText("Cancellation Failed");
                error.setTitle("Error");
                error.showAndWait();
            }
        });
    }
}
