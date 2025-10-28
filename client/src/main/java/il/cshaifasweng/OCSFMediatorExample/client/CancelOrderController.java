package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.EmailSender;
import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import il.cshaifasweng.OCSFMediatorExample.entities.CancelOrderEvent;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class CancelOrderController {

    @FXML private Button cancelBtn;
    @FXML private TextField orderIdField;
    @FXML private TextField emailField;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
        messageLabel.setText("");
        messageLabel.setVisible(false);
    }

    @FXML
    private void onCancelOrder() {
        String orderNumber = orderIdField.getText().trim();
        String customerEmail = emailField.getText().trim();

        if (orderNumber.isEmpty() || customerEmail.isEmpty()) {
            showMessage("Please fill in all fields.", false);
            return;
        }

        if (!orderNumber.matches("\\d+")) {
            showMessage("Order number must contain only digits.", false);
            return;
        }

        CancelOrderEvent cancelEvent = new CancelOrderEvent(orderNumber, customerEmail);
        try {
            client.sendToServer(cancelEvent);
        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Failed to contact server.", false);
        }
    }

    @Subscribe
    public void handleCancelOrderResponse(CancelOrderEvent event) {
        Platform.runLater(() -> {
            Order order = event.getOrder();

            if (order == null) {
                showMessage("❌ Order not found or email mismatch.", false);
                return;
            }

            if (event.getStatus().equalsIgnoreCase("Already cancelled")) {
                showMessage("⚠️ This order was already cancelled.", false);
                return;
            }

            if (event.getStatus().startsWith("Order found")) {
                double refundAmount = calculateRefund(order);
                String subject = "Your refund from FlowerShop 🌸";
                String body = String.format("""
                        Hello %s,
                        
                        We've received your cancellation request for order #%d.
                        Your refund amount is: %.2f₪
                        
                        Thank you for shopping with us 💐
                        FlowerShop Team
                        """, order.getUserAccount().getName(), order.getId(), refundAmount);

                // שולח מייל ללקוחה
                EmailSender.sendEmail(subject, body, order.getUserAccount().getEmail());

                // מציג הודעת הצלחה יפה
                showSuccessPopup("💌 Confirmation sent by email");
            } else {
                showMessage(event.getStatus(), false);
            }
        });
    }

    private double calculateRefund(Order order) {
        LocalDateTime delivery = order.getDeliveryDateTime();
        LocalDateTime now = LocalDateTime.now();
        Duration diff = Duration.between(now, delivery);
        long hours = diff.toHours();

        double total = order.getTotalPrice();

        if (hours >= 3) return total;         // החזר מלא
        else if (hours >= 1) return total * 0.5; // החזר 50%
        else return 0;                         // אין החזר
    }

    private void showMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.setStyle(success
                ? "-fx-text-fill: green; -fx-font-weight: bold;"
                : "-fx-text-fill: #a64f73; -fx-font-weight: bold;");
        messageLabel.setVisible(true);
    }

    private void showSuccessPopup(String text) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);

        Label label = new Label(text);
        label.setStyle("""
            -fx-background-color: #f8b6c6;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-padding: 10 25 10 25;
            -fx-background-radius: 20;
        """);

        StackPane root = new StackPane(label);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(null);
        popup.setScene(scene);
        popup.setAlwaysOnTop(true);

        // מיקום באמצע המסך
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        popup.setX(bounds.getMinX() + bounds.getWidth() / 2 - 100);
        popup.setY(bounds.getMinY() + bounds.getHeight() / 2 - 50);

        popup.show();

        // fade-out נחמד
        FadeTransition fade = new FadeTransition(javafx.util.Duration.seconds(2), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(javafx.util.Duration.seconds(1));
        fade.setOnFinished(ev -> popup.close());
        fade.play();
    }
}
