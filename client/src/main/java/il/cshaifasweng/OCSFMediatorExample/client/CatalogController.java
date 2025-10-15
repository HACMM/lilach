package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item; // for cart conversion
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CatalogController (catalog stays on client.CatalogItem).
 * Converts CatalogItem -> entities.Item only when adding to cart.
 * No images are used.
 */
public class CatalogController {

	// top bar
	@FXML private Button backBtn;
	@FXML private Button cartTopBtn;
	@FXML private Button addItemBtn;   // owner only
	@FXML private TextField filterField;

	// table (NOTE: CatalogItem, not entities.Item)
	@FXML private TableView<CatalogItem> table;
	@FXML private TableColumn<CatalogItem, String> nameCol;
	@FXML private TableColumn<CatalogItem, String> typeCol;
	@FXML private TableColumn<CatalogItem, Double> priceCol;
	@FXML private TableColumn<CatalogItem, String> imageCol; // kept to satisfy FXML, but we don't render images

	private final ObservableList<CatalogItem> allItems = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		// show owner button (if you toggle this at login)
		if (addItemBtn != null) addItemBtn.setVisible(CurrentUser.isOwner());

		// basic columns
		nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
		typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
		priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

		// we don't show images; keep the column but display empty strings
		if (imageCol != null) {
			imageCol.setCellValueFactory(new PropertyValueFactory<>("imageUrl")); // will be null/empty; no rendering
		}

		// Add-to-Cart per row (converts CatalogItem -> entities.Item)
		TableColumn<CatalogItem, Void> actionCol = new TableColumn<>("");
		actionCol.setPrefWidth(140);
		actionCol.setCellFactory(col -> new TableCell<>() {
			private final Button addBtn = new Button("Add to Cart");
			{
				addBtn.getStyleClass().add("button");
				addBtn.setOnAction(e -> {
					CatalogItem ci = getTableView().getItems().get(getIndex());
					Item cartItem = toEntityItem(ci);
					CartService.get().addOne(cartItem);
				});
			}
			@Override
			protected void updateItem(Void v, boolean empty) {
				super.updateItem(v, empty);
				setGraphic(empty ? null : addBtn);
			}
		});
		table.getColumns().add(actionCol);

		// attach list (empty until server fills it)
		table.setItems(allItems);
	}

	/** Call this from whoever loads data from the server. */
	public void setCatalog(List<CatalogItem> items) {
		allItems.setAll(items);
		table.setItems(allItems);
	}

	// ---------- actions ----------
	@FXML
	private void onBackClicked(ActionEvent e) {
		Stage s = (Stage) backBtn.getScene().getWindow();
		if (s != null) s.close();
	}

	@FXML
	private void onSearchClicked(ActionEvent e) {
		String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
		if (q.isBlank()) { table.setItems(allItems); return; }
		var filtered = allItems.stream()
				.filter(it -> (it.getName() != null && it.getName().toLowerCase().contains(q)) ||
						(it.getType() != null && it.getType().toLowerCase().contains(q)))
				.collect(Collectors.toList());
		table.setItems(FXCollections.observableArrayList(filtered));
	}

	@FXML
	private void onOpenCart(ActionEvent e) {
		try {
			FXMLLoader fxml = new FXMLLoader(getClass().getResource("CartView.fxml"));
			Parent root = fxml.load();
			Stage s = new Stage();
			s.setTitle("Your Cart");
			s.setScene(new Scene(root));
			s.show();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML private void onItemClicked() { /* keep if you use double-click */ }

	@FXML
	private void onAddItemClicked(ActionEvent e) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("AddItemPage.fxml"));
			Parent root = loader.load();
			AddItemController ctrl = loader.getController();
			// when owner saves, push to table immediately
			ctrl.setOnSaved(newItem -> {
				allItems.add(newItem);
				table.refresh();
			});
			Stage stage = new Stage();
			stage.setTitle("Add New Catalog Item");
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	// ---------- conversion helper ----------
	private static Item toEntityItem(CatalogItem ci) {
		Item it = new Item();
		it.setName(ci.getName());
		it.setType(ci.getType());
		it.setPrice(ci.getPrice());
		// no images
		return it;
	}
}


