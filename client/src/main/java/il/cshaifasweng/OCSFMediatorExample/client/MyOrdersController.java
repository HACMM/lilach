package il.cshaifasweng.OCSFMediatorExample.client;

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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

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
        // Defensive null-checks (in case injection failed)
        if (idCol == null || ordersTable == null || statusFilter == null) {
            System.err.println("FXML injection failed: one of required controls is null.");
        }

        // Column bindings
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().orderId));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date));
        itemsCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().itemsCount).asObject());
        totalCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().total).asObject());
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));

        // Details button column
        detailsCol.setCellFactory(createDetailsButtonFactory());

        // Status filter setup
        statusFilter.setItems(FXCollections.observableArrayList("All", "Pending", "Shipped", "Delivered", "Cancelled"));
        statusFilter.setValue("All");

        loadDummyData();
        ordersTable.setItems(masterData);

        ordersTable.setOnMouseClicked(this::onRowDoubleClick);
    }

    private void loadDummyData() {
        masterData.addAll(
                new OrderRow("ORD123", "2025-08-01", 3, 49.99, "Pending"),
                new OrderRow("ORD124", "2025-07-28", 1, 12.50, "Delivered"),
                new OrderRow("ORD125", "2025-07-20", 5, 99.00, "Shipped")
        );
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
                .filter(o -> search.isEmpty() || o.orderId.toLowerCase().contains(search))
                .filter(o -> status == null || status.equals("All") || o.status.equalsIgnoreCase(status))
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
            URL resource = getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/OrderDetailView.fxml");
            if (resource == null) {
                String msg = "OrderDetailView.fxml not found at expected location";
                System.err.println(msg);
                new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            OrderDetailController ctrl = loader.getController();
            ctrl.init(order);
            Stage st = new Stage();
            st.setTitle("Order " + order.orderId);
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

    public static class OrderRow {
        public final String orderId;
        public final String date;
        public final int itemsCount;
        public final double total;
        public final String status;
        public final List<OrderItem> items;

        public OrderRow(String orderId, String date, int itemsCount, double total, String status) {
            this.orderId = orderId;
            this.date = date;
            this.itemsCount = itemsCount;
            this.total = total;
            this.status = status;
            this.items = List.of(
                    new OrderItem("Rose Bouquet", 1, 20.0),
                    new OrderItem("Sunflower", 2, 14.99)
            );
        }
    }

    public static class OrderItem {
        public final String name;
        public final int qty;
        public final double price;

        public OrderItem(String name, int qty, double price) {
            this.name = name;
            this.qty = qty;
            this.price = price;
        }
    }
}
