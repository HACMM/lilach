package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;

public class SearchByController {

    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> colorCombo;
    @FXML private Slider minPriceSlider;
    @FXML private Slider maxPriceSlider;
    @FXML private Label minValueLbl;
    @FXML private Label maxValueLbl;

    private Consumer<SearchCriteria> onFilterApplied;

    public void setOnFilterApplied(Consumer<SearchCriteria> callback) {
        this.onFilterApplied = callback;
    }

    @FXML
    public void initialize() {
        // סוגי פרחים וצבעים
        typeCombo.getItems().addAll("Bouquet", "Single Flower", "Arrangement", "Box", "Bridal Bouquet");
        colorCombo.getItems().addAll("Red", "Pink", "White", "Yellow", "Purple");

        // האזנה לשינוי ערכים בלייב
        minPriceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                minValueLbl.setText(String.format("%.0f₪", newVal.doubleValue())));

        maxPriceSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                maxValueLbl.setText(String.format("%.0f₪", newVal.doubleValue())));
    }

    @FXML
    private void onApplyFilterClicked() {
        double min = minPriceSlider.getValue();
        double max = maxPriceSlider.getValue();

        // לוודא שהמינימום לא גדול מהמקסימום
        if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        SearchCriteria criteria = new SearchCriteria(
                typeCombo.getValue(),
                colorCombo.getValue(),
                min,
                max
        );

        AppSession.setLastSearchCriteria(criteria);

        try {
            App.setRoot("CatalogView");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void onBackClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("CatalogView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