//
//import il.cshaifasweng.OCSFMediatorExample.entities.Item;   // <<— USE ENTITIES.ITEM
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.event.ActionEvent;
//import javafx.fxml.FXML;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.control.TableCell;
//import javafx.scene.control.TableColumn;
//import javafx.scene.control.TableView;
//import javafx.scene.control.TextField;
//import javafx.scene.control.cell.PropertyValueFactory;
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.stage.Stage;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * Catalog controller wired to ENTITIES.Item and CartService.
// * - Adds an “Add to Cart” button per row (calls CartService.addOne(Item))
// * - Owner-only “Add New Item” button (visibility toggled by CurrentUser.isOwner())
// * - Search works on name/type
// */
//public class CatalogController {
//
//	/* ----- top bar controls ----- */
//	@FXML private Button backBtn;
//	@FXML private Button cartTopBtn;
//	@FXML private Button addItemBtn;      // owner only
//	@FXML private TextField filterField;
//
//	/* ----- table ----- */
//	@FXML private TableView<Item> table;
//	@FXML private TableColumn<Item, String> nameCol;
//	@FXML private TableColumn<Item, String> typeCol;
//	@FXML private TableColumn<Item, Double> priceCol;
//	@FXML private TableColumn<Item, String> imageCol;
//
//	private final ObservableList<Item> allItems = FXCollections.observableArrayList();
//
//	@FXML
//	public void initialize() {
//		// owner toggle
//		if (addItemBtn != null) {
//			addItemBtn.setVisible(CurrentUser.isOwner());
//		}
//
//		// basic columns
//		nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
//		typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
//		priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
//
//		// image column (thumbnail)
//		imageCol.setCellFactory(col -> new TableCell<>() {
//			private final ImageView view = new ImageView();
//			{
//				view.setFitWidth(110);
//				view.setFitHeight(85);
//				view.setPreserveRatio(true);
//			}
//			@Override
//			protected void updateItem(String url, boolean empty) {
//				super.updateItem(url, empty);
//				if (empty || url == null || url.isBlank()) {
//					setGraphic(null);
//				} else {
//					try { view.setImage(new Image(url, true)); setGraphic(view); }
//					catch (Exception ex) { setGraphic(null); }
//				}
//			}
//		});
//		imageCol.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
//
//		// Add-to-Cart button per row — IMPORTANT: type is ENTITIES.Item
//		TableColumn<Item, Void> actionCol = new TableColumn<>("");
//		actionCol.setPrefWidth(140);
//		actionCol.setCellFactory(col -> new TableCell<>() {
//			private final Button addBtn = new Button("Add to Cart");
//			{
//				addBtn.getStyleClass().add("button");
//				addBtn.setOnAction(e -> {
//					Item item = getTableView().getItems().get(getIndex());
//					CartService.get().addOne(item);     // <<— EXACT MATCH
//				});
//			}
//			@Override
//			protected void updateItem(Void v, boolean empty) {
//				super.updateItem(v, empty);
//				setGraphic(empty ? null : addBtn);
//			}
//		});
//		table.getColumns().add(actionCol);
//
//		// load data
//		loadCatalog();
//	}
//
//	/** Replace this with your OCSF fetch. Leaving empty list runs fine & compiles. */
//	private void loadCatalog() {
//		// TODO: fetch List<Item> from server and set into allItems
//		// Example if you already have items: allItems.setAll(fetchedList);
//		table.setItems(allItems);
//	}
//
//	/* ================== top-bar actions (names kept as you had) ================== */
//
//	@FXML
//	private void onBackClicked(ActionEvent e) {
//		// use your navigation; this close is a safe fallback
//		Stage s = (Stage) backBtn.getScene().getWindow();
//		if (s != null) s.close();
//	}
//
//	@FXML
//	private void onSearchClicked(ActionEvent e) {
//		String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
//		if (q.isBlank()) {
//			table.setItems(allItems);
//			return;
//		}
//		List<Item> filtered = allItems.stream()
//				.filter(it -> (it.getName() != null && it.getName().toLowerCase().contains(q))
//						|| (it.getType() != null && it.getType().toLowerCase().contains(q)))
//				.collect(Collectors.toList());
//		table.setItems(FXCollections.observableArrayList(filtered));
//	}
//
//	@FXML
//	private void onOpenCart(ActionEvent e) {
//		try {
//			FXMLLoader fxml = new FXMLLoader(getClass().getResource("CartView.fxml"));
//			Parent root = fxml.load();
//			Stage s = new Stage();
//			s.setTitle("Your Cart");
//			s.setScene(new Scene(root));
//			s.show();
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
//	}
//
//	/** Keep if you still use double-click to open item details. */
//	@FXML
//	private void onItemClicked() { /* no-op */ }
//
//	/** Owner adds new item window (callback refresh is optional). */
//	@FXML
//	private void onAddItemClicked(ActionEvent e) {
//		try {
//			FXMLLoader loader = new FXMLLoader(getClass().getResource("AddItemPage.fxml"));
//			Parent root = loader.load();
//			// If your AddItemController returns ENTITIES.Item in a callback, you can refresh list here.
//			Stage stage = new Stage();
//			stage.setTitle("Add New Catalog Item");
//			stage.setScene(new Scene(root));
//			stage.show();
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
//	}
//}
