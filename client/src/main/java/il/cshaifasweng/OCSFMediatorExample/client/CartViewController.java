package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.PublicUser;
import Request.NewOrderRequest;
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
import javafx.event.ActionEvent;
import org.greenrobot.eventbus.Subscribe;

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
        PublicUser currentUser = AppSession.getCurrentUser();

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
    private void onBack(ActionEvent e) throws IOException {
        // Pick the view you want to return to:
        App.setRoot("CatalogView");    // or App.setRoot("CatalogView");
    }

    @FXML
    private void onClear(ActionEvent e) {

        CartService.get().clear();
        refreshTotals();
        showMessage("Cart cleared", true);
    }

    @FXML
    private void onCheckout() {
        PublicUser currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            showMessage("Please log in before checking out.", false);
            return;
        }

        if (selectedPaymentMethod == null) {
            showMessage("Please select a payment method first.", false);
            return;
        }

        String greeting     = greetingField.getText().trim();
        String deliveryType = pickupRadio.isSelected() ? "Pickup" : "Delivery";
        String recipient    = recipientName.getText().trim();
        String phone        = recipientPhone.getText().trim();
        LocalDate date      = deliveryDate.getValue();
        String timeSlot     = deliveryTime.getValue(); // "Morning" / "Afternoon" / "Evening"

        if ("Delivery".equals(deliveryType)) {
            if (recipient.isEmpty() || phone.isEmpty()
                    || cityField.getText().isEmpty()
                    || streetField.getText().isEmpty()
                    || buildingField.getText().isEmpty()) {
                showMessage("Please fill in all delivery details.", false);
                return;
            }
        }

        if (date == null || timeSlot == null) {
            showMessage("Please select a delivery date and time.", false);
            return;
        }

        try {
            // ---- Build the DTO we send to the server ----
            NewOrderRequest req = new NewOrderRequest();
            req.userId           = currentUser.getUserId();
            // Branch is optional for network/subscription users; keep null if you don't have it
            try { req.branchId = currentUser.getBranchId(); } catch (Throwable ignored) { req.branchId = null; }

            req.deliveryType     = deliveryType;
            LocalTime slotTime   = mapSlotToTime(timeSlot);
            req.deliveryDateTime = LocalDateTime.of(date, slotTime);
            req.greeting         = greeting;
            req.paymentMethod    = selectedPaymentMethod;
            req.deliveryFee      = "Delivery".equals(deliveryType) ? 20.0 : 0.0;

            if ("Delivery".equals(deliveryType)) {
                req.recipientName  = recipient;
                req.recipientPhone = phone;
                req.city           = cityField.getText().trim();
                req.street         = streetField.getText().trim();
                req.building       = buildingField.getText().trim();
            }

            // Order lines from cart (snapshot unit price now)
            for (CartItem ci : CartService.get().items()) {
                Item item = ci.getItem();
                int qty   = Math.max(1, ci.getQty());
                req.lines.add(new NewOrderRequest.Line(item.getId(), qty, item.getPrice()));
            }

            // ---- Send to server ----
            client.sendToServer(new Message("newOrder", req));

            // ---- Immediate confirmation email (client-side) ----
            // We don’t have the server order-id yet; this is a “request received” email.
            NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("he", "IL"));
            double itemsTotal = CartService.get().items().stream()
                    .mapToDouble(ci -> ci.getItem().getPrice() * Math.max(1, ci.getQty()))
                    .sum();

            // Subscriber discount preview (same logic as your UI)
            double discount = (currentUser.isSubscriptionUser() ? itemsTotal * 0.10 : 0.0);
            double afterDiscount = itemsTotal - discount;
            double estimatedTotal = afterDiscount + req.deliveryFee;

            String subject = "FlowerShop 🌷 – Order request received";
            String body = String.format("""
            Hello %s,

            We’ve received your order request. Here is a summary:

            Delivery type: %s
            Scheduled for: %s at %s
            Greeting: %s

            Items total: %s
            %sDelivery fee: %s
            -------------------------
            Estimated total: %s

            %s

            You’ll receive a second email with your official order number once it’s confirmed. 
            Thank you for shopping with us! 💐
            """,
                    currentUser.getName(),
                    deliveryType,
                    date.toString(), slotTime.toString(),
                    (greeting.isBlank() ? "(none)" : greeting),
                    currency.format(itemsTotal),
                    (discount > 0 ? "Subscriber discount (10%): -" + currency.format(discount) + "\n" : ""),
                    currency.format(req.deliveryFee),
                    currency.format(estimatedTotal),
                    "Delivery details: " + ("Delivery".equals(deliveryType)
                            ? String.format("%s, %s %s | Recipient: %s (%s)",
                            req.city, req.street, req.building, recipient, phone)
                            : "Pickup")
            );

            EmailSender.sendEmail(subject, body, currentUser.getEmail());

            // ---- UI & flow feedback (same as you had) ----
            showMessage("Your order has been placed successfully!", true);
            CartService.get().clear();
            clearFields();
            refreshTotals();

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        try { App.setRoot("MyOrdersView"); }
                        catch (IOException e) { e.printStackTrace(); }
                    });
                } catch (InterruptedException ignored) {}
            }).start();

        } catch (IOException e) {
            showMessage("Failed to send order to server.", false);
            e.printStackTrace();
        }
    }

    // helper (keep from previous message)
    private LocalTime mapSlotToTime(String slot) {
        if (slot == null) return LocalTime.of(12, 0);
        switch (slot) {
            case "Morning":   return LocalTime.of(9, 0);
            case "Afternoon": return LocalTime.of(13, 0);
            case "Evening":   return LocalTime.of(18, 0);
            default:          return LocalTime.of(12, 0);
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


    @Subscribe
    public void onOrderCreated(Message m) {
        if (!"newOrderOk".equals(m.getType())) return;
        Integer orderId = (Integer) m.getData();
        PublicUser user = AppSession.getCurrentUser();
        if (user == null) return;

        String subject = "FlowerShop 🌷 – Order confirmed #" + orderId;
        String body = "Hi " + user.getName() + ",\n\nYour order has been confirmed.\nOrder #: " + orderId + "\n\nThanks! 💐";
        EmailSender.sendEmail(subject, body, user.getEmail());
    }
}

