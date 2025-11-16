package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Filter;
import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.client.Events.AddItemEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.Objects;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class CatalogController implements Initializable {
    @FXML
    private TableView<Item> table;
    @FXML
    private TableColumn<Item, String> nameCol;
    @FXML
    private TableColumn<Item, String> typeCol;
    @FXML
    private TableColumn<Item, Double> priceCol;
    @FXML
    private TableColumn<Item, ImageView> imageCol;
//    @FXML
//    private ComboBox<String> categoryFilter;
//    @FXML
//    private ComboBox<String> colorFilter;
//    @FXML
//    private ComboBox<String> priceFilter;
    @FXML
    private TextField filterField;
	@FXML
	private Button addItemBtn;

	@FXML

    //@FXML private Button searchBtn;
    private final ObservableList<Item> masterData = FXCollections.observableArrayList(); // כל הקטלוג
    private final ObservableList<Item> filteredData = FXCollections.observableArrayList(); // הנתונים שמוצגים בטבלה

	// ← Back button
	@FXML private Button backBtn;

	/** Set up table and ask server for catalog */
	@Override
	public void initialize(URL loc, ResourceBundle res) {
		EventBus.getDefault().register(this);

		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
		priceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPrice()));
//		imageCol.setCellValueFactory(d -> {
//			String path = "/images/" + d.getValue().getImageLink();
//			Image img;
//			try {
//				img = new Image(getClass().getResourceAsStream(path));
//			} catch (Exception ex) {
//				img = null;
//			}
//			ImageView iv = new ImageView(img);
//			iv.setFitWidth(100);
//			iv.setFitHeight(100);
//			iv.setPreserveRatio(true);
//			return new SimpleObjectProperty<>(iv);

		imageCol.setCellValueFactory(d -> {
			byte[] imgData = d.getValue().getImageData();
			ImageView imageView;

			if (imgData != null && imgData.length > 0) {
				Image img = new Image(new ByteArrayInputStream(imgData));
				imageView = new ImageView(img);
				imageView.setFitWidth(100);
				imageView.setFitHeight(100);
				imageView.setPreserveRatio(true);
			} else {
				imageView = new ImageView(new Image(getClass().getResourceAsStream("/images/no_image.jpg")));
				imageView.setFitWidth(100);
				imageView.setFitHeight(100);
				imageView.setPreserveRatio(true);
			}

			return new SimpleObjectProperty<>(imageView);

	});

