package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.client.OrderRow;

import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.OrderLine;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.fxml.FXMLLoader;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class MyOrdersController {

    @FXML private TableView<OrderRow> ordersTable;
    @FXML private TableColumn<OrderRow, String> idCol;
    @FXML private TableColumn<OrderRow, String> dateCol;
    @FXML private TableColumn<OrderRow, Integer> itemsCol;
    @FXML private TableColumn<OrderRow, Double> totalCol;
    @FXML private TableColumn<OrderRow, String> statusCol;
    @FXML private TableColumn<OrderRow, Void> detailsCol;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button backBtn;

    private final ObservableList<OrderRow> masterData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        EventBus.getDefault().register(this);

        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderId()));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDate()));
        itemsCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getItemsCount()).asObject());
        totalCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotal()).asObject());
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        detailsCol.setCellFactory(createDetailsButtonFactory());

        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Shipped", "Delivered", "Cancelled"));
        statusFilter.setValue("All");

        ordersTable.setItems(masterData);
        ordersTable.setOnMouseClicked(this::onRowDoubleClick);

        // שלח בקשה לשרת לקבל את ההזמנות של המשתמש המחובר
        requestOrdersFromServer();
    }

    /** שולח בקשה לשרת עבור ההזמנות של המשתמש המחובר */
    private void requestOrdersFromServer() {
        try {
            PublicUser user = AppSession.getCurrentUser();
            if (user != null && client != null && client.isConnected()) {
                client.sendToServer("getOrdersForUser:" + user.getUserId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** מאזין לאירוע שמחזיר את רשימת ההזמנות מהשרת */
    @Subscribe
    public void onOrdersReceived(List<Order> orders) {
        Platform.runLater(() -> {
            masterData.clear();
            for (Order o : orders) {
                int itemCount = o.getOrderLines() != null ? o.getOrderLines().size() : 0;
                double total = o.getTotalPrice();
                String status = o.getStatus() != null ? o.getStatus() : "Pending";
                String date = o.getCreatedAt() != null ? o.getCreatedAt().toString() : "Unknown";
                String branchName = (o.getBranch() != null && o.getBranch().getName() != null)
                        ? o.getBranch().getName()
                        : "—";

                masterData.add(new OrderRow(
                        String.valueOf(o.getId()), date, itemCount, total, status, branchName,o.getOrderLines()
                ));
            }
            ordersTable.refresh();
        });
    }

    @Subscribe
    public void onRefreshOrders(Message msg) {
        if (msg.getType().equals("refreshOrders")) {
            requestOrdersFromServer();
        }
    }

    private Callback<TableColumn<OrderRow, Void>, TableCell<OrderRow, Void>> createDetailsButtonFactory() {
        return col -> new TableCell<>() {
            private final Button btn = new Button("View");
            {
                btn.getStyleClass().addAll("button", "secondary");
                btn.setOnAction(e -> {
                    OrderRow data = getTableView().getItems().get(getIndex());
                    openOrderDetail(data);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    @FXML
    private void onFilterClicked(ActionEvent event) {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String status = statusFilter.getValue();

        List<OrderRow> filtered = masterData.stream()
                .filter(o -> search.isEmpty() || o.getOrderId().toLowerCase().contains(search))
                .filter(o -> status == null || status.equals("All") || o.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());

        ordersTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void onRowDoubleClick(MouseEvent e) {
        if (e.getClickCount() < 2) return;
        OrderRow selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openOrderDetail(selected);
        }
    }

    private void openOrderDetail(OrderRow order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/OrderDetailView.fxml"
            ));
            Parent root = loader.load();
            OrderDetailController ctrl = loader.getController();
            ctrl.init(order);
            Stage st = new Stage();
            st.setTitle("Order " + order.getOrderId());
            st.setScene(new Scene(root));
            st.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open order detail: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onBack(ActionEvent event) {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    /** מודל שורה בטבלה */
//    public static class OrderRow {
//        public final String orderId;
//        public final String date;
//        public final int itemsCount;
//        public final double total;
//        public final String status;
//        public final String branchName;
//        public final List<OrderLine> items;
//
//        public OrderRow(String orderId, String date, int itemsCount, double total, String status, String branchName, List<OrderLine> items) {
//            this.orderId = orderId;
//            this.date = date;
//            this.itemsCount = itemsCount;
//            this.total = total;
//            this.status = status;
//            this.branchName = branchName;
//            this.items = items;
//        }
//    }
}
