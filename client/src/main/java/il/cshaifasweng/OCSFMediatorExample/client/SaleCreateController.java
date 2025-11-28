package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;


public class SaleCreateController implements Initializable {

    @FXML private TextField saleNameField;
    @FXML private TextArea saleDescField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField imageLinkField;
    @FXML private ComboBox<DiscountType> discountTypeCombo;
    @FXML private TextField discountValueField;
    @FXML private ListView<Item> itemsList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // 1️⃣ בדיקת הרשאות
        PublicUser user = AppSession.getCurrentUser();
        if (user == null ||
                !(user.getRole() == Role.NETWORK_MANAGER ||
                        user.getRole() == Role.MANAGER ||
                        user.getRole() == Role.EMPLOYEE)) {

            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "You do not have permission to create promotions.");
            alert.show();
            return;
        }

        EventBus.getDefault().register(this);

        // 2️⃣ מילוי סוגי הנחה
        discountTypeCombo.getItems().addAll(DiscountType.values());

        // 3️⃣ בקשה להביא פריטים מהשרת
        try {
            client.sendToServer(new Message("#getItems", null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // אירוע: קיבלנו מהשרת את רשימת הפריטים
    @Subscribe
    public void onItemsReceived(List<?> list) {
        if (list == null || list.isEmpty() || !(list.get(0) instanceof Item)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Item> items = (List<Item>) list;

        Platform.runLater(() -> {
            itemsList.getItems().setAll(items);
        });
    }

    @FXML
    private void onCreateSale() {
        try {
            String name = saleNameField.getText();
            String desc = saleDescField.getText();

            if (name.isEmpty() || desc.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Name and description are required").show();
                return;
            }

            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Start and end dates are required").show();
                return;
            }

            if (discountTypeCombo.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Please select a discount type.").show();
                return;
            }

            String discountText = discountValueField.getText();
            if (discountText == null || discountText.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a discount value.").show();
                return;
            }

            double discount;
            try {
                discount = Double.parseDouble(discountText);
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Discount must be a number.").show();
                return;
            }

            if (itemsList.getSelectionModel().getSelectedItems().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please select at least one item for the sale.").show();
                return;
            }

            Date start = Date.from(startDatePicker.getValue()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(endDatePicker.getValue()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());

            // Create sale
            Sale sale = new Sale(name, desc, start, end);
            sale.setStatus(SaleStatus.Announced);
            sale.setDiscountType(discountTypeCombo.getValue());
            sale.setDiscountValue(discount);

            // Selected items
            List<Item> selected = itemsList.getSelectionModel().getSelectedItems();

            if (selected == null || selected.isEmpty()) {
                new Alert(Alert.AlertType.WARNING,
                        "Please select at least one item for the sale").show();
                return;
            }

            Item first = selected.get(0);
            String imageLinkFromItem = first.getImagePath();
            sale.setImageLink(imageLinkFromItem);

            List<ItemSale> itemSales = new ArrayList<>();
            for (Item item : selected) {
                ItemSale is = new ItemSale();
                is.setItem(item);
                is.setSale(sale);
                is.setDiscountType(discountTypeCombo.getValue());
                is.setDiscount(discount);  // use the parsed value
                itemSales.add(is);
            }

            Message msg = new Message("#create-sale", itemSales);
            client.sendToServer(msg);

            new Alert(Alert.AlertType.INFORMATION, "Sale created successfully!").show();
            App.setRoot("MainPage");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error creating sale.").show();
        }
    }


    @FXML
    private void onBack() {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
