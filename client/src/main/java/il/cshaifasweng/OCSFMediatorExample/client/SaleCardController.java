package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SaleCardController {

    @FXML private ImageView saleImage;
    @FXML private Label saleName;
    @FXML private Label saleDesc;
    @FXML private Label saleDates;

    public void setSale(Sale sale) {
        saleName.setText(sale.getName());
        saleDesc.setText(sale.getDescription());

        saleDates.setText("From: " + sale.getStartDate() +
                "  To: " + sale.getEndDate());

        if (sale.getImageLink() != null) {
            saleImage.setImage(new Image(sale.getImageLink()));
        }
    }
}
