package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class AddItemController {

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField priceField;
    @FXML private Label     errorLabel;

    private Consumer<CatalogItem> onSaved;
    public void setOnSaved(Consumer<CatalogItem> cb) { this.onSaved = cb; }

    @FXML
    private void handleSave(ActionEvent e) {
        errorLabel.setText("");
        try {
            String name = nameField.getText().trim();
            String type = typeField.getText().trim();
            String priceStr = priceField.getText().trim();

            if (name.isEmpty() || type.isEmpty() || priceStr.isEmpty()) {
                errorLabel.setText("Please fill name, type and price.");
                return;
            }
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                errorLabel.setText("Price must be positive.");
                return;
            }

            CatalogItem newItem = new CatalogItem(name, type, price, null); // no image

            // TODO: send to server if you persist items
            // SimpleClient.getClient().sendToServer(new AddItemMessage(newItem));

            if (onSaved != null) onSaved.accept(newItem);
            close();
        } catch (NumberFormatException nfe) {
            errorLabel.setText("Price must be a number (e.g., 12.5).");
        } catch (Exception ex) {
            errorLabel.setText("Failed to save item.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent e) { close(); }

    private void close() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
