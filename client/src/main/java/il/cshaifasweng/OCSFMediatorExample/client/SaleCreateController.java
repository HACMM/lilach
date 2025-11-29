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
    @FXML private RadioButton individualItemsRadio;
    @FXML private RadioButton byTypeRadio;
    @FXML private RadioButton allItemsRadio;
    @FXML private ComboBox<String> itemTypeCombo;
    @FXML private Label itemTypeLabel;
    @FXML private Label itemsCountLabel;
    
    private List<Item> allItems = new ArrayList<>();

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

        // 3️⃣ Setup selection mode radio buttons
        ToggleGroup selectionGroup = new ToggleGroup();
        individualItemsRadio.setToggleGroup(selectionGroup);
        byTypeRadio.setToggleGroup(selectionGroup);
        allItemsRadio.setToggleGroup(selectionGroup);
        individualItemsRadio.setSelected(true); // Default to individual items
        
        // Initially hide type combo
        itemTypeCombo.setVisible(false);
        itemTypeLabel.setVisible(false);
        
        // Add listeners for radio button changes
        individualItemsRadio.setOnAction(e -> {
            itemsList.getSelectionModel().clearSelection();
            itemTypeCombo.setVisible(false);
            itemTypeLabel.setVisible(false);
            itemsList.setDisable(false);
            updateItemsCountLabel();
        });
        
        byTypeRadio.setOnAction(e -> {
            itemsList.getSelectionModel().clearSelection();
            itemTypeCombo.setVisible(true);
            itemTypeLabel.setVisible(true);
            itemsList.setDisable(true);
            updateItemsByType();
            updateItemsCountLabel();
        });
        
        allItemsRadio.setOnAction(e -> {
            itemsList.getSelectionModel().clearSelection();
            itemTypeCombo.setVisible(false);
            itemTypeLabel.setVisible(false);
            itemsList.setDisable(true);
            updateItemsCountLabel();
        });

        // 4️⃣ בקשה להביא פריטים מהשרת
        try {
            client.sendToServer(new Message("#getItems", null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void updateItemsByType() {
        if (itemTypeCombo.getValue() != null && !allItems.isEmpty()) {
            String selectedType = itemTypeCombo.getValue();
            List<Item> filtered = allItems.stream()
                    .filter(item -> item.getType() != null && item.getType().equalsIgnoreCase(selectedType))
                    .collect(Collectors.toList());
            itemsList.getItems().setAll(filtered);
            updateItemsCountLabel();
        }
    }
    
    private void updateItemsCountLabel() {
        if (itemsCountLabel == null) return;
        
        int count = 0;
        if (individualItemsRadio.isSelected()) {
            count = itemsList.getSelectionModel().getSelectedItems().size();
            itemsCountLabel.setText(count > 0 ? count + " item(s) selected" : "No items selected");
        } else if (byTypeRadio.isSelected()) {
            if (itemTypeCombo.getValue() != null) {
                String selectedType = itemTypeCombo.getValue();
                count = (int) allItems.stream()
                        .filter(item -> item.getType() != null && item.getType().equalsIgnoreCase(selectedType))
                        .count();
                itemsCountLabel.setText(count + " item(s) of type '" + selectedType + "' will be included");
            } else {
                itemsCountLabel.setText("Please select an item type");
            }
        } else if (allItemsRadio.isSelected()) {
            count = allItems.size();
            itemsCountLabel.setText("All " + count + " item(s) in the store will be included");
        }
        
        itemsCountLabel.setVisible(true);
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
            allItems = new ArrayList<>(items);
            itemsList.getItems().setAll(items);
            
            // Populate item types combo with unique types from items
            Set<String> types = items.stream()
                    .map(Item::getType)
                    .filter(type -> type != null && !type.isEmpty())
                    .collect(Collectors.toSet());
            List<String> sortedTypes = new ArrayList<>(types);
            sortedTypes.sort(String::compareTo);
            itemTypeCombo.getItems().setAll(sortedTypes);
            
            // Add listener to type combo
            itemTypeCombo.setOnAction(e -> {
                updateItemsByType();
                updateItemsCountLabel();
            });
            
            // Add listener to items list selection for individual mode
            itemsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (individualItemsRadio.isSelected()) {
                    updateItemsCountLabel();
                }
            });
            
            // Also listen to multiple selection changes
            itemsList.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener.Change<? extends Item> c) -> {
                if (individualItemsRadio.isSelected()) {
                    updateItemsCountLabel();
                }
            });
            
            updateItemsCountLabel();
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

            Date start = Date.from(startDatePicker.getValue()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(endDatePicker.getValue()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());

            // Create sale
            Sale sale = new Sale(name, desc, start, end);
            sale.setStatus(SaleStatus.Announced);
            sale.setDiscountType(discountTypeCombo.getValue());
            sale.setDiscountValue(discount);

            // Determine selected items based on selection mode
            List<Item> selected = new ArrayList<>();
            
            if (individualItemsRadio.isSelected()) {
                selected = new ArrayList<>(itemsList.getSelectionModel().getSelectedItems());
                if (selected.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Please select at least one item for the sale.").show();
                    return;
                }
            } else if (byTypeRadio.isSelected()) {
                if (itemTypeCombo.getValue() == null || itemTypeCombo.getValue().isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "Please select an item type.").show();
                    return;
                }
                String selectedType = itemTypeCombo.getValue();
                selected = allItems.stream()
                        .filter(item -> item.getType() != null && item.getType().equalsIgnoreCase(selectedType))
                        .collect(Collectors.toList());
                if (selected.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "No items found for the selected type.").show();
                    return;
                }
            } else if (allItemsRadio.isSelected()) {
                selected = new ArrayList<>(allItems);
                if (selected.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "No items available in the store.").show();
                    return;
                }
            }

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

            // Don't navigate immediately - wait for server response
            // The navigation will happen in the @Subscribe method below

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error creating sale: " + e.getMessage()).show();
        }
    }
    
    @Subscribe
    public void onSaleCreated(Message msg) {
        Platform.runLater(() -> {
            if ("createSaleOk".equals(msg.getType())) {
                // Unregister from EventBus before navigating away
                if (EventBus.getDefault().isRegistered(this)) {
                    EventBus.getDefault().unregister(this);
                }
                new Alert(Alert.AlertType.INFORMATION, "Sale created successfully!").showAndWait();
                try {
                    App.setRoot("MainPage");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if ("createSaleError".equals(msg.getType())) {
                new Alert(Alert.AlertType.ERROR, "Failed to create sale: " + msg.getData()).showAndWait();
            }
        });
    }

    @FXML
    private void onBack() {
        try {
            // Unregister from EventBus before navigating away
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this);
            }
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
