package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Filter;
import Request.PublicUser;
import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.AddItemEvent;
import il.cshaifasweng.OCSFMediatorExample.client.Events.CategoryRefreshEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.util.*;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class CatalogController implements Initializable {
	@FXML
	private TableColumn<Item, Void> actionCol;
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
    private TextField filterField;
	@FXML
	private Button addItemBtn;
	@FXML
	private TableColumn<Item, String> colorCol;

	@FXML

    //@FXML private Button searchBtn;
    private final ObservableList<Item> masterData = FXCollections.observableArrayList(); // כל הקטלוג
    private final ObservableList<Item> filteredData = FXCollections.observableArrayList(); // הנתונים שמוצגים בטבלה
	private int currentCategoryId = -1;

	// ← Back button
	@FXML private Button backBtn;

    @FXML private Label branchLabel;
    @FXML private ComboBox<Branch> branchSelector;
    private final ObservableList<Branch> branchChoices = FXCollections.observableArrayList();

	/** Set up table and ask server for catalog */
	@Override
	public void initialize(URL loc, ResourceBundle res) {
		// Unregister first to avoid duplicate registrations
		try {
			EventBus.getDefault().unregister(this);
		} catch (IllegalArgumentException e) {
			// Not registered, that's okay
		}
		EventBus.getDefault().register(this);
		System.out.println("CatalogController initialized!");

		if (AppSession.isCameFromCategory()) {
			List<Item> items = AppSession.getLastItemList();
			long cacheTimestamp = AppSession.getLastItemListTimestamp();
			long now = System.currentTimeMillis();
			long cacheAge = now - cacheTimestamp;
			
			// Only use cached list if it's recent (within last 5 seconds) and not empty
			// This ensures we don't use stale cached data when navigating back to catalog
			if (items != null && !items.isEmpty() && cacheAge < 5000) {
				// Use cached category-filtered list (it's recent)
				System.out.println("CatalogController: Using recent cached category list (" + items.size() + " items, age: " + cacheAge + "ms)");
				masterData.setAll(items);
				filteredData.setAll(items);
				table.setItems(filteredData);
				// Don't reset the flag here - keep it until we explicitly want to clear it
				// This prevents the catalog from being overwritten when branch selection triggers a refresh
			} else {
				// Cached list is null, empty, or stale - request fresh data
				if (items == null || items.isEmpty()) {
					System.out.println("CatalogController: isCameFromCategory is true but cached list is null/empty, requesting fresh catalog");
				} else {
					System.out.println("CatalogController: Cached category list is stale (age: " + cacheAge + "ms), requesting fresh catalog");
				}
				AppSession.setCameFromCategory(false); // Clear the flag since we don't have valid cached data
				PublicUser currentUser = AppSession.getCurrentUser();
				Integer branchId = (currentUser != null ? currentUser.getBranchId() : null);
				if (branchId != null) {
					requestCatalogForBranch(branchId);
				} else {
					try {
						if (client != null && client.isConnected()) {
							client.sendToServer("getCatalog");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			// Not filtering by category - request fresh catalog from server
			PublicUser currentUser = AppSession.getCurrentUser();
			Integer branchId = (currentUser != null ? currentUser.getBranchId() : null);
			if (branchId != null) {
				requestCatalogForBranch(branchId);
			} else {
				try {
					if (client != null && client.isConnected()) {
						client.sendToServer("getCatalog");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Image placeholder = new Image(
				Objects.requireNonNull(
						getClass().getResource("/images/no_image.jpg")
				).toExternalForm()
		);
		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
		priceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPrice()));
		
		// Custom cell factory for price column to show strikethrough for sale prices
		priceCol.setCellFactory(col -> new TableCell<Item, Double>() {
			@Override
			protected void updateItem(Double originalPrice, boolean empty) {
				super.updateItem(originalPrice, empty);
				if (empty || originalPrice == null) {
					setText(null);
					setGraphic(null);
				} else {
					Item item = getTableView().getItems().get(getIndex());
					SalePriceHelper.SalePriceResult priceResult = SalePriceHelper.calculateSalePrice(item);
					
					if (priceResult.hasActiveSale() && priceResult.getSalePrice() < priceResult.getOriginalPrice()) {
						// Show original price with strikethrough (red) and sale price (green)
						Text originalPriceText = new Text(String.format("%.2f₪", priceResult.getOriginalPrice()));
						originalPriceText.setStyle("-fx-strikethrough: true; -fx-fill: #cc0000; -fx-font-size: 12px;");
						
						Text salePriceText = new Text("  " + String.format("%.2f₪", priceResult.getSalePrice()));
						salePriceText.setStyle("-fx-fill: #228B22; -fx-font-weight: bold; -fx-font-size: 13px;");
						
						TextFlow priceFlow = new TextFlow(originalPriceText, salePriceText);
						setGraphic(priceFlow);
						setText(null);
					} else {
						setText(String.format("%.2f₪", priceResult.getOriginalPrice()));
						setGraphic(null);
					}
				}
			}
		});

		imageCol.setCellValueFactory(d -> {
			ImageView imageView = new ImageView(placeholder);
			imageView.setFitWidth(100);
			imageView.setFitHeight(100);
			imageView.setPreserveRatio(true);

			Item item = d.getValue();
			byte[] imgData = item.getImageData();

			if (imgData != null && imgData.length > 0) {

				// ⭐ 2 — טענת תמונה אמיתית ברקע
				new Thread(() -> {
					Image img = new Image(new ByteArrayInputStream(imgData));

					Platform.runLater(() -> imageView.setImage(img));
				}).start();
			}

			return new SimpleObjectProperty<>(imageView);
		});




		colorCol.setCellValueFactory(d ->
				new SimpleStringProperty(d.getValue().getColor())
		);

		colorCol.setCellFactory(col -> new TableCell<Item, String>() {
			@Override
			protected void updateItem(String colorName, boolean empty) {
				super.updateItem(colorName, empty);

				if (empty || colorName == null || colorName.isEmpty()) {
					setGraphic(null);
					return;
				}

				// עיגול עדין
				javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(8);

				// אם הצבע הוא לבן → נוסיף גבול אפור כדי שיראה
				String fill = colorName.toLowerCase();

				// אם רוצים גוון עדין יותר:
				switch (fill) {
					case "red":
						fill = "#ff8a8a";
						break;
					case "yellow":
						fill = "#fff79a";
						break;
					case "pink":
						fill = "#ffb6d5";
						break;
					case "purple":
						fill = "#c7a7f3";
						break;
					case "white":
						fill = "white";
						break;
					case "green":
					case "deep green":
						fill = "#8fd19e";  // ירוק פסטלי
						break;
					case "green with yellow edges":
						fill = "#cce58b";  // ירוק-צהבהב עדין
						break;
					case "peach":
						fill = "#ffd4b3";
						break;
					case "cream":
						fill = "#fff3d6";
						break;
					case "lavender":
					case "lavender & white":
						fill = "#dcbcfa";
						break;
					case "peach, cream & white":
						fill = "#f6d6c7";
						break;
					case "red & white":
						fill = "#e6a4a4";
						break;
					case "pink & green":
						fill = "#d4c6dd";
						break;
					case "green & white variegated":
						fill = "#c9e3d3";
						break;
					default:
						fill = "#dddddd"; // צבע ברירת מחדל עדין
						break;
				}


				circle.setStyle(
						"-fx-fill: " + fill + ";" +
								"-fx-stroke: #cccccc;" +      // גבול עדין
								"-fx-stroke-width: 1;"
				);

				setGraphic(circle);
			}
		});


		filterField.textProperty().addListener((obs, oldV, newV) -> applyFilters());

		table.setItems(filteredData);

		PublicUser currentUser = AppSession.getCurrentUser();
		if (currentUser != null &&
				(currentUser.getRole() == Role.EMPLOYEE ||
						currentUser.getRole() == Role.MANAGER ||
						currentUser.getRole() == Role.NETWORK_MANAGER)) {

			addItemBtn.setVisible(true);
			addItemBtn.setManaged(true);

		} else {
			addItemBtn.setVisible(false);
			addItemBtn.setManaged(false);
		}

        // Branch selector visibility & behavior
        if (currentUser != null && currentUser.isNetworkUser()) {
            // network / subscription users: can switch branch
            branchSelector.setVisible(true);
            branchSelector.setManaged(true);
            branchLabel.setText("Branch:");

            // request list of all branches from server
            try {
                client.sendToServer(new Message("show branches", null));
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // branch-only account: fixed branch
            branchSelector.setVisible(false);
            branchSelector.setManaged(false);

            Integer branchId = (currentUser != null ? currentUser.getBranchId() : null);
            if (branchId != null) {
                branchLabel.setText("Branch ID: " + branchId);
                // we don’t have Branch object yet here; we’ll fill it once we get BranchListEvent
            } else {
                branchLabel.setText("Branch: (unknown)");
            }
//			if (!AppSession.isCameFromCategory()) {
//				// רק אם לא באנו מסינון לפי קטגוריה – נבקש את כל הקטלוג
//				requestCatalogForBranch(branchId);
//			}
        }

        //  nice rendering of Branch in combo
        branchSelector.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.getName());
            }
        });
        branchSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Branch b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "Select branch" : b.getName());
            }
        });

		SearchCriteria last = AppSession.getLastSearchCriteria();
		if (last != null) {
			applySearchCriteria(last);
		}

		addButtonToTable();

	}

	private void addButtonToTable() {
		actionCol.setCellFactory(col -> new TableCell<Item, Void>() {

			private final Button addBtn = new Button();
			private final Label msgLabel = new Label("");
			private final HBox container = new HBox(6);

			{
				// טוענים אייקון של עגלה
				ImageView icon = new ImageView(
						new Image(Objects.requireNonNull(
								getClass().getResourceAsStream("/images/AddToCart_icon.png")
						))
				);
				icon.setFitWidth(33);
				icon.setFitHeight(33);

				addBtn.setGraphic(icon);
				addBtn.setStyle(
						"-fx-background-color: transparent;" +
								"-fx-cursor: hand;" +
								"-fx-padding: 4;"
				);

				// אפקט יפה בהובר
				addBtn.setOnMouseEntered(e -> addBtn.setStyle(
						"-fx-background-color: rgba(231,179,209,0.35);" +
								"-fx-background-radius: 20;" +
								"-fx-cursor: hand;"
				));
				addBtn.setOnMouseExited(e -> addBtn.setStyle(
						"-fx-background-color: transparent;" +
								"-fx-padding: 4;"
				));

				// הודעה קטנה ליד הכפתור
				msgLabel.setStyle("-fx-text-fill: #a64f73; -fx-font-weight: bold;");
				msgLabel.setVisible(false);

				container.getChildren().addAll(addBtn, msgLabel);

				addBtn.setOnAction(e -> {
					Item item = getTableView().getItems().get(getIndex());
					CartService.get().addOne(item);

					msgLabel.setText("✓ Added!");
					msgLabel.setVisible(true);

					new Thread(() -> {
						try {
							Thread.sleep(1500);
							Platform.runLater(() -> msgLabel.setVisible(false));
						} catch (InterruptedException ignored) {}
					}).start();
				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					setGraphic(container);
				}
			}
		});
	}




	private void applySearchCriteria(SearchCriteria criteria) {
		List<Item> filtered = masterData.stream()
				.filter(item -> {
//					boolean matchType = (criteria.getType() == null || criteria.getType().isEmpty()
//							|| item.getType().equalsIgnoreCase(criteria.getType()));

					boolean matchColor = (criteria.getColor() == null || criteria.getColor().isEmpty()
							|| item.getColor().equalsIgnoreCase(criteria.getColor()));

					boolean matchPrice = true;
					double min = criteria.getMinPrice();
					double max = criteria.getMaxPrice() == 0 ? Double.MAX_VALUE : criteria.getMaxPrice();
					double price = item.getPrice();
					if (price < min || price > max) {
						matchPrice = false;
					}

					return matchColor && matchPrice;
				})
				.collect(Collectors.toList());

		filteredData.setAll(filtered);
		table.refresh();
	}

	/** Ask server for the full catalog */
    private void requestCatalogForBranch(Integer branchId) {
        try {
            if (client != null && client.isConnected()) {
                client.sendToServer(new Message("getCatalogForBranch", branchId));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


	/** Receive catalog and populate table */

	@Subscribe
	public void onCatalogReceived(List<?> list) {
		// CRITICAL: Only process if we're actually on the CatalogView
		// Check if we're on CatalogView by verifying the scene root
		if (App.scene == null || App.scene.getRoot() == null) {
			System.out.println("CatalogController: Scene or root is null, ignoring catalog update");
			return;
		}
		
		// Check if we're on CatalogView by looking for a unique element (like the table)
		// But be more lenient - if table exists, we're probably on CatalogView
		javafx.scene.Node found = App.scene.getRoot().lookup("#table");
		if (found == null) {
			System.out.println("CatalogController: Not on CatalogView (table not found), ignoring catalog update");
			return;
		}
		// If found node is not our table instance, that's okay - it might be a different instance
		// Just check that we have a table reference
		if (table == null) {
			System.out.println("CatalogController: Table reference is null, ignoring catalog update");
			return;
		}
		
		// If we came from category selection, we should still update the catalog when items are added
		// The cameFromCategory flag should only prevent automatic refreshes, not manual item additions
		// So we'll update masterData but preserve the category filter if active
		// EventBus uses raw types for List, so verify the payload is actually a list of Item
		if (list == null || list.isEmpty()) {
			System.out.println("CatalogController: Received empty or null list, requesting catalog again");
			Platform.runLater(() -> {
				PublicUser currentUser = AppSession.getCurrentUser();
				if (currentUser != null && currentUser.isNetworkUser()) {
					// For network users, we need to wait for branch selection
					// Just log the issue - the user should select a branch
					System.out.println("CatalogController: Network user - catalog will load when branch is selected");
				} else {
					// For branch users, request catalog for their branch
					Integer branchId = currentUser != null ? currentUser.getBranchId() : null;
					if (branchId != null) {
						requestCatalogForBranch(branchId);
					} else {
						// Fallback: try generic getCatalog
						try {
							if (client != null && client.isConnected()) {
								client.sendToServer("getCatalog");
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
			return;
		}
		
		if (!(list.get(0) instanceof Item)) {
			return; // ignore non-catalog lists (e.g., orders list)
		}
		@SuppressWarnings("unchecked")
		List<Item> items = (List<Item>) list;

		Platform.runLater(() -> {
			System.out.println("CatalogController: Received " + items.size() + " items, updating catalog");
			masterData.setAll(items);

			// Check if this is a full catalog (many items, like 20+) vs category-filtered (few items, like 6)
			// Full catalogs typically have 27+ items, category-filtered have 6 or fewer
			boolean isFullCatalog = items.size() >= 20;
			
			if (AppSession.isCameFromCategory() && isFullCatalog) {
				// This is a full catalog refresh (e.g., after adding an item)
				// Clear the category flag and show all items so new items appear
				System.out.println("CatalogController: Full catalog received (" + items.size() + " items), clearing category filter to show all items including new ones");
				AppSession.setCameFromCategory(false);
				AppSession.setLastSelectedCategory(null);
				AppSession.setLastItemList(null); // Clear cached category-filtered list
				// Apply normal filters
				SearchCriteria last = AppSession.getLastSearchCriteria();
				if (last != null) {
					applySearchCriteria(last);
				} else {
					applyFilters();
				}
			} else if (AppSession.isCameFromCategory() && !isFullCatalog) {
				// This is a category-filtered response, just update with the received items
				System.out.println("CatalogController: Category-filtered catalog received (" + items.size() + " items), updating filtered view");
				filteredData.setAll(items);
				table.refresh();
			} else {
				// Not filtering by category, apply normal filters
				SearchCriteria last = AppSession.getLastSearchCriteria();
				if (last != null) {
					applySearchCriteria(last);
				} else {
					applyFilters();
				}
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
			App.setRoot("BrowseCategoriesView");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}


	private void applyFilters() {
	Filter f = new Filter();
	f.setSearchText(filterField.getText().trim());

	List<Item> filtered = masterData.stream()
			.filter(f::filter)
			.collect(Collectors.toList());

	filteredData.setAll(filtered);

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

            System.out.println("CatalogController: Item saved callback - refreshing full catalog from server for item ID: " + item.getId());
            // Always refresh full catalog after adding an item, because:
            // 1. New items aren't associated with categories yet (ItemCategory relationships aren't created)
            // 2. We need to show the new item immediately, which requires the full catalog
            // 3. Clear category filter so the new item is visible
            AppSession.setCameFromCategory(false);
            AppSession.setLastItemList(null);
            PublicUser currentUser = AppSession.getCurrentUser();
            Integer branchId = (currentUser != null ? currentUser.getBranchId() : null);
            if (branchId != null) {
                requestCatalogForBranch(branchId);
            } else {
                try {
                    if (client != null && client.isConnected()) {
                        client.sendToServer("getCatalog");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Stage s = new Stage();
        s.setTitle("add item");
        s.setScene(new Scene(root));
        s.show();
    }

	public void onCartClicked(ActionEvent actionEvent) {
		PublicUser currentUser = AppSession.getCurrentUser();

		try {
			if (currentUser == null) {
				// המשתמש לא מחובר → מעבר לדף התחברות
				App.setRoot("Login");
			} else {
				// המשתמש מחובר → מעבר לעגלת הקניות
				App.setRoot("CartView");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void onAddToCart(Item item) {
		System.out.println("Added to cart → " + item.getName());
		CartService.get().addOne(item);
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

    @Subscribe
    public void onAddItemEvent(AddItemEvent event) {
        if (event == null || event.getItem() == null || event.getItemId() <= 0) {
            return;
        }
        
        Platform.runLater(() -> {
            // CRITICAL: Check if we're on CatalogView before processing
            if (App.scene == null || App.scene.getRoot() == null) {
                System.out.println("CatalogController: onAddItemEvent - Scene or root is null, ignoring");
                return;
            }
            
            javafx.scene.Node found = App.scene.getRoot().lookup("#table");
            if (found == null || found != table) {
                System.out.println("CatalogController: onAddItemEvent - Not on CatalogView, ignoring");
                return;
            }
            
            // Force a catalog refresh from the server to get the latest items
            // Always refresh full catalog after adding an item, because:
            // 1. New items aren't associated with categories yet (ItemCategory relationships aren't created)
            // 2. We need to show the new item immediately, which requires the full catalog
            // 3. Clear category filter so the new item is visible
            System.out.println("CatalogController: onAddItemEvent - Item added, refreshing full catalog from server");
            AppSession.setCameFromCategory(false);
            AppSession.setLastItemList(null);
            PublicUser currentUser = AppSession.getCurrentUser();
            Integer branchId = (currentUser != null ? currentUser.getBranchId() : null);
            if (branchId != null) {
                requestCatalogForBranch(branchId);
            } else {
                try {
                    if (client != null && client.isConnected()) {
                        client.sendToServer("getCatalog");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Subscribe
    public void onBranchListReceived(BranchListEvent ev) {
        if (ev == null || ev.getBranches() == null || ev.getBranches().isEmpty()) {
            System.out.println("CatalogController: received empty branch list");
            return;
        }

        java.util.List<Branch> branches = ev.getBranches();
        System.out.println("CatalogController: received " + branches.size() + " branches");

        Platform.runLater(() -> {
            branchChoices.setAll(branches);
            branchSelector.setItems(branchChoices);

            Branch toSelect = null;

            // 1) Prefer branch from AppSession if already set
            Branch sessionBranch = AppSession.getCurrentBranch();
            if (sessionBranch != null) {
                for (Branch b : branches) {
                    if (b.getId() == sessionBranch.getId()) {
                        toSelect = b;
                        break;
                    }
                }
            }

            // 2) If no session branch, fall back to user's fixed branch (if any)
            if (toSelect == null) {
                PublicUser currentUser = AppSession.getCurrentUser();
                if (currentUser != null && currentUser.getBranchId() != null) {
                    int userBranchId = currentUser.getBranchId();
                    for (Branch b : branches) {
                        if (b.getId() == userBranchId) {
                            toSelect = b;
                            break;
                        }
                    }
                }
            }

            // 3) Last resort: first branch in list
            if (toSelect == null && !branches.isEmpty()) {
                toSelect = branches.get(0);
            }

            if (toSelect != null) {
                branchSelector.getSelectionModel().select(toSelect);
                branchLabel.setText("Branch: " + toSelect.getName());
                //  persist selection for next time
                AppSession.setCurrentBranch(toSelect);
                CartService.get().switchBranch(toSelect.getId());
                
                // Only request catalog if we didn't come from category selection
                // This prevents overwriting category-filtered items
                if (!AppSession.isCameFromCategory()) {
                    requestCatalogForBranch(toSelect.getId());
                } else {
                    System.out.println("CatalogController: Preserving category-filtered items - not requesting full catalog");
                }
            }
        });
    }

    @FXML
    private void onBranchSelectorChanged(ActionEvent event) {
        Branch selected = branchSelector.getValue();
        if (selected == null) return;

        CartService.get().switchBranch(selected.getId());
        AppSession.setCurrentBranch(selected);
        branchLabel.setText("Branch: " + selected.getName());
        System.out.println("Catalog: switched to branch " + selected.getName()
                + " (id=" + selected.getId() + ")");

        // If we came from category selection, don't request full catalog - preserve the filtered items
        if (!AppSession.isCameFromCategory()) {
            requestCatalogForBranch(selected.getId());
        } else {
            System.out.println("Catalog: Preserving category-filtered items after branch change");
        }
    }

	@Subscribe
	public void onRefreshCategory(CategoryRefreshEvent ev) {
		List<Item> updatedItems = ev.getItems();

		Platform.runLater(() -> {
			System.out.println("CatalogController: Refreshing category with "
					+ updatedItems.size() + " items");

			masterData.setAll(updatedItems);
			filteredData.setAll(updatedItems);
			table.refresh();
		});
	}


}
