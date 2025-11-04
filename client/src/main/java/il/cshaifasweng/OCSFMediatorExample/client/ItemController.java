package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;


import java.io.ByteArrayInputStream;
import java.io.IOException;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ItemController {
    @FXML
    private TextArea descriptionTextArea;
    @FXML private Label nameLabel, typeLabel, priceLabel;
    @FXML private ImageView itemImage;

    private Item item;
    @FXML
    private Button addToCartBUTTON;
    @FXML
    private Button EditPriceBTN;
    @FXML
    private Button RemoveBTN;
    @FXML
    private ImageView addToCartImage;

    @FXML
    private void initialize() {
        addToCartImage.setImage(new Image(getClass().getResourceAsStream("/images/cart_icon.jpg")));
    }


    @FXML
    void addToCart(ActionEvent event) {
        if (item == null) return;

        CartService.get().addOne(item);

        Alert ok = new Alert(Alert.AlertType.INFORMATION,
                item.getName() + " added to your cart!");
        ok.setHeaderText("Added to Cart");
        ok.showAndWait();
    }

    public void updatePriceLabel(double newPrice) {
        priceLabel.setText("Price: " + item.getPrice() + "$");
    }

    public void init(Item item) {
        PublicUser currentUser = AppSession.getCurrentUser();

        if (currentUser != null) {
            Role role = currentUser.getRole();

            // just employee and manager see the button
            if (role == Role.EMPLOYEE || role == Role.MANAGER) {
                EditPriceBTN.setVisible(true);
                RemoveBTN.setVisible(true);
            } else {
                EditPriceBTN.setVisible(false);
                RemoveBTN.setVisible(false);
            }
        } else {
            EditPriceBTN.setVisible(false);
            RemoveBTN.setVisible(false);
    }
        this.item = item;
        nameLabel.setText(item.getName());
        typeLabel.setText(item.getType());


        double finalPrice = item.getPrice();
        for (ItemSale sale : item.getSales()) {
            double discount = sale.getDiscount();
            double discountedPrice = finalPrice;

            if (sale.getDiscountType() == DiscountType.PercentDiscount) {
                discountedPrice = finalPrice * (1 - discount / 100);
            } else if (sale.getDiscountType() == DiscountType.FlatDiscount) {
                discountedPrice = finalPrice - discount;
            }

            if (discountedPrice < finalPrice) {
                finalPrice = discountedPrice;
            }
        }
        priceLabel.setText(finalPrice + "$");

//        double discount = item.getSales();
//        priceLabel.setText(String.valueOf(item.getPrice())+"$");
        descriptionTextArea.setText(item.getDescription());
        if (item.getImageData() != null && item.getImageData().length > 0) {
            Image img = new Image(new ByteArrayInputStream(item.getImageData()));
            itemImage.setImage(img);
        } else {
            itemImage.setImage(new Image(getClass().getResourceAsStream("/images/no_image.jpg")));
        }
    }

    @FXML
    private void onEditPrice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/ItemEditView.fxml")
            );
            Parent root = loader.load();
            ItemEditController c = loader.getController();
            c.init(item);
            c.setParentController(this);
            Stage st = new Stage();
            st.setScene(new Scene(root));
            st.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onRemoveItem(ActionEvent event) {
        if (item == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to remove " + item.getName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Removal");
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            try {
                client.sendToServer(new Message("removeItem", item));
                Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Item removed successfully!");
                ok.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to contact server.").showAndWait();
            }
        }
    }

}