package il.cshaifasweng.OCSFMediatorExample.client;

import Request.CustomOrder;
import Request.Message;
import Request.PublicUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class CustomOrderController {

    @FXML private ComboBox<String> itemTypeCombo;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private TextField colorField;
    @FXML private TextArea notesArea;
    @FXML private Label messageLabel; // תווית להודעות

    @FXML
    public void initialize() {
        // Register for EventBus to receive server responses
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @FXML
    private void onSubmitClicked() {
        try {
            String type = itemTypeCombo.getValue();
            String color = colorField.getText().trim();
            String notes = notesArea.getText().trim();

            Double minPrice = null;
            Double maxPrice = null;
            try {
                if (!minPriceField.getText().isEmpty()) minPrice = Double.parseDouble(minPriceField.getText());
                if (!maxPriceField.getText().isEmpty()) maxPrice = Double.parseDouble(maxPriceField.getText());
            } catch (NumberFormatException e) {
                showMessage("Please enter valid numeric values for prices.", false);
                return;
            }

            if (type == null || type.isEmpty()) {
                showMessage("Please select the item type.", false);
                return;
            }

            // Check if user is logged in
            PublicUser currentUser = AppSession.getCurrentUser();
            if (currentUser == null) {
                showMessage("Please log in before submitting a custom design request.", false);
                return;
            }

            // בונים בקשת עיצוב אישי
            CustomOrder order = new CustomOrder(currentUser.getUserId(), type, minPrice, maxPrice, color, notes);
            client.sendToServer(order);

            showMessage("Submitting custom design request...", true);

            // סוגרים אחרי כמה שניות (רק אם רוצים)
//            new Thread(() -> {
//                try {
//                    Thread.sleep(2000);
//                    javafx.application.Platform.runLater(() ->
//                            ((Stage) itemTypeCombo.getScene().getWindow()).close());
//                } catch (InterruptedException ignored) {}
//            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Something went wrong while submitting your request.", false);
        }
    }

    @FXML
    private void onCancelClicked() {
        ((Stage) itemTypeCombo.getScene().getWindow()).close();
    }

    /** מציג הודעה על המסך בתווית (במקום Alert) */
    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success
                ? "-fx-text-fill: green; -fx-font-weight: bold;"
                : "-fx-text-fill: red; -fx-font-weight: bold;");
        messageLabel.setVisible(true);
    }

    public void onBackClicked(ActionEvent actionEvent) {
        try {
            // Clear category filter when navigating to catalog from custom order
            AppSession.setCameFromCategory(false);
            AppSession.setLastItemList(null);
            App.setRoot("CatalogView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onCustomOrderResponse(Message msg) {
        javafx.application.Platform.runLater(() -> {
            if (msg.getType().equals("customOrderOk")) {
                showMessage("✅ Custom design order submitted successfully! It will appear in 'My Orders'.", true);
                // Clear form after successful submission
                itemTypeCombo.setValue(null);
                minPriceField.clear();
                maxPriceField.clear();
                colorField.clear();
                notesArea.clear();
            } else if (msg.getType().equals("customOrderError")) {
                showMessage("❌ Failed to submit custom design order: " + msg.getData(), false);
            }
        });
    }
}
