package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SaleCarouselController {

    @FXML private ImageView carouselImage;
    @FXML private Label carouselTitle;
    @FXML private Button viewSaleButton;

    private List<Sale> sales = new ArrayList<>();
    private int currentIndex = 0;

    public void setSales(List<Sale> saleList) {
        this.sales = saleList;

        if (!sales.isEmpty()) {
            showSale(0);
            startAutoSwitch();
        }
    }

    private void showSale(int index) {
        Sale sale = sales.get(index);

        // אפקט מעבר יפה
        FadeTransition fade = new FadeTransition(Duration.millis(600), carouselImage);
        fade.setFromValue(0);
        fade.setToValue(1);

        carouselTitle.setText(sale.getName());

        if (sale.getImageLink() != null) {
            carouselImage.setImage(new Image(sale.getImageLink()));
        }

        fade.play();
    }

    private void startAutoSwitch() {
        PauseTransition pause = new PauseTransition(Duration.seconds(5));
        pause.setOnFinished(e -> {
            currentIndex = (currentIndex + 1) % sales.size();
            showSale(currentIndex);
            startAutoSwitch();
        });
        pause.play();
    }

    @FXML
    void onViewSale() {
        Sale selected = sales.get(currentIndex);
        AppSession.setSelectedSale(selected);// אם תרצי

        try {
            App.setRoot("SalePage");
        } catch (IOException e) {
            e.printStackTrace();
        }            // לעבור לדף המבצעים
    }
}
