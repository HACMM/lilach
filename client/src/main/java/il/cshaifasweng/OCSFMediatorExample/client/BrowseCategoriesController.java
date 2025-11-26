package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.Category;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class BrowseCategoriesController {

    @FXML
    private VBox categoryList;

    public void loadCategories(List<Category> categories) {

        categoryList.getChildren().clear();

        for (Category cat : categories) {

            Button btn = new Button(cat.getName());
            btn.setPrefWidth(300);
            btn.setStyle("-fx-background-color: #e7b3d1; -fx-text-fill: white; -fx-font-size: 16px; " +
                    "-fx-background-radius: 20; -fx-padding: 10 20;");

            btn.setOnAction(e -> {
                AppSession.setLastSelectedCategory(cat.getCategory_id());
                try {
                    client.sendToServer(new Message("getCatalogByCategory", cat.getCategory_id()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            categoryList.getChildren().add(btn);
        }
    }
}
