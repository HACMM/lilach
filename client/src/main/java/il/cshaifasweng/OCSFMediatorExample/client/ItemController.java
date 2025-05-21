package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.model.Item;

import java.io.IOException;

public class ItemController {
    @FXML private Label nameLabel, typeLabel, priceLabel;
    private Item item;

    public void init(Item item) {
        this.item = item;
        nameLabel.setText(item.getName());
        typeLabel.setText(item.getType());
        priceLabel.setText(String.valueOf(item.getPrice()));
    }

    @FXML
    private void onEditPrice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/ItemEditView.fxml")
            );
            Parent root = loader.load();
            ItemEditController c = loader.getController();
            c.init(item);
            Stage st = new Stage();
            st.setScene(new Scene(root));
            st.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}