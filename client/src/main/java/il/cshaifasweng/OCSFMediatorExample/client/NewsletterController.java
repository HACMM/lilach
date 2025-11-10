package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class NewsletterController {

    @FXML private TextField subjectField;
    @FXML private TextArea messageArea;
    @FXML private Label statusLabel;

    @FXML
    private void onSendClicked(ActionEvent event) {
        String subject = subjectField.getText().trim();
        String body = messageArea.getText().trim();

        if (subject.isEmpty() || body.isEmpty()) {
            statusLabel.setText("⚠️ Please fill in both subject and message.");
            statusLabel.setVisible(true);
            return;
        }

        try {
            client.sendToServer(new Message("newsletterSend", new String[]{subject, body}));
            statusLabel.setText("✅ Newsletter sent to all subscribers!");
            statusLabel.setVisible(true);
            subjectField.clear();
            messageArea.clear();
        } catch (IOException e) {
            statusLabel.setText("❌ Failed to send newsletter.");
            statusLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    @FXML
    private void onBackClicked(ActionEvent event) {
        try {
            App.setRoot("ManagementPage"); // או MainPage, בהתאם לארגון שלך
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
