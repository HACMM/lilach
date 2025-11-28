package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
    private PauseTransition autoSwitchTimer;

    public void setSales(List<Sale> saleList) {
        Platform.runLater(() -> {
            // stop old timer if exists
            if (autoSwitchTimer != null) {
                autoSwitchTimer.stop();
                autoSwitchTimer = null;
            }

            sales.clear();
            if (saleList != null) {
                sales.addAll(saleList);
            }
            currentIndex = 0;

            if (!sales.isEmpty()) {
                showSale(0);
                startAutoSwitch();
            } else {
                carouselTitle.setText("No promotions currently");
                carouselImage.setImage(loadSaleImage(null));
            }
        });
    }

    private void showSale(int index) {
        if (sales.isEmpty() || index < 0 || index >= sales.size()) {
            return; // safety guard
        }

        Sale sale = sales.get(index);

        FadeTransition fade = new FadeTransition(Duration.millis(600), carouselImage);
        fade.setFromValue(0);
        fade.setToValue(1);

        carouselTitle.setText(sale.getName());

        Image img = loadSaleImage(sale.getImageLink());
        carouselImage.setImage(img);
        fade.play();
    }


    private void startAutoSwitch() {
        if (sales.isEmpty()) return;

        autoSwitchTimer = new PauseTransition(Duration.seconds(5));
        autoSwitchTimer.setOnFinished(e -> {
            // list might have changed while we waited
            if (sales.isEmpty()) {
                return;
            }

            currentIndex = (currentIndex + 1) % sales.size();
            showSale(currentIndex);

            // restart timer
            startAutoSwitch();
        });
        autoSwitchTimer.play();
    }

    @FXML
    void onViewSale() {
        if (sales == null || sales.isEmpty()) {
            // No sales loaded – show a friendly message
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "There are currently no promotions to view.");
            alert.showAndWait();
            return;
        }

        Sale selected = sales.get(currentIndex);
        AppSession.setSelectedSale(selected);// אם תרצי

        try {
            App.setRoot("SalePage");
        } catch (IOException e) {
            e.printStackTrace();
        }            // לעבור לדף המבצעים
    }

    /** Load sale image safely, with fallback to the flower background. */
    private Image loadSaleImage(String link) {
        String defaultPath = "/images/flower_background.png";

        try {
            // 1) No link -> just default flower
            if (link == null || link.isBlank()) {
                var url = getClass().getResource(defaultPath);
                return (url != null) ? new Image(url.toExternalForm()) : null;
            }

            link = link.trim();

            // 2) HTTP/HTTPS or file URLs
            if (link.startsWith("http://") || link.startsWith("https://") || link.startsWith("file:")) {
                return new Image(link, true);
            }

            // 3) Try as classpath resource (e.g. "/images/daisy.png" or "images/daisy.png")
            var url = getClass().getResource(link);
            if (url == null) {
                url = getClass().getResource("/" + link);
            }
            if (url != null) {
                return new Image(url.toExternalForm());
            }

            // 4) Fallback to default
            url = getClass().getResource(defaultPath);
            return (url != null) ? new Image(url.toExternalForm()) : null;

        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                var url = getClass().getResource(defaultPath);
                return (url != null) ? new Image(url.toExternalForm()) : null;
            } catch (Exception ignore) {
                ignore.printStackTrace();
                return null;
            }
        }
    }


}
