package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.text.SimpleDateFormat;

import static il.cshaifasweng.OCSFMediatorExample.entities.DiscountType.FlatDiscount;
import static il.cshaifasweng.OCSFMediatorExample.entities.DiscountType.PercentDiscount;

public class SaleCardController {

    @FXML private ImageView saleImage;
    @FXML private Label saleName;
    @FXML private Label saleDesc;
    @FXML private Label saleDates;
    @FXML private Label saleDiscount;
    @FXML private Button endSaleButton;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private Sale sale;

    public void setSale(Sale sale) {
        this.sale = sale;
        saleName.setText(sale.getName());
        saleDesc.setText(sale.getDescription());
        saleDates.setText(sdf.format(sale.getStartDate()) + " - " + sdf.format(sale.getEndDate()));

        String discountText = "";
        if (sale.getDiscountType() != null) {
            switch (sale.getDiscountType()) {
                case PercentDiscount:
                    discountText = sale.getDiscountValue() + "% off";
                    break;
                case FlatDiscount:
                    discountText = sale.getDiscountValue() + "₪ off";
                    break;
            }
        }
        saleDiscount.setText(discountText);
        saleImage.setImage(loadSaleImage(sale.getImageLink()));

        // show button only for managers / network managers
        var user = AppSession.getCurrentUser();
        boolean isManager =
                user != null &&
                        (user.getRole() == Role.MANAGER || user.getRole() == Role.NETWORK_MANAGER);

        endSaleButton.setVisible(isManager);
        endSaleButton.setManaged(isManager);
    }

    @FXML
    private void onEndSale() {
        if (sale == null) return;

        try {
            // 1) Tell server to end this sale
            SimpleClient.client.sendToServer("#endSale " + sale.getId());

            // 2) Immediately re-fetch the list of sales
            SimpleClient.client.sendToServer("#getSales");

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Failed to end sale. Please try again.").showAndWait();
        }
    }

    private Image loadSaleImage(String link) {
        String defaultPath = "/images/sale_image.jpg";

        try {
            if (link == null || link.isBlank()) {
                var url = getClass().getResource(defaultPath);
                return (url != null) ? new Image(url.toExternalForm()) : null;
            }

            link = link.trim();

            if (link.startsWith("http://") || link.startsWith("https://") || link.startsWith("file:")) {
                return new Image(link, true);
            }

            var url = getClass().getResource(link);
            if (url == null) {
                url = getClass().getResource("/" + link);
            }
            if (url != null) {
                return new Image(url.toExternalForm());
            }

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

