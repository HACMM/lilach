package il.cshaifasweng.OCSFMediatorExample.client;

import Request.CustomOrder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

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

            // בונים בקשת עיצוב אישי
            CustomOrder order = new CustomOrder(type, minPrice, maxPrice, color, notes);
            client.sendToServer(order);

            showMessage("Your custom design request has been submitted!", true);

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
            App.setRoot("CatalogView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
