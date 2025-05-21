package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.example.model.Item;
import org.example.network.CommunicationHandler;

import java.io.IOException;
import java.util.List;

public class CatalogController {
	@FXML
	private TableView<Item> table;
	@FXML
	private TableColumn<Item, String> nameCol;
	@FXML
	private TableColumn<Item, String> typeCol;
	@FXML
	private TableColumn<Item, Double> priceCol;
	@FXML
	private TextField filterField;

	private final CommunicationHandler comm = new CommunicationHandler();

	@FXML
	public void initialize() {
		getClient("", 3000).sendToServer("Catalog Initialized");
		EventBus.getDefault().register(this);
		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
		priceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPrice()));
		loadCatalog();
	}

	@Subscribe
	public void onCatalogReceived(List<Item> items) {
		Platform.runLater(() -> {
			if (filterField.getText() == null || filterField.getText().isEmpty()) {
				table.getItems().setAll(items);
			} else {
				String filter = filterField.getText().toLowerCase();
				table.getItems().setAll(
						items.stream()
								.filter(item -> item.getName().toLowerCase().contains(filter)
										|| item.getType().toLowerCase().contains(filter))
								.toList()
				);
			}
		});
	}

	@FXML
	private void onRefreshClicked() {
		loadCatalog();
	}

	private void loadCatalog() {
		if (client != null && client.isConnected()) {
			client.sendToServer("getCatalog");
		}
	}

	@FXML
	private void onItemClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			Item sel = table.getSelectionModel().getSelectedItem();
			if (sel == null) return;
			try {
				FXMLLoader loader = new FXMLLoader(
						getClass().getResource("/org/example/ItemView.fxml")
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
	}
}
