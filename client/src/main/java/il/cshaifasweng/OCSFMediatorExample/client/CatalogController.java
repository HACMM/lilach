package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Filter;
import Request.PublicUser;
import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.AddItemEvent;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
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

    @FXML private Label branchLabel;
    @FXML private ComboBox<Branch> branchSelector;
    private final ObservableList<Branch> branchChoices = FXCollections.observableArrayList();

	/** Set up table and ask server for catalog */
	@Override
	public void initialize(URL loc, ResourceBundle res) {
		EventBus.getDefault().register(this);

		nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
		typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType()));
		priceCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPrice()));

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
            requestCatalogForBranch(branchId);
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

        requestCatalogForBranch(selected.getId());
    }


}
