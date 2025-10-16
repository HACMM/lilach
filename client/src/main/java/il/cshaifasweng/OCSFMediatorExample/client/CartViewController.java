package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class CartViewController {
    @FXML private TableView<CartItem> table;
    @FXML private TableColumn<CartItem, String> nameCol;
    @FXML private TableColumn<CartItem, Integer> qtyCol;
    @FXML private TableColumn<CartItem, Double> priceCol;
    @FXML private TableColumn<CartItem, Double> subtotalCol;
    @FXML private TableColumn<CartItem, Void> removeCol;
    @FXML private Label totalLbl;
    @FXML private Button checkoutBtn;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    private void initialize() {
        table.setItems(CartService.get().items());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);

        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItem().getName()));

        // Price & Subtotal with currency formatting
        priceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getItem().getPrice()));
        priceCol.setCellFactory(col -> moneyCell());

        subtotalCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSubtotal()));
        subtotalCol.setCellFactory(col -> moneyCell());

        // Editable Qty (simple & reliable). Guarantees >= 1.
        qtyCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getQty()));
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        qtyCol.setOnEditCommit(ev -> {
            CartItem row = ev.getRowValue();
            int newQty = ev.getNewValue() == null ? 1 : Math.max(1, ev.getNewValue());
            row.setQty(newQty);
            refreshTotals();
            table.refresh();
        });

        // Remove button
        removeCol.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Remove");
            {
                btn.getStyleClass().addAll("button", "secondary");
                btn.setOnAction(e -> {
                    CartItem ci = getTableView().getItems().get(getIndex());
                    CartService.get().remove(ci);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Total binding + disable checkout when cart empty
        refreshTotals();
        CartService.get().items().addListener((javafx.collections.ListChangeListener<CartItem>) c -> refreshTotals());
        checkoutBtn.disableProperty().bind(Bindings.isEmpty(CartService.get().items()));
        checkoutBtn.setTooltip(new Tooltip("Place your order"));

        // Nice placeholder text
        table.setPlaceholder(new Label("Your cart is empty. Add some flowers from the catalog 🌸"));
    }

    private TableCell<CartItem, Double> moneyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : currency.format(val));
            }
        };
    }

    private void refreshTotals() {
        totalLbl.setText(currency.format(CartService.get().total()));
    }

    @FXML private void onClear() {
        if (!CartService.get().items().isEmpty()) {
            CartService.get().clear();
            refreshTotals();
        }
    }

    @FXML private void onBack() {
        try { App.setRoot("CatalogView"); } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void onCheckout() {
        try {
            UserAccount currentUser = AppSession.getCurrentUser();
            Set<Item> items = new HashSet<>(CartService.get().items()
                    .stream()
                    .map(CartItem::getItem)
                    .collect(Collectors.toList()));

            Order newOrder = new Order(currentUser, items);

            // TODO: ask the customer if he wants to change the payment method
            // newOrder.setPaymentMethod(customPaymentMethod);

            client.sendToServer(new Message("newOrder", newOrder));

            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Your order has been placed successfully!");
            ok.setHeaderText("Order Sent");
            ok.showAndWait();

            CartService.get().clear();
            App.setRoot("MyOrdersView");

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to send order to server.").showAndWait();
        }
    }


    // Optional: if you later want a Spinner cell instead of text editing,
    // I can swap qtyCol to a Spinner-based cell with validation.
}
