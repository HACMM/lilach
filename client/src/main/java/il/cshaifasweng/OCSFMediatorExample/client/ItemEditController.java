package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import il.cshaifasweng.OCSFMediatorExample.client.Item;

public class ItemEditController {
    @FXML
    private TextField priceField;
    private Item item;

    public void init(Item item) {
        this.item = item;
        priceField.setText(String.valueOf(item.getPrice()));
    }

    @FXML private void onSave() {
        item.setPrice(Double.parseDouble(priceField.getText().trim()));
        ((Stage)priceField.getScene().getWindow()).close();
    }

    @FXML private void onCancel() {
        ((Stage)priceField.getScene().getWindow()).close();
    }
}