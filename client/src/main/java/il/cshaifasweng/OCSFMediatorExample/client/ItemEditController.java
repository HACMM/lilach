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

    public void init(Item item) {
        this.item = item;
        priceField.setText(String.valueOf(item.getPrice()));
    }

    @FXML
    private void onSave() {
        try {
            item.setPrice(Double.parseDouble(priceField.getText().trim()));
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