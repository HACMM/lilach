package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ItemEditController {
    @FXML
    private TextField priceField;
    private Item item;
    private ItemController parentController;

    public void setParentController(ItemController parentController) {
        this.parentController = parentController;
    }

    public void init(Item item) {
        this.item = item;
        priceField.setText(String.valueOf(item.getPrice()));
    }



    @FXML
    private void onSave() {
        try {
            if (item == null) {
                System.err.println("[ERROR] item is null!");
                return;
            }

            double newPrice = Double.parseDouble(priceField.getText().trim());
            item.setPrice(newPrice);
            System.out.println("[CLIENT] Sending item: " + item.getName() + ", new price: " + newPrice);
            if (parentController != null) {
                parentController.updatePriceLabel(item.getPrice());
            }

            client.sendToServer(item);

            ((Stage) priceField.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void onCancel() {
        ((Stage) priceField.getScene().getWindow()).close();
    }
}