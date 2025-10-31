package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Filter;
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
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> colorFilter;
    @FXML
    private ComboBox<String> priceFilter;
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

		categoryFilter.setItems(FXCollections.observableArrayList("All", "Bouquet", "Single Flower", "Plant", "Accessory"));
		categoryFilter.setValue("All");
		categoryFilter.setOnAction(e -> applyFilters());


		filterField.textProperty().addListener((obs, oldV, newV) -> applyFilters());

		table.setItems(filteredData);

		UserAccount currentUser = AppSession.getCurrentUser();
		if (currentUser == null || (currentUser.getRole() != Role.EMPLOYEE && currentUser.getRole() != Role.MANAGER)) {
			addItemBtn.setVisible(false);
		}
		requestCatalog();
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
	public void onCatalogReceived(List<Item> items) {
		Platform.runLater(() -> {
			masterData.setAll(items);
			applyFilters();
		});
	}


	/** Filter by name/type when Search clicked */
	@FXML
	private void onSearchClicked() {
		String term = filterField.getText().toLowerCase();
		List<Item> filtered = table.getItems().stream()
				.filter(i ->
						i.getName().toLowerCase().contains(term) ||
								i.getType().toLowerCase().contains(term)
				)
				.collect(Collectors.toList());
		table.getItems().setAll(filtered);
		applyFilters();
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
							"/il/cshaifasweng/OCSFMediatorExample/client/LoginView.fxml"
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
		try {
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
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(
					"/il/cshaifasweng/OCSFMediatorExample/client/PersonalDetailsView.fxml"));
			Stage stage = new Stage();
			stage.setScene(new Scene(loader.load()));
			stage.setTitle("Personal Details");
			stage.show();
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
}
