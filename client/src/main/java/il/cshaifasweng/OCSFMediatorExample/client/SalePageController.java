package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.SalesListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.SaleStatus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
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

    private void loadSales(List<Sale> sales) {
        Platform.runLater(() -> {
            saleContainer.getChildren().clear();

            for (Sale sale : sales) {
                try {
                    FXMLLoader loader =
                            new FXMLLoader(getClass().getResource("SaleCard.fxml"));
                    Parent card = loader.load();

                    SaleCardController controller = loader.getController();
                    controller.setSale(sale);

                    saleContainer.getChildren().add(card);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Subscribe
    public void onSalesListEvent(SalesListEvent event) {
        System.out.println("SalePageController: received SalesListEvent with "
                + (event.getSales() == null ? "null" : event.getSales().size()) + " sales");

        List<Sale> activeSales = event.getSales().stream()
                .filter(s -> s.getStatus() != SaleStatus.Stashed)
                .collect(Collectors.toList());

        System.out.println("SalePageController: activeSales size = " + activeSales.size());


        loadSales(activeSales);
    }

    @Subscribe
    public void onEndSaleOk(Message msg) {
        if ("endSaleOk".equals(msg.getType())) {
            Integer saleId = (Integer) msg.getData();

            Platform.runLater(() -> {
                new Alert(Alert.AlertType.INFORMATION,
                        "Sale " + saleId + " ended successfully.").showAndWait();
            });

            // Optionally re-fetch sales (if for some reason server-side listAll didn't reach )
            try {
                client.sendToServer("#getSales");
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if ("endSaleError".equals(msg.getType())) {
            String error = (String) msg.getData();
            Platform.runLater(() -> {
                new Alert(Alert.AlertType.ERROR,
                        "Failed to end sale: " + error).showAndWait();
            });
        }
    }

    @Subscribe
    public void onEndSaleError(Message msg) {
        if (!"endSaleError".equals(msg.getType())) {
            return;
        }
        Platform.runLater(() -> {
            String text = (msg.getData() instanceof String)
                    ? (String) msg.getData()
                    : "Failed to end sale.";
            new Alert(Alert.AlertType.ERROR, text).showAndWait();
        });
    }


    @FXML
    private void onBack(ActionEvent event) {
        EventBus.getDefault().unregister(this);
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
