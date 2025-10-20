package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class PaymentMethodController {

    @FXML private ComboBox<PaymentMethod> savedCardsCombo;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardHolderField;
    @FXML private TextField expiryDateField;
    @FXML private PasswordField cvvField;
    @FXML private Label errorLabel;
    @FXML private CheckBox rememberCardCheckBox;

    private PaymentMethod paymentMethod;

    @FXML
    public void initialize() {
        savedCardsCombo.setDisable(true);
        savedCardsCombo.setOnAction(e -> onCardSelected());
    }


    public void loadSavedCards(List<PaymentMethod> savedCards) {
        if (savedCards != null && !savedCards.isEmpty()) {
            savedCardsCombo.getItems().setAll(savedCards);
            savedCardsCombo.setDisable(false);

            savedCardsCombo.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(PaymentMethod card, boolean empty) {
                    super.updateItem(card, empty);
                    if (empty || card == null) {
                        setText(null);
                    } else {
                        String last4 = card.getCardNumber()
                                .substring(card.getCardNumber().length() - 4);
                        setText("**** **** **** " + last4 + " (" + card.getCardHolderName() + ")");
                    }
                }
            });

            savedCardsCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(PaymentMethod card, boolean empty) {
                    super.updateItem(card, empty);
                    if (empty || card == null) {
                        setText(null);
                    } else {
                        String last4 = card.getCardNumber()
                                .substring(card.getCardNumber().length() - 4);
                        setText("**** **** **** " + last4 + " (" + card.getCardHolderName() + ")");
                    }
                }
            });
        }
    }


    private void onCardSelected() {
        PaymentMethod selected = savedCardsCombo.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cardNumberField.clear();
            cardHolderField.clear();
            expiryDateField.clear();
            cvvField.clear();
            paymentMethod = selected;
        }
    }


    @FXML
    private void onSave() {
        errorLabel.setText("");


        if (savedCardsCombo.getValue() != null) {
            paymentMethod = savedCardsCombo.getValue();
            close();
            return;
        }

        String num = cardNumberField.getText().trim();
        String holder = cardHolderField.getText().trim();
        String expiry = expiryDateField.getText().trim();
        String cvv = cvvField.getText().trim();

        if (num.isEmpty() || holder.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            errorLabel.setText("Please fill all fields.");
            return;
        }
        if (!num.matches("\\d{16}")) {
            errorLabel.setText("Card number must be 16 digits.");
            return;
        }
        if (!cvv.matches("\\d{3}")) {
            errorLabel.setText("CVV must be 3 digits.");
            return;
        }
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            errorLabel.setText("Expiry date must be in MM/YY format.");
            return;
        }

        paymentMethod = new PaymentMethod(num, holder, expiry, cvv);
        close();
    }


    @FXML
    private void onCancel() {
        paymentMethod = null;
        close();
    }

    private void close() {
        Stage st = (Stage) cardNumberField.getScene().getWindow();
        st.close();
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public boolean shouldRememberCard() {
        return rememberCardCheckBox != null && rememberCardCheckBox.isSelected();
    }
}
