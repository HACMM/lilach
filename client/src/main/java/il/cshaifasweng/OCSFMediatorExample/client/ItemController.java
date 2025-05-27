package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class ItemController {
    @FXML
    private TextArea descriptionTextArea;
    @FXML private Label nameLabel, typeLabel, priceLabel;
    private Item item;


    public void init(Item item) {
        this.item = item;
        nameLabel.setText(item.getName());
        typeLabel.setText(item.getType());
        priceLabel.setText(String.valueOf(item.getPrice())+"$");
        descriptionTextArea.setText(item.getDescription());
    }

    @FXML
    private void onEditPrice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/ItemEditView.fxml")
            );
            Parent root = loader.load();
            ItemEditController c = loader.getController();
            Stage st = new Stage();
            st.setScene(new Scene(root));
            c.init(item);
            c.setStage(st);
            st.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}