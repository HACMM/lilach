package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class CartViewController {
    @FXML private TableView<CartItem> table;
    @FXML private TableColumn<CartItem, String> nameCol;
    @FXML private TableColumn<CartItem, Integer> qtyCol;
    @FXML private TableColumn<CartItem, Double> priceCol;
    @FXML private TableColumn<CartItem, Double> subtotalCol;
    @FXML private TableColumn<CartItem, Void> removeCol;
    @FXML private Label totalLbl;
    @FXML private Label discountedTotalLbl;
    @FXML private Label discountLbl;
    @FXML private Button checkoutBtn;
    @FXML private Button selectPaymentBtn;
    @FXML private DatePicker deliveryDate;
    @FXML private ComboBox<String> deliveryTime;
    @FXML private RadioButton pickupRadio;
    @FXML private RadioButton deliveryRadio;
    @FXML private VBox deliveryFields;
    @FXML private TextField cityField;
    @FXML private TextField streetField;
    @FXML private TextField buildingField;
    @FXML private TextField recipientName;
    @FXML private TextField recipientPhone;
    @FXML private TextArea greetingField;
    @FXML private ToggleGroup receiveGroup;

    @FXML private Label messageLabel; // 🔹 הודעות למשתמש

    private PaymentMethod selectedPaymentMethod = null;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    private void initialize() {
        table.setItems(CartService.get().items());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setEditable(true);

        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItem().getName()));
        priceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getItem().getPrice()));
        priceCol.setCellFactory(col -> moneyCell());
        subtotalCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getSubtotal()));
        subtotalCol.setCellFactory(col -> moneyCell());
        qtyCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getQty()));
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        qtyCol.setOnEditCommit(ev -> {
            CartItem row = ev.getRowValue();
            int newQty = ev.getNewValue() == null ? 1 : Math.max(1, ev.getNewValue());
            row.setQty(newQty);
            refreshTotals();
            table.refresh();
        });

        removeCol.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Remove");
            {
                btn.setOnAction(e -> {
                    CartItem ci = getTableView().getItems().get(getIndex());
                    CartService.get().remove(ci);
                    refreshTotals();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.setPlaceholder(new Label("Your cart is empty. Add some flowers from the catalog 🌸"));
        CartService.get().items().addListener((javafx.collections.ListChangeListener<CartItem>) c -> refreshTotals());
        checkoutBtn.disableProperty().bind(Bindings.isEmpty(CartService.get().items()));

        // מוסתר כברירת מחדל
        discountedTotalLbl.setVisible(false);
        discountLbl.setVisible(false);
        messageLabel.setVisible(false);

        deliveryFields.setVisible(false);
        deliveryTime.getItems().addAll("Morning", "Afternoon", "Evening");
        pickupRadio.setSelected(true);

        deliveryRadio.selectedProperty().addListener((obs, oldV, newV) -> {
            deliveryFields.setVisible(newV);
            refreshTotals();
        });

        refreshTotals();
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
        double total = CartService.get().total();
        UserAccount currentUser = AppSession.getCurrentUser();

        if (currentUser != null && currentUser.isSubscriptionUser()) {
            double discount = total * 0.1;
            double newTotal = total - discount;

            totalLbl.setText(currency.format(total));
            totalLbl.setStyle("-fx-text-fill: gray; -fx-strikethrough: true; -fx-font-size: 16px;");
            discountedTotalLbl.setText(currency.format(newTotal));
            discountedTotalLbl.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 18px;");
            discountedTotalLbl.setVisible(true);
            discountLbl.setText("(10% subscriber discount)");
            discountLbl.setStyle("-fx-text-fill: #228B22; -fx-font-style: italic;");
            discountLbl.setVisible(true);
        } else {
            totalLbl.setText(currency.format(total));
            totalLbl.setStyle("-fx-font-size: 18px; -fx-text-fill: black;");
            discountLbl.setVisible(false);
            discountedTotalLbl.setVisible(false);
        }
        if (deliveryRadio.isSelected()) total += 20.0; // מחיר משלוח קבוע

    }

    @FXML
    private void onSelectPayment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/il/cshaifasweng/OCSFMediatorExample/client/PaymentMethodView.fxml"));
            Parent root = loader.load();
            PaymentMethodController ctrl = loader.getController();

            Stage st = new Stage();
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle("Select Payment Method");
            st.setScene(new Scene(root));
            st.showAndWait();

            selectedPaymentMethod = ctrl.getPaymentMethod();
            if (selectedPaymentMethod != null) {
                String last4 = selectedPaymentMethod.getCardNumber()
                        .substring(selectedPaymentMethod.getCardNumber().length() - 4);
                selectPaymentBtn.setText("Payment: **** **** **** " + last4 +
                        " (" + selectedPaymentMethod.getCardHolderName() + ")");
                showMessage("Payment method selected successfully.", true);
            } else {
                showMessage("Payment selection was cancelled.", false);
            }

        } catch (IOException e) {
            showMessage("Error loading payment window.", false);
            e.printStackTrace();
        }
    }

    @FXML
    private void onCheckout() {
        UserAccount currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            showMessage("Please log in before checking out.", false);
            return;
        }

        if (selectedPaymentMethod == null) {
            showMessage("Please select a payment method first.", false);
            return;
        }
        String greeting = greetingField.getText().trim();
        String deliveryType = pickupRadio.isSelected() ? "Pickup" : "Delivery";
        String recipient = recipientName.getText().trim();
        String phone = recipientPhone.getText().trim();
        LocalDate date = deliveryDate.getValue();
        String time = deliveryTime.getValue();
        if (deliveryType.equals("Delivery")) {
            if (recipient.isEmpty() || phone.isEmpty() ||
                    cityField.getText().isEmpty() || streetField.getText().isEmpty() || buildingField.getText().isEmpty()) {
                showMessage("Please fill in all delivery details.", false);
                return;
            }
        }

        if (date == null || time == null) {
            showMessage("Please select a delivery date and time.", false);
            return;
        }

        try {
            Set<Item> items = new HashSet<>(CartService.get().items()
                    .stream()
                    .map(CartItem::getItem)
                    .collect(Collectors.toList()));

            Order newOrder = new Order(currentUser, items);
            newOrder.setPaymentMethod(selectedPaymentMethod);
            newOrder.setGreeting(greeting);
            newOrder.setDeliveryType(deliveryType);
            newOrder.setDeliveryDateTime(LocalDateTime.of(date, LocalTime.parse("12:00"))); // או לפי time אם תשתמשי בשעות אמיתיות

            if (deliveryType.equals("Delivery")) {
                Address addr = new Address(
                        cityField.getText().trim(),
                        streetField.getText().trim(),
                        buildingField.getText().trim()
                );
                newOrder.setDeliveryAddress(addr);
                newOrder.setRecipientName(recipient);
                newOrder.setRecipientPhone(phone);
                newOrder.setDeliveryFee(20.0);
                newOrder.setTotalPrice(CartService.get().total());

            }
            client.sendToServer(new Message("newOrder", newOrder));

            // 🌸 שליחת מייל ללקוחה על אישור הזמנה
            String subject = "FlowerShop 🌷 - Order Confirmation";
            String body = String.format("""
                Hello %s,

                Thank you for your order! 🌸
                Your order number is: #%d

                Delivery type: %s
                Scheduled for: %s
                Total amount: %.2f₪

                We’ll notify you once your order is out for delivery.
                
                Love,
                The FlowerShop Team 💐
                """,
                    currentUser.getName(),
                    newOrder.getId(),
                    newOrder.getDeliveryType(),
                    newOrder.getDeliveryDateTime().toLocalDate(),
                    newOrder.getTotalPrice()
            );

            // שולח את המייל
            EmailSender.sendEmail(subject, body, currentUser.getEmail());

            showMessage("Your order has been placed successfully!", true);
            CartService.get().clear();
            clearFields();
            refreshTotals();

            // נניח מעבירים למסך ההזמנות אחרי כמה שניות
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        try {
                            App.setRoot("MyOrdersView");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException ignored) {
                }
            }).start();

        } catch (IOException e) {
            showMessage("Failed to send order to server.", false);
            e.printStackTrace();
        }
    }

        private void clearFields() {
            cityField.clear();
            streetField.clear();
            buildingField.clear();
            recipientName.clear();
            recipientPhone.clear();
            greetingField.clear();
        }


    /** מציג הודעה בתווית למטה במקום Alert */
    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success
                ? "-fx-text-fill: green; -fx-font-weight: bold;"
                : "-fx-text-fill: red; -fx-font-weight: bold;");
        messageLabel.setVisible(true);

        // ההודעה תיעלם אחרי 3 שניות
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> messageLabel.setVisible(false));
            } catch (InterruptedException ignored) {}
        }).start();
    }
}