//		categoryFilter.setItems(FXCollections.observableArrayList("All", "Bouquet", "Single Flower", "Plant", "Accessory"));
//		categoryFilter.setValue("All");
//		categoryFilter.setOnAction(e -> applyFilters());


		filterField.textProperty().addListener((obs, oldV, newV) -> applyFilters());

		table.setItems(filteredData);

		PublicUser currentUser = AppSession.getCurrentUser();
		if (currentUser == null || (currentUser.getRole() != Role.EMPLOYEE && currentUser.getRole() != Role.MANAGER || currentUser.getRole() != Role.NETWORK_MANAGER)) {

			addItemBtn.setVisible(false);
		}
		requestCatalog();

		SearchCriteria last = AppSession.getLastSearchCriteria();
		if (last != null) {
			applySearchCriteria(last);
		}
	}

	private void applySearchCriteria(SearchCriteria criteria) {
		List<Item> filtered = masterData.stream()
				.filter(item -> {
					boolean matchType = (criteria.getType() == null || criteria.getType().isEmpty()
							|| item.getType().equalsIgnoreCase(criteria.getType()));

					boolean matchColor = (criteria.getColor() == null || criteria.getColor().isEmpty()
							|| item.getColor().equalsIgnoreCase(criteria.getColor()));

					boolean matchPrice = true;
					double min = criteria.getMinPrice();
					double max = criteria.getMaxPrice() == 0 ? Double.MAX_VALUE : criteria.getMaxPrice();
					double price = item.getPrice();
					if (price < min || price > max) {
						matchPrice = false;
					}

					return matchType && matchColor && matchPrice;
				})
				.collect(Collectors.toList());

		filteredData.setAll(filtered);
		table.refresh();
	}

	/** Ask server for the full catalog */
	private void requestCatalog() {
		try {
			if (client != null && client.isConnected()) {
				client.sendToServer("getCatalog");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Receive catalog and populate table */
//	@Subscribe
//	public void onCatalogReceived(List<Item> items) {
//
//		Platform.runLater(() -> table.getItems().setAll(items));
//	}
	@Subscribe
	public void onCatalogReceived(List<?> list) {
		// EventBus uses raw types for List, so verify the payload is actually a list of Item
		if (list == null || list.isEmpty() || !(list.get(0) instanceof Item)) {
			return; // ignore non-catalog lists (e.g., orders list)
		}
		@SuppressWarnings("unchecked")
		List<Item> items = (List<Item>) list;

		Platform.runLater(() -> {
			masterData.setAll(items);

			SearchCriteria last = AppSession.getLastSearchCriteria();
			if (last != null) {
				applySearchCriteria(last);
			} else {
				applyFilters();
			}
		});
	}


    @FXML
    private void onSearchByClicked() throws IOException {
        App.setRoot("SearchByView");
    }

    @FXML
    private void onCustomOrderClicked() throws IOException {
        App.setRoot("CustomOrderView");

    }


	/** Open detail view on double‐click */
	@FXML
	private void onItemClicked(MouseEvent e) {
		if (e.getClickCount() < 2) return;
		Item sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) return;
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource(
							"/il/cshaifasweng/OCSFMediatorExample/client/ItemView.fxml"
					)
			);
			Parent root = loader.load();
			ItemController ctrl = loader.getController();
			ctrl.init(sel);
			Stage st = new Stage();
			st.setScene(new Scene(root));
			st.show();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/** Show login popup */
	@FXML
	private void LogIn(javafx.event.ActionEvent evt) {
		try {
			FXMLLoader loader = new FXMLLoader(
					getClass().getResource(
							"/il/cshaifasweng/OCSFMediatorExample/client/Login.fxml"
					)
			);
			Parent root = loader.load();
			Stage st = new Stage();
			st.setTitle("Login");
			st.setScene(new Scene(root));
			st.show();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/** Show complaint window */
	@FXML
	private void openComplaint() {
		PublicUser currentUser = AppSession.getCurrentUser();

		try {
			if (currentUser != null &&
					(currentUser.getRole() == Role.EMPLOYEE || currentUser.getRole() == Role.MANAGER || currentUser.getRole() == Role.NETWORK_MANAGER)) {
				// 🔹 עבור עובד או מנהל – לעמוד ניהול תלונות
				App.setRoot("ComplaintManagementView");
			} else {
				// 🔹 עבור לקוח – חלון הגשת תלונה רגיל
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource(
								"/il/cshaifasweng/OCSFMediatorExample/client/ComplaintView.fxml"
						)
				);
				Parent root = loader.load();
				Stage popup = new Stage();
				popup.setTitle("Submit Complaint");
				popup.setScene(new Scene(root));
				popup.show();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/** ← Back to the main dashboard */
	@FXML
	private void onBackClicked() {
		try {
			App.setRoot("MainPage");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}


	private void applyFilters() {
	Filter f = new Filter();
	f.setSearchText(filterField.getText().trim());
	//f.setCategory(categoryFilter.getValue());

	// f.setFlowerType(flowerTypeCombo.getValue());
	//f.setColor(colorFilter.getValue());
	//f.setMinPrice(minPriceField.getValue());
	//f.setMaxPrice(maxPriceField.getValue());

	List<Item> filtered = masterData.stream()
			.filter(f::filter)
			.collect(Collectors.toList());

	filteredData.setAll(filtered);



//	String search = filterField.getText().toLowerCase().trim();
//	String category = categoryFilter.getValue();
//
//	List<Item> filtered = masterData.stream()
//			.filter(i -> (search.isEmpty() || i.getName().toLowerCase().contains(search) || i.getType().toLowerCase().contains(search)))
//			.filter(i -> ("All".equals(category) || i.getType().contains(category)))
//			.collect(Collectors.toList());
//
//	filteredData.setAll(filtered);
}

    @FXML
    private void onProfileClicked(ActionEvent event) {
        if (AppSession.getCurrentUser() == null) {
            // route to login screen in the same window
            try {
                App.setRoot("Login");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

		try {
			App.setRoot("PersonalDetailsView");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @FXML
    public void onAddItemClicked(ActionEvent actionEvent) throws IOException {
		FXMLLoader loader = new FXMLLoader(
				getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/AddNewItemView.fxml")
		);
		Parent root = loader.load();
		AddItemController ctrl = loader.getController();

        // נגדיר מה קורה כשיש אישור מהשרת:
        ctrl.setOnSaved(item -> {
            if (item == null) return;
            // אם getId מחזיר int → אין בדיקת null:
            if (item.getId() <= 0) return;

            boolean exists = masterData.stream()
                    .anyMatch(i -> Objects.equals(i.getId(), item.getId()));
            if (!exists) {
                masterData.add(item);
                if (table != null) table.refresh();
            }
        });

        Stage s = new Stage();
        s.setTitle("add item");
        s.setScene(new Scene(root));
        s.show();
    }

	public void onCartClicked(ActionEvent actionEvent) {
		try {
			App.setRoot("CartView");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void onClearFiltersClicked(ActionEvent actionEvent) {
		// ננקה את הקריטריונים האחרונים
		AppSession.setLastSearchCriteria(null);

		// ננקה את שדה החיפוש
		filterField.clear();

		// נציג את כל הנתונים מחדש
		filteredData.setAll(masterData);
		table.refresh();
	}
}
