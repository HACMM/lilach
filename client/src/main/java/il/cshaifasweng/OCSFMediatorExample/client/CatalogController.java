package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class CatalogController implements Initializable {
	@FXML private TableView<Item> table;
	@FXML private TableColumn<Item, String> nameCol;
	@FXML private TableColumn<Item, String> typeCol;
	@FXML private TableColumn<Item, Double> priceCol;
	@FXML private TableColumn<Item, ImageView> imageCol;
	@FXML private TextField filterField;
	@FXML private Button searchBtn;

	// ← Back button
	@FXML private Button backBtn;

	/** Set up table and ask server for catalog */
	@Override
	public void initialize(URL loc, ResourceBundle res) {
		EventBus.getDefault().register(this);

		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
		priceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPrice()));
		imageCol.setCellValueFactory(d -> {
			String path = "/images/" + d.getValue().getImageLink();
			Image img;
			try {
				img = new Image(getClass().getResourceAsStream(path));
			} catch (Exception ex) {
				img = null;
			}
			ImageView iv = new ImageView(img);
			iv.setFitWidth(100);
			iv.setFitHeight(100);
			iv.setPreserveRatio(true);
			return new SimpleObjectProperty<>(iv);
		});

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
	@org.greenrobot.eventbus.Subscribe
	public void onCatalogReceived(List<Item> items) {
		Platform.runLater(() -> table.getItems().setAll(items));
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
}
