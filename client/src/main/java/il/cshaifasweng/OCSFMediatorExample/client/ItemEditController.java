package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ItemEditController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField typeField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField colorField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ImageView previewImage;
    @FXML
    private Label imageNameLabel;
    @FXML
    private Label errorLabel;
    
    private Item item;
    private ItemController parentController;
    private File selectedImageFile;

    public void setParentController(ItemController parentController) {
        this.parentController = parentController;
    }

    public void init(Item item) {
        this.item = item;
        
        // Populate fields with current item data
        nameField.setText(item.getName() != null ? item.getName() : "");
        typeField.setText(item.getType() != null ? item.getType() : "");
        priceField.setText(String.valueOf(item.getPrice()));
        colorField.setText(item.getColor() != null ? item.getColor() : "");
        descriptionArea.setText(item.getDescription() != null ? item.getDescription() : "");
        
        // Load current image if available
        if (item.getImageData() != null && item.getImageData().length > 0) {
            Image currentImage = new Image(new java.io.ByteArrayInputStream(item.getImageData()));
            previewImage.setImage(currentImage);
            imageNameLabel.setText(item.getImagePath() != null ? item.getImagePath() : "Current image");
        } else {
            imageNameLabel.setText("No image");
        }
        
        errorLabel.setText("");
    }

    @FXML
    private void handleUploadImage() {
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

    @FXML
    private void onSave() {
        errorLabel.setText("");
        
        try {
            if (item == null) {
                errorLabel.setText("Error: Item is null!");
                return;
            }

            // Validate required fields
            String name = nameField.getText().trim();
            String type = typeField.getText().trim();
            String priceStr = priceField.getText().trim();
            String description = descriptionArea.getText().trim();

            if (name.isEmpty() || type.isEmpty() || priceStr.isEmpty() || description.isEmpty()) {
                errorLabel.setText("Please fill all required fields (Name, Type, Price, Description).");
                return;
            }

            double newPrice;
            try {
                newPrice = Double.parseDouble(priceStr);
                if (newPrice < 0) {
                    errorLabel.setText("Price must be a positive number.");
                    return;
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Price must be a valid number (e.g., 12.5).");
                return;
            }

            // Update item fields
            item.setName(name);
            item.setType(type);
            item.setPrice(newPrice);
            item.setColor(colorField.getText().trim());
            item.setDescription(description);

            // Handle image upload if a new image was selected
            if (selectedImageFile != null) {
                try {
                    File serverDir = new File("images");
                    if (!serverDir.exists()) serverDir.mkdirs();

                    File destination = new File(serverDir, selectedImageFile.getName());
                    Files.copy(selectedImageFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    item.setImagePath("/images/" + selectedImageFile.getName());
                } catch (IOException e) {
                    System.err.println("Failed to copy image: " + e.getMessage());
                    errorLabel.setText("Failed to upload image. Item will be saved without image update.");
                }
            }

            System.out.println("[CLIENT] Sending edited item: " + item.getName() + ", ID: " + item.getId());
            
            // Send to server - the server should handle EditItem message
            client.sendToServer(item);

            // Update parent controller if available
            if (parentController != null) {
                parentController.refreshItem(item);
            }

            ((Stage) nameField.getScene().getWindow()).close();
        } catch (Exception e) {
            errorLabel.setText("Failed to save item: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}
