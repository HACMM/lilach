package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.UpdateOrderStatusRequest;
import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.OrderLine;
import il.cshaifasweng.OCSFMediatorExample.client.OrderRow;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ManageOrdersController {

    @FXML private TableView<OrderRow> ordersTable;
    @FXML private TableColumn<OrderRow, String> idCol;
    @FXML private TableColumn<OrderRow, String> dateCol;
    @FXML private TableColumn<OrderRow, Integer> itemsCol;
    @FXML private TableColumn<OrderRow, Double> totalCol;
    @FXML private TableColumn<OrderRow, String> statusCol;
    @FXML private TableColumn<OrderRow, Void> updateStatusCol;
    @FXML private TableColumn<OrderRow, Void> detailsCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    private final ObservableList<OrderRow> masterData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        System.out.println("ManageOrdersController: Initializing...");
        EventBus.getDefault().register(this);
        System.out.println("ManageOrdersController: Registered with EventBus");

        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderId()));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()));
        itemsCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getItemsCount()).asObject());
        totalCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotal()).asObject());
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        updateStatusCol.setCellFactory(createStatusButtonFactory());
        detailsCol.setCellFactory(createDetailsButtonFactory());

        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Shipped", "Delivered", "Cancelled"));
        statusFilter.setValue("All");

        // Set the table to use masterData from the start
        ordersTable.setItems(masterData);
        System.out.println("ManageOrdersController: Table initialized with masterData (size: " + masterData.size() + ")");

        // Request orders immediately - no delay needed
        requestOrders();
    }

    private void requestOrders() {
        try {
            if (client != null && client.isConnected()) {
                // Request all orders for managers
                client.sendToServer("getAllOrders");
                System.out.println("ManageOrdersController: Requested all orders");
            } else {
                System.err.println("ManageOrdersController: Client not connected");
            }
        } catch (Exception e) {
            System.err.println("ManageOrdersController: Error requesting orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onOrdersReceived(List<Order> orders) {
        Platform.runLater(() -> {
            System.out.println("ManageOrdersController: Received " + (orders != null ? orders.size() : 0) + " orders");
            masterData.clear();
            if (orders != null && !orders.isEmpty()) {
                for (Order o : orders) {
                    try {
                        int itemCount = (o.getOrderLines() != null) ? o.getOrderLines().size() : 0;
                        String branchName = (o.getBranch() != null && o.getBranch().getName() != null) 
                                ? o.getBranch().getName() 
                                : "Unknown";
                        String date = (o.getCreatedAt() != null) ? o.getCreatedAt().toString() : "Unknown";
                        String status = (o.getStatus() != null) ? o.getStatus() : "Pending";
                        
                        masterData.add(new OrderRow(
                                String.valueOf(o.getId()),
                                date,
                                itemCount,
                                o.getTotalPrice(),
                                status,
                                branchName,
                                o.getOrderLines() != null ? o.getOrderLines() : new java.util.ArrayList<>()
                        ));
                    } catch (Exception e) {
                        System.err.println("ManageOrdersController: Error processing order " + o.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("ManageOrdersController: Received empty or null orders list");
            }
            
            // Ensure table is bound to masterData
            ordersTable.setItems(masterData);
            ordersTable.refresh(); // Force table refresh
            System.out.println("ManageOrdersController: Populated table with " + masterData.size() + " orders");
            System.out.println("ManageOrdersController: Table items count: " + ordersTable.getItems().size());
        });
    }

    private Callback<TableColumn<OrderRow, Void>, TableCell<OrderRow, Void>>
    createStatusButtonFactory() {
        return col -> new TableCell<>() {

            private final Button btn = new Button("Update");

            {
                btn.setStyle("-fx-background-color: #e7b3d1; -fx-text-fill: white; -fx-font-weight: bold;");
                btn.setOnAction(e -> {
                    OrderRow order = getTableView().getItems().get(getIndex());
                    openStatusUpdateDialog(order);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private void openStatusUpdateDialog(OrderRow order) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(order.getStatus(),
                "Pending", "Shipped", "Delivered", "Cancelled");
        dialog.setHeaderText("Update Status for Order #" + order.getOrderId());
        dialog.setTitle("Update Status");
        dialog.setContentText("Choose new status:");

        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                client.sendToServer(new UpdateOrderStatusRequest(Integer.parseInt(order.getOrderId()), newStatus, AppSession.getCurrentUser().getUserId()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Subscribe
    public void onStatusUpdated(Message msg) {
        if (msg.getType().equals("updateOrderStatusOk")) {
            System.out.println("ManageOrdersController: Order status updated successfully, refreshing table...");
            Platform.runLater(() -> {
                requestOrders();
                // Also refresh the table immediately
                ordersTable.refresh();
            });
        } else if (msg.getType().equals("updateOrderStatusError")) {
            System.err.println("ManageOrdersController: Failed to update order status: " + msg.getData());
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Update Status Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to update order status: " + (msg.getData() != null ? msg.getData().toString() : "Unknown error"));
                alert.showAndWait();
            });
        }
    }

    private Callback<TableColumn<OrderRow, Void>, TableCell<OrderRow, Void>>
    createDetailsButtonFactory() {
        return col -> new TableCell<>() {

            private final Button btn = new Button("View");

            {
                btn.setStyle("-fx-background-color: #a64f73; -fx-text-fill: white; -fx-font-weight: bold;");
                btn.setOnAction(e -> {
                    OrderRow row = getTableView().getItems().get(getIndex());
                    openDetail(row);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private void openDetail(OrderRow order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/OrderDetailView.fxml"));
            Parent root = loader.load();
            OrderDetailController ctrl = loader.getController();
            ctrl.init(order);

            Stage st = new Stage();
            st.setTitle("Order " + order.getOrderId());
            st.setScene(new Scene(root));
            st.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onFilterClicked() {
        String search = searchField.getText().toLowerCase().trim();
        String status = statusFilter.getValue();

        List<OrderRow> filtered = masterData.stream()
                .filter(o -> search.isEmpty() || o.getOrderId().contains(search))
                .filter(o -> status.equals("All") || o.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());

        ordersTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void onBack() {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static class OrderRow {
//        public final String orderId;
//        public final String date;
//        public final int itemsCount;
//        public final double totalPrice;
//        public final String status;
//        public final List<OrderLine> items;
//
//        public OrderRow(String orderId, String date,
//                        int itemsCount, double totalPrice,
//                        String status, List<OrderLine> items) {
//            this.orderId = orderId;
//            this.date = date;
//            this.itemsCount = itemsCount;
//            this.totalPrice = totalPrice;
//            this.status = status;
//            this.items = items;
//        }
//    }
}
