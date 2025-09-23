package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.DiscountType;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import il.cshaifasweng.OCSFMediatorExample.entities.ItemSale;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;


import java.io.IOException;

public class ItemController {
    @FXML
    private TextArea descriptionTextArea;
    @FXML private Label nameLabel, typeLabel, priceLabel;
    private Item item;
    @FXML
    private Button addToCartBUTTON;

    @FXML
    void addToCart(ActionEvent event) {
        //TODO: implement when the cart page is ready
    }

    public void updatePriceLabel(double newPrice) {
        priceLabel.setText("Price: " + item.getPrice() + "$");
    }

    public void init(Item item) {
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
}