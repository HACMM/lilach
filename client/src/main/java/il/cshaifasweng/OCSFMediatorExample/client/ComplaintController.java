package il.cshaifasweng.OCSFMediatorExample.client;

import Request.NewComplaint;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import il.cshaifasweng.OCSFMediatorExample.entities.EmailSender;
import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.User;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
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
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ComplaintController {

    @FXML private ComboBox<Branch> BranchCombo;
    @FXML private TextField EmailTextField;
    @FXML private TextField NameTextField;
    @FXML private TextField OrderNumberTextField;
    @FXML private TextArea descriptionArea;
    @FXML private Button sendButton;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
        try {
            System.out.println("ComplaintController: Requesting branches from server...");
            client.sendToServer("#getAllBranches");
        } catch (IOException e) {
            System.err.println("❌ Failed to request branch list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    @FXML
    private void onSendComplaint() {
        String desc = descriptionArea.getText().trim();
        String name = NameTextField.getText().trim();
        String order = OrderNumberTextField.getText().trim();
        String email = EmailTextField.getText().trim();
        Branch branch = BranchCombo.getSelectionModel().getSelectedItem();

        if (desc.isEmpty() || branch == null || name.isEmpty() || order.isEmpty() || email.isEmpty()) {
            showErrorPopup("Please fill all fields ❗");
            return;
        }

        if (!isValidEmail(email)) {
            showErrorPopup("Please enter a valid email address 📧");
            return;
        }

        try {
            sendButton.setDisable(true);
            client.sendToServer(new NewComplaint(branch.getId(),order,name,email,desc));
            // Success/error handling is done in onComplaintResponse
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPopup("Failed to send complaint to server ⚠️");
            sendButton.setDisable(false);
        }
    }

    @FXML
    private void onBack(ActionEvent event) {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
            showErrorPopup("Could not navigate back ❌");
        }
    }

    @Subscribe
    public void onBranchListReceived(BranchListEvent event) {
        System.out.println("ComplaintController: Received branch list with " + event.getBranches().size() + " branches");
        Platform.runLater(() -> {
            BranchCombo.getItems().setAll(event.getBranches());
            System.out.println("ComplaintController: Populated combobox with " + BranchCombo.getItems().size() + " branches");
        });
    }

    @Subscribe
    public void onComplaintResponse(Message msg) {
        Platform.runLater(() -> {
            if (msg.getType().equals("newComplaintOk")) {
                showSuccessPopup("💌 Complaint submitted successfully!");
                // Clear form fields
                BranchCombo.getSelectionModel().clearSelection();
                NameTextField.clear();
                OrderNumberTextField.clear();
                EmailTextField.clear();
                descriptionArea.clear();
                sendButton.setDisable(false);
            } else if (msg.getType().equals("newComplaintError")) {
                showErrorPopup("Failed to submit complaint: " + msg.getData());
                sendButton.setDisable(false);
            }
        });
    }

    private void showSuccessPopup(String text) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);

        Label label = new Label(text);
        label.setStyle("""
            -fx-background-color: #f5a7b8;
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

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        popup.setX(bounds.getMinX() + bounds.getWidth() / 2 - 100);
        popup.setY(bounds.getMinY() + bounds.getHeight() / 2 - 50);

        popup.show();

        FadeTransition fade = new FadeTransition(javafx.util.Duration.seconds(2), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(javafx.util.Duration.seconds(1));
        fade.setOnFinished(ev -> popup.close());
        fade.play();
    }


    private void showErrorPopup(String text) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);

        Label label = new Label(text);
        label.setStyle("""
            -fx-background-color: #f7b0c1;
            -fx-text-fill: white;
            -fx-font-size: 15px;
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

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        popup.setX(bounds.getMinX() + bounds.getWidth() / 2 - 100);
        popup.setY(bounds.getMinY() + bounds.getHeight() / 2 - 50);

        popup.show();

        FadeTransition fade = new FadeTransition(javafx.util.Duration.seconds(2), root);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(javafx.util.Duration.seconds(1));
        fade.setOnFinished(ev -> popup.close());
        fade.play();
    }
}
