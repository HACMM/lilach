package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.entities.Category;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class BrowseCategoriesController {

    @FXML
    private VBox categoryList;

    @FXML
    public void initialize() {
        // חייבים להירשם ל־EventBus!
        EventBus.getDefault().register(this);

        // מעלה את הקטגוריות מה־AppSession
        List<Category> categories = AppSession.getCategories();
        if (categories != null) {
            loadCategories(categories);
        }
    }

    public void loadCategories(List<Category> categories) {

        categoryList.getChildren().clear();

        for (Category cat : categories) {

            // כרטיס (VBox)
            VBox card = new VBox();
            card.setSpacing(10);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(300);
            card.setPrefHeight(80);

            // עיצוב התחלתי
            card.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-background-radius: 18;" +
                            "-fx-border-radius: 18;" +
                            "-fx-border-color: #eab6ce;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-padding: 18;"
            );

            // כותרת הקטגוריה
            Label lbl = new Label(cat.getName());
            lbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #b36384;");

            // מוסיפים לכרטיס
            card.getChildren().add(lbl);

            // Hover effect
            card.setOnMouseEntered(e -> card.setStyle(
                    "-fx-background-color: #ffe7f2;" +
                            "-fx-background-radius: 18;" +
                            "-fx-border-radius: 18;" +
                            "-fx-border-color: #d48aab;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-padding: 18;"
            ));

            card.setOnMouseExited(e -> card.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-background-radius: 18;" +
                            "-fx-border-radius: 18;" +
                            "-fx-border-color: #eab6ce;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-padding: 18;"
            ));

            // לחיצה על הקטגוריה
            card.setOnMouseClicked(e -> {
                AppSession.setLastSelectedCategory(cat.getCategory_id());
                AppSession.setCameFromCategory(true); // חובה!!

                try {
                    client.sendToServer(new Message("getCatalogByCategory", cat.getCategory_id()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // מוסיפים ל־VBox שב־FXML
            categoryList.getChildren().add(card);
        }
    }

    @FXML
    public void onBack(ActionEvent actionEvent) {
        try {
            // Unregister from EventBus before navigating away
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this);
            }
            App.setRoot("MainPage");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onCategoryCatalogReceived(List<?> list) {
        if (list.isEmpty() || !(list.get(0) instanceof Item)) return;
        
        // CRITICAL: Only process if we're actually on BrowseCategoriesView
        if (App.scene == null || App.scene.getRoot() == null) {
            System.out.println("BrowseCategoriesController: Scene or root is null, ignoring item list");
            return;
        }
        
        // Check if we're on BrowseCategoriesView by looking for the categoryList VBox
        javafx.scene.Node found = App.scene.getRoot().lookup("#categoryList");
        if (found == null || found != categoryList) {
            System.out.println("BrowseCategoriesController: Not on BrowseCategoriesView (categoryList not found or doesn't match), ignoring item list");
            return;
        }
        
        EventBus.getDefault().unregister(this);
        List<Item> items = (List<Item>) list;

        System.out.println("BrowseCategoriesController: received " + items.size() + " items for category");

        AppSession.setLastItemList(items);
        AppSession.setCameFromCategory(true); // << דגל חדש!
        try {
            App.setRoot("CatalogView");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
