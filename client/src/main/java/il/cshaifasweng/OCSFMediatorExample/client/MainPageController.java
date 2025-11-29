package il.cshaifasweng.OCSFMediatorExample.client;

import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.client.Events.SalesListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class MainPageController {

    @FXML
    private Button manageOrdersBtn;
    //@FXML private Button ordersBtn;
    @FXML
    private Button complaintsBtn;
    @FXML
    private Button LogInBtn;
    @FXML
    private Button LogoutBtn;
    @FXML
    private Button reportsBtn;
    @FXML
    private Button manageEmployeesBtn;
    @FXML
    private Button newsletterBtn;
    @FXML
    private Button manageCustomersBtn;
    @FXML
    private Button addSaleBtn;
    @FXML
    private SaleCarouselController carouselController;

    // Flag to track if we explicitly requested categories (via Browse button)
    private boolean categoriesRequested = false;
    // Reference to track if this controller instance is currently active
    private static MainPageController activeInstance = null;
    // Flag to prevent category navigation immediately after clicking Add Sale
    private long lastAddSaleClickTime = 0;
    private static final long NAVIGATION_COOLDOWN_MS = 2000; // 2 seconds


    @FXML
    private void initialize() {
        // CRITICAL: Clear any previous active instance and reset its flags
        if (activeInstance != null && activeInstance != this) {
            // Clear the old instance's request flag
            activeInstance.categoriesRequested = false;
            activeInstance.lastAddSaleClickTime = 0;
            try {
                EventBus.getDefault().unregister(activeInstance);
                System.out.println("MainPageController: Unregistered old active instance");
            } catch (IllegalArgumentException e) {
                // Not registered, that's okay
            }
        }
        
        // Unregister this instance first to avoid duplicates
        try {
            EventBus.getDefault().unregister(this);
        } catch (IllegalArgumentException e) {
            // Not registered, that's okay
        }

        EventBus.getDefault().register(this);
        categoriesRequested = false; // Reset flag when page initializes
        lastAddSaleClickTime = 0; // Reset cooldown
        activeInstance = this; // Mark this instance as active
        
        System.out.println("MainPageController: Initialized, activeInstance set to this, categoriesRequested=" + categoriesRequested);

        // Always refresh promotions whenever this page is shown
        try {
            client.sendToServer("#getSales");
        } catch (IOException e) {
            e.printStackTrace();
        }


        PublicUser user = AppSession.getCurrentUser();
        if (user == null || (user.getRole() != Role.MANAGER && user.getRole() != Role.NETWORK_MANAGER)) {
            manageEmployeesBtn.setVisible(false);
            newsletterBtn.setVisible(false);
            manageCustomersBtn.setVisible(false);
            reportsBtn.setVisible(false);
            addSaleBtn.setVisible(false);
            addSaleBtn.setManaged(false);
            complaintsBtn.setVisible(false);
            manageOrdersBtn.setVisible(false);


        }

        if (user == null) {
            // not logged in
            LogInBtn.setVisible(true);
            LogInBtn.setManaged(true);

            if (LogoutBtn != null) {
                LogoutBtn.setVisible(false);
                LogoutBtn.setManaged(false);
            }
        } else {
            // logged in
            LogInBtn.setVisible(false);
            LogInBtn.setManaged(false);
            complaintsBtn.setVisible(true);

            if (LogoutBtn != null) {
                LogoutBtn.setVisible(true);
                LogoutBtn.setManaged(true);
            }
        }
    }

    @FXML
    private void onLoginClicked(ActionEvent event) {
        switchTo("Login");
    }

    @FXML
    private void onLogoutClicked(ActionEvent event) {
        // Clear session
        AppSession.clear();

        // go back to Login screen
        try {
            App.setRoot("Login");
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load Login view.").showAndWait();
        }
    }


    @FXML
    private void onBrowseClicked(ActionEvent event) {
        // Don't use the event-based approach - handle it directly
        // This prevents unwanted navigation when categories arrive from other sources
        try {
            // Request categories and handle response directly via a one-time listener
            categoriesRequested = true;
            client.sendToServer("getCategories");

            // The response will be handled by onCategoriesReceived, but only if categoriesRequested is true
            // and we're still the active instance
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onOrdersClicked(ActionEvent event) {
        PublicUser user = AppSession.getCurrentUser();
        if (user == null) {
            new Alert(Alert.AlertType.ERROR, "Please log in first.").showAndWait();
            return;
        }
        if (user.getRole() == Role.CUSTOMER) {
            switchTo("MyOrdersView");
        } else {
            switchTo("OrderManagementView");
        }
    }

    @FXML
    private void onComplaintsClicked(ActionEvent event) {

        try {
            PublicUser currentUser = AppSession.getCurrentUser();

            if (currentUser == null) {
                // אם אין משתמש מחובר
                new Alert(Alert.AlertType.WARNING, "Please log in first.").showAndWait();
                return;
            }

            Role userRole = currentUser.getRole();

            if (userRole == Role.CUSTOMER) {
                App.setRoot("ComplaintView");
            } else if (userRole == Role.EMPLOYEE || userRole == Role.MANAGER || userRole == Role.NETWORK_MANAGER) {
                App.setRoot("ComplaintManagementView");
            } else {
                new Alert(Alert.AlertType.ERROR, "Unknown user role: " + userRole).showAndWait();
            }

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load complaints page.").showAndWait();
        }
    }

    @FXML
    private void onReportsClicked(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportView.fxml"));
            Parent root = loader.load();

            Stage reportStage = new Stage();
            reportStage.setTitle("Reports");
            reportStage.setScene(new Scene(root));
            reportStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open Reports window.").showAndWait();
        }
    }


    private void switchTo(String fxml) {
        try {
            App.setRoot(fxml);
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement ste : e.getStackTrace()) {
                sb.append(ste.toString()).append("\n");
            }
            new Alert(Alert.AlertType.ERROR, "Failed to load view: " + fxml + "\n" + e + "\n" + sb).showAndWait();
            e.printStackTrace();
        }
    }


    @FXML
    private void onProfileClicked(ActionEvent event) {
        try {
            App.setRoot("PersonalDetailsView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void onManageEmployeesClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("EmployeeManagementView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onNewsletterClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("NewsletterView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onManageCustomersClicked(ActionEvent actionEvent) {
        try {
            App.setRoot("CustomerManagementView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onSalesArrived(SalesListEvent event) {
        List<Sale> active = event.getSales().stream()
                .filter(s -> s.getStatus() != SaleStatus.Stashed)
                .collect(Collectors.toList());

        carouselController.setSales(active);
    }

    @FXML
    private void onAddSaleClicked(ActionEvent event) {
        // Set cooldown to prevent category navigation
        lastAddSaleClickTime = System.currentTimeMillis();

        // Clear active instance and reset categories flag before navigating away
        if (activeInstance == this) {
            activeInstance = null;
        }
        categoriesRequested = false; // Reset to prevent any delayed category events from navigating
        
        // Unregister from EventBus to prevent processing delayed events
        try {
            EventBus.getDefault().unregister(this);
            System.out.println("MainPageController: Unregistered from EventBus before navigating to SaleCreateView");
        } catch (IllegalArgumentException e) {
            // Not registered, that's okay
        }

        try {
            App.setRoot("SaleCreateView");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onCategoriesReceived(List<?> list) {
        if (list.isEmpty() || !(list.get(0) instanceof Category)) return;

        System.out.println("MainPageController: onCategoriesReceived called - activeInstance=" + activeInstance + ", this=" + this + ", categoriesRequested=" + categoriesRequested);

        // CRITICAL: Only process if this is the active instance
        if (activeInstance != this) {
            System.out.println("MainPageController: Categories received but this is not the active instance (active=" + activeInstance + ", this=" + this + "), ignoring");
            // Also clear the flag on non-active instances to prevent future issues
            categoriesRequested = false;
            return;
        }

        // CRITICAL: Check cooldown - if Add Sale was clicked recently, ignore category events
        long timeSinceAddSaleClick = System.currentTimeMillis() - lastAddSaleClickTime;
        if (timeSinceAddSaleClick < NAVIGATION_COOLDOWN_MS && lastAddSaleClickTime > 0) {
            System.out.println("MainPageController: Categories received but Add Sale was clicked recently (" + timeSinceAddSaleClick + "ms ago), ignoring navigation");
            categoriesRequested = false; // Reset flag
            return;
        }

        // CRITICAL: Only navigate if we explicitly requested categories (via Browse button)
        if (!categoriesRequested) {
            System.out.println("MainPageController: Categories received but not requested, ignoring navigation");
            return;
        }
        
        // CRITICAL: Double-check we're still the active instance (might have changed during async processing)
        if (activeInstance != this) {
            System.out.println("MainPageController: Active instance changed during processing, ignoring navigation");
            categoriesRequested = false;
            return;
        }
        
        // CRITICAL: Final safety check - verify addSaleBtn exists and is not null
        if (addSaleBtn == null) {
            System.out.println("MainPageController: addSaleBtn is null, we're probably not initialized properly, ignoring navigation");
            categoriesRequested = false;
            return;
        }

        // CRITICAL: Verify we're actually on MainPage by checking the scene root
        Platform.runLater(() -> {
            try {
                // CRITICAL: Re-check we're still the active instance (might have changed during async processing)
                if (activeInstance != this) {
                    System.out.println("MainPageController: Active instance changed during Platform.runLater, ignoring navigation");
                    categoriesRequested = false;
                    return;
                }
                
                // Check if we're still on MainPage by verifying the scene root contains our button
                if (App.scene == null || App.scene.getRoot() == null) {
                    System.out.println("MainPageController: Scene or root is null, ignoring navigation");
                    categoriesRequested = false; // Reset flag
                    return;
                }

                // Check if addSaleBtn exists and is in the scene
                if (addSaleBtn == null) {
                    System.out.println("MainPageController: addSaleBtn is null, ignoring navigation");
                    categoriesRequested = false;
                    return;
                }

                // Verify the button is actually in the current scene
                javafx.scene.Node found = App.scene.getRoot().lookup("#addSaleBtn");
                if (found == null) {
                    System.out.println("MainPageController: addSaleBtn not found in scene, we're not on MainPage - ignoring navigation");
                    categoriesRequested = false;
                    return;
                }

                // Final check: make sure the found node is actually our button
                if (found != addSaleBtn && !addSaleBtn.getParent().equals(App.scene.getRoot())) {
                    System.out.println("MainPageController: addSaleBtn not matching scene root, ignoring navigation");
                    categoriesRequested = false;
                    return;
                }

                // All checks passed - we're on MainPage and requested categories
                System.out.println("MainPageController: All checks passed, navigating to BrowseCategoriesView");

                // Reset the flag before navigating
                categoriesRequested = false;

                @SuppressWarnings("unchecked")
                List<Category> categories = (List<Category>) list;

                AppSession.setCategories(categories);

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("BrowseCategoriesView.fxml"));
                    Parent root = loader.load();

                    BrowseCategoriesController controller = loader.getController();
                    controller.loadCategories(categories);

                    App.scene.setRoot(root);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.out.println("MainPageController: Exception in onCategoriesReceived: " + e.getMessage());
                e.printStackTrace();
                categoriesRequested = false; // Reset flag on error
            }
        });

        // Don't process here - let Platform.runLater handle it
        return;
    }

    @FXML
    private void onManageOrdersClicked(ActionEvent e) {
        try {
            App.setRoot("ManageOrdersView");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

