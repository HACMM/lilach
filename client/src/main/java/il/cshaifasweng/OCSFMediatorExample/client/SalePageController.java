package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.SalesListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.SaleStatus;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SalePageController {

    @FXML private VBox saleContainer;

    public void initialize() {
        EventBus.getDefault().register(this);
        try {
            client.sendToServer("#getSales");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadSales(List<Sale> sales) {
        saleContainer.getChildren().clear();

        for (Sale sale : sales) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("SaleCard.fxml"));
                VBox card = loader.load();

                SaleCardController controller = loader.getController();
                controller.setSale(sale);

                saleContainer.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Subscribe
    public void onSalesListEvent(SalesListEvent event) {
        List<Sale> activeSales = event.getSales().stream()
                .filter(s -> s.getStatus() != SaleStatus.Stashed)
                .collect(Collectors.toList());

        loadSales(activeSales);
    }


    @FXML
    private void onBack(ActionEvent event) {
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
