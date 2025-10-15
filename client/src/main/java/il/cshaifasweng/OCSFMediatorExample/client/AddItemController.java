package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import java.util.function.Consumer;
import javafx.scene.control.ComboBox;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;
import org.greenrobot.eventbus.EventBus;

public class AddItemController {

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField priceField;
    @FXML private Label     errorLabel;
    @FXML private TextArea DescriptionArea;
    @FXML private ComboBox<String> branchComboBox;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
        try {
            client.sendToServer("#getAllBranches");
        } catch (Exception e) {
            errorLabel.setText("Failed to load branches.");
        }
    }


    private Consumer<Item> onSaved;
    public void setOnSaved(Consumer<Item> cb) { this.onSaved = cb; }


    @FXML
    private void handleSave(ActionEvent e) {
        errorLabel.setText("");
        try {
            String name = nameField.getText().trim();
            String type = typeField.getText().trim();
            String priceStr = priceField.getText().trim();
            String description = DescriptionArea.getText().trim();
            String branch = branchComboBox.getSelectionModel().getSelectedItem();

            if (name.isEmpty() || type.isEmpty() || priceStr.isEmpty() || description.isEmpty() || branch.isEmpty()) {
                errorLabel.setText("Please fill name, type, price and description.");
                return;
            }
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                errorLabel.setText("Price must be positive.");
                return;
            }

            Item newItem = new Item(name, type, description, price, null); // no image

            // TODO: Make AddItemMessage
            //client.sendToServer(new AddItemMessage(newItem));

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

    //TODO: fill the comboBox with all the branches
//    @Subscribe
//    public void onBranchListReceived(BranchListEvent event) {
//        branchComboBox.getItems().setAll(event.getBranches());
//}

}