package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.ComboBox;
import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.imageio.ImageIO;

public class AddItemController {

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField priceField;
    @FXML private TextField colorField;
    @FXML private Label     errorLabel;
    @FXML private TextArea DescriptionArea;
    @FXML private ComboBox<Branch> branchComboBox;
    @FXML private Label imageNameLabel;
    @FXML private ImageView previewImage;

    private File selectedImageFile; // הקובץ שנבחר
    private List<Branch> branches;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);
        try {
            client.sendToServer("#getAllBranches");
        } catch (Exception e) {
            errorLabel.setText("Failed to load branches.");
        }
    }

    public static byte[] imageToByteArray(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        PixelReader pixelReader = image.getPixelReader();

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixelReader.getArgb(x, y);
                bufferedImage.setRGB(x, y, argb);
            }
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @FXML
    private void handleUploadImage(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Item Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) nameField.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            Image image = new Image(selectedImageFile.toURI().toString());
            previewImage.setImage(image);
            imageNameLabel.setText(selectedImageFile.getName());
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
            String color = colorField.getText().trim();
            String description = DescriptionArea.getText().trim();
            Branch branch = branchComboBox.getSelectionModel().getSelectedItem();

            if (name.isEmpty() || type.isEmpty() || priceStr.isEmpty() || description.isEmpty() || branch == null) {
                errorLabel.setText("Please fill name, type, price, color and description.");
                return;
            }
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                errorLabel.setText("Price must be positive.");
                errorLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            byte[] imagebytes = null;
            if (previewImage.getImage() != null) {
                imagebytes = imageToByteArray(previewImage.getImage());
            }
            Item newItem = new Item(name, type, description, price, imagebytes, color);

            // TODO: Make AddItemMessage
            client.sendToServer(new Message("AddItem",newItem));

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

    @Subscribe
    public void onBranchListReceived(BranchListEvent event) {
            Platform.runLater(() -> {
                branchComboBox.getItems().setAll(event.getBranches());
            });
    }

//        branches = event.getBranches();
//        Platform.runLater(() -> {
//            branchComboBox.getItems().clear();
//            for (Branch branch : branches) {
//                branchComboBox.getItems().add(branch);
//            }
//        });

}

