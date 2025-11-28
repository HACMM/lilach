package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.PublicUser;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ReportsController {

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private StackPane chartContainer;
    @FXML private Label infoLabel;
    @FXML private Button generateBtn;

    @FXML private BarChart<String, Number> mainChart;
    private LineChart<String, Number> revenueLineChart;  // For revenue reports
    @FXML private TableView<Object> ordersTable;  // Changed to Object to support both OrderRow and ComplaintRow
    @FXML private TableColumn<Object, String> orderIdCol;
    @FXML private TableColumn<Object, String> orderDateCol;
    @FXML private TableColumn<Object, String> orderCustomerCol;
    @FXML private TableColumn<Object, Object> orderItemsCol;  // Changed to Object to support both Integer and String
    @FXML private TableColumn<Object, Object> orderTotalCol;  // Changed to Object to support both Double and String
    @FXML private TableColumn<Object, String> orderStatusCol;
    @FXML private TableColumn<Object, String> orderTypesCol;

    private boolean isNetworkManager = false;
    private int branchRequestRetries = 0;
    private static final int MAX_BRANCH_RETRIES = 3;
    
    // Static cache to store branches so we can populate immediately if available
    private static List<Branch> cachedBranches = null;

    @FXML
    public void initialize() {
        // Unregister first to avoid stale registrations, then register fresh
        try {
            EventBus.getDefault().unregister(this);
        } catch (IllegalArgumentException e) {
            // Not registered, that's okay
        }
        
        // Register with EventBus
        EventBus.getDefault().register(this);
        System.out.println("ReportsController: Registered with EventBus");

        // Clear chart container and create new chart
        chartContainer.getChildren().clear();
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        mainChart = new BarChart<>(xAxis, yAxis);
        mainChart.setAnimated(false);
        chartContainer.getChildren().add(mainChart);

        // ברירת מחדל לתאריכים
        fromDate.setValue(LocalDate.now().minusDays(7));
        toDate.setValue(LocalDate.now());

        // Set combobox visibility based on user role immediately
        PublicUser currentUser = AppSession.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == Role.NETWORK_MANAGER) {
            isNetworkManager = true;
            branchCombo.setVisible(true);
            System.out.println("ReportsController: Network manager detected, showing branch combobox");
        } else {
            isNetworkManager = false;
            branchCombo.setVisible(false);
            System.out.println("ReportsController: Not network manager, hiding branch combobox");
        }

        // Clear and repopulate report type combobox
        reportTypeCombo.getItems().clear();
        reportTypeCombo.getItems().addAll(
                "Complaints Report",
                "Revenue Report",
                "Orders Report"
        );
        reportTypeCombo.setPromptText("Select report type");
        reportTypeCombo.setValue(null); // Reset selection

        // Clear branch combobox and request branches
        if (branchCombo != null) {
            branchCombo.getItems().clear();
            branchCombo.setValue(null); // Reset selection
            branchCombo.setPromptText("Select Branch");
            
            // If we have cached branches, populate immediately
            if (cachedBranches != null && !cachedBranches.isEmpty()) {
                System.out.println("ReportsController: Using cached branches (" + cachedBranches.size() + " branches)");
                branchCombo.getItems().setAll(cachedBranches);
            }
        }

        // Request branches AFTER EventBus registration to ensure we receive them
        // Use Platform.runLater to ensure EventBus is fully ready
        branchRequestRetries = 0; // Reset retry counter
        Platform.runLater(() -> {
            requestBranches();
        });

        infoLabel.setVisible(false);
        
        // Initialize orders table - set up columns and hide it by default
        if (ordersTable != null) {
            setupOrdersTable();
            ordersTable.setVisible(false);
            ordersTable.setManaged(false);
            ordersTable.getItems().clear();
            System.out.println("ReportsController: Orders table initialized and hidden");
        } else {
            System.err.println("ReportsController: WARNING - ordersTable is null in initialize()!");
        }
    }

    @FXML
    private void onGenerateReport() {
        if (AppSession.getCurrentUser() == null) {
            showAlert("Please log in first.", Alert.AlertType.WARNING);
            try { App.setRoot("Login"); } catch (IOException ignored) {}
            return;
        }

        // Refresh client reference - it might have changed
        client = SimpleClient.client;
        
        // Check if client exists
        if (client == null) {
            showAlert("Not connected to server. Please check your connection.", Alert.AlertType.ERROR);
            return;
        }
        
        // Hide info label and table (will be shown again when report data arrives)
        infoLabel.setVisible(false);
        infoLabel.setText("");
        if (ordersTable != null) {
            ordersTable.setVisible(false);
            ordersTable.getItems().clear();
        }

        String type = reportTypeCombo.getValue();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        if (type == null) {
            showAlert("Please select a report type.", Alert.AlertType.WARNING);
            return;
        }
        if (from == null || to == null || from.isAfter(to)) {
            showAlert("Invalid date range.", Alert.AlertType.WARNING);
            return;
        }

        Integer branchId = null;
        if (branchCombo.isVisible()) {
            // Network manager - can select a branch or "All Network" (null)
            if (branchCombo.getValue() != null) {
                branchId = branchCombo.getValue().getId();
            }
            // If null, it means "All Network" - this is valid for network managers
        } else {
            // Branch manager - use their branch
            branchId = AppSession.getCurrentUser().getBranchId();
            if (branchId == null) {
                showAlert("Please select a branch.", Alert.AlertType.WARNING);
                return;
            }
        }

        try {
            System.out.println("ReportsController: Sending report request - Type: " + type + ", BranchId: " + branchId + ", From: " + from + ", To: " + to);
            
            // Ensure client is connected and stable before sending
            if (!client.isConnected()) {
                System.out.println("ReportsController: Client not connected, attempting to connect...");
                try {
                    client.openConnection();
                    Thread.sleep(300); // Wait for connection to establish
                    client.sendToServer("add client");
                    Thread.sleep(500); // Wait for server to register and send initial response
                } catch (Exception connectEx) {
                    System.err.println("ReportsController: Failed to connect: " + connectEx.getMessage());
                    showAlert("Not connected to server. Please check your connection.", Alert.AlertType.ERROR);
                    return;
                }
            }
            
            // Send the request
            try {
                System.out.println("ReportsController: Preparing to send report - Type: " + type + ", BranchId: " + branchId + " (null = All Network), From: " + from + ", To: " + to);
                // Use ArrayList instead of List.of() to allow null branchId
                List<Object> reportData = new ArrayList<>();
                reportData.add(branchId);  // Can be null for network-wide reports
                reportData.add(from);
                reportData.add(to);
                
                switch (type) {
                    case "Complaints Report" -> client.sendToServer(new Message("getComplaintsReport", reportData));
                    case "Revenue Report" -> client.sendToServer(new Message("getRevenueReport", reportData));
                    case "Orders Report" -> client.sendToServer(new Message("getOrdersReport", reportData));
                    default -> {
                        showAlert("Unknown report type.", Alert.AlertType.ERROR);
                        return;
                    }
                }
                System.out.println("ReportsController: Report request sent successfully");
            } catch (java.net.SocketException e) {
                // Socket error - try to reconnect and resend
                System.err.println("ReportsController: Socket error detected: " + e.getMessage() + ", attempting to reconnect...");
                try {
                    if (!client.isConnected()) {
                        client.openConnection();
                        Thread.sleep(300);
                    }
                    client.sendToServer("add client");
                    Thread.sleep(500); // Wait for server response before resending report request
                    
                    // Resend the request
                    switch (type) {
                        case "Complaints Report" -> client.sendToServer(new Message("getComplaintsReport", List.of(branchId, from, to)));
                        case "Revenue Report" -> client.sendToServer(new Message("getRevenueReport", List.of(branchId, from, to)));
                        case "Orders Report" -> client.sendToServer(new Message("getOrdersReport", List.of(branchId, from, to)));
                    }
                    System.out.println("ReportsController: Reconnected and report request resent successfully");
                } catch (Exception reconnectEx) {
                    System.err.println("ReportsController: Failed to reconnect: " + reconnectEx.getMessage());
                    reconnectEx.printStackTrace();
                    showAlert("Connection lost. Please try again.", Alert.AlertType.ERROR);
                }
            } catch (IOException e) {
                System.err.println("ReportsController: Failed to send report request: " + e.getMessage());
                e.printStackTrace();
                showAlert("Failed to send request to server: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            System.err.println("ReportsController: Unexpected error sending report request: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "An unexpected error occurred: " + e.getClass().getSimpleName();
            }
            showAlert("Unexpected error: " + errorMsg, Alert.AlertType.ERROR);
        }
    }

    // ========================== 📊 תלונות ==========================
    @Subscribe
    public void onComplaintsReportReceived(ComplaintsReportEvent event) {
        Platform.runLater(() -> {
            System.out.println("ReportsController: Received complaints report with " + event.getComplaints().size() + " complaints");
            
            // Create LineChart for complaints report
            setupLineChart("Complaints per Day", "Date", "Number of Complaints", "#f5a7b8");

            // Group complaints by date
            Map<String, Long> grouped = event.getComplaints().stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getCreatedAt().toLocalDate().toString(),
                            Collectors.counting()
                    ));

            // Generate all dates in the selected range (from date picker)
            LocalDate fromDateValue = fromDate.getValue();
            LocalDate toDateValue = toDate.getValue();
            List<String> allDatesInRange = new ArrayList<>();
            
            if (fromDateValue != null && toDateValue != null) {
                LocalDate current = fromDateValue;
                while (!current.isAfter(toDateValue)) {
                    allDatesInRange.add(current.toString());
                    current = current.plusDays(1);
                }
            } else {
                // Fallback: use dates from complaints if date pickers are null
                allDatesInRange = new ArrayList<>(grouped.keySet());
                allDatesInRange.sort(String::compareTo);
            }

            // Add complaints series - include all dates, 0 for dates with no complaints
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Complaints");
            
            allDatesInRange.forEach(date -> {
                Long count = grouped.getOrDefault(date, 0L);
                series.getData().add(new XYChart.Data<>(date, count));
            });
            
            if (revenueLineChart != null) {
                revenueLineChart.getData().clear();
                revenueLineChart.getData().add(series);
            }

            // Setup and populate complaints table
            if (ordersTable != null) {
                setupComplaintsTable();
                ObservableList<Object> tableData = FXCollections.observableArrayList();
                
                for (Complaint c : event.getComplaints()) {
                    String date = c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "Unknown";
                    String status = c.getStatus() != null ? c.getStatus().toString() : "Unknown";
                    String clientName = c.getClientName() != null ? c.getClientName() : "Unknown";
                    String clientEmail = c.getClientEmail() != null ? c.getClientEmail() : "Unknown";
                    String description = c.getDescription() != null ? c.getDescription() : "";
                    
                    tableData.add(new ComplaintRow(
                        String.valueOf(c.getComplaintId()),
                        date,
                        clientName,
                        clientEmail,
                        description,
                        status
                    ));
                }
                
                ordersTable.setItems(tableData);
                ordersTable.setManaged(true);
                ordersTable.setVisible(true);
                ordersTable.refresh();
                System.out.println("ReportsController: Populated complaints table with " + tableData.size() + " rows");
            }

            infoLabel.setText("📅 " + allDatesInRange.size() + " days shown (" + event.getComplaints().size() + " total complaints)");
            infoLabel.setVisible(true);
        });
    }

    // ========================== 💰 דו"ח הכנסות ==========================
    @Subscribe
    public void onRevenueReportReceived(RevenueReportEvent event) {
        Platform.runLater(() -> {
            System.out.println("ReportsController: Received revenue report with " + event.getOrders().size() + " orders");
            
            // Create LineChart for revenue report
            setupLineChart("Revenue Report", "Date", "Revenue (₪)", "#b97a95");
            
            // Ensure chart reference is set
            if (revenueLineChart == null) {
                System.err.println("ReportsController: ERROR - revenueLineChart is null after setup!");
                return;
            }

            // Separate cancelled and uncancelled orders
            List<Order> uncancelledOrders = event.getOrders().stream()
                    .filter(o -> o.getStatus() == null || !o.getStatus().equalsIgnoreCase("Cancelled"))
                    .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                    .collect(Collectors.toList());
            
            List<Order> cancelledOrders = event.getOrders().stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().equalsIgnoreCase("Cancelled"))
                    .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                    .collect(Collectors.toList());

            // Group uncancelled orders by date (for revenue calculation)
            Map<String, Double> uncancelledGrouped = uncancelledOrders.stream()
                    .collect(Collectors.groupingBy(
                            o -> o.getCreatedAt().toLocalDate().toString(),
                            Collectors.summingDouble(Order::getTotalPrice)
                    ));

            // Group cancelled orders by date (for display only)
            Map<String, Double> cancelledGrouped = cancelledOrders.stream()
                    .collect(Collectors.groupingBy(
                            o -> o.getCreatedAt().toLocalDate().toString(),
                            Collectors.summingDouble(Order::getTotalPrice)
                    ));
            
            System.out.println("ReportsController: Grouped into " + uncancelledGrouped.size() + " dates with revenue, " + cancelledGrouped.size() + " dates with cancelled revenue");

            // Generate all dates in the selected range (from date picker)
            LocalDate fromDateValue = fromDate.getValue();
            LocalDate toDateValue = toDate.getValue();
            List<String> allDatesInRange = new ArrayList<>();
            
            if (fromDateValue != null && toDateValue != null) {
                LocalDate current = fromDateValue;
                while (!current.isAfter(toDateValue)) {
                    allDatesInRange.add(current.toString());
                    current = current.plusDays(1);
                }
            } else {
                // Fallback: use dates from orders if date pickers are null
                allDatesInRange = new ArrayList<>(uncancelledGrouped.keySet());
                allDatesInRange.sort(String::compareTo);
            }
            
            // Add uncancelled orders series (main revenue) - include all dates, 0 for dates with no orders
            // Note: setupLineChart() already created a fresh chart, so we can directly add data
            XYChart.Series<String, Number> uncancelledSeries = new XYChart.Series<>();
            uncancelledSeries.setName("Revenue - Active Orders (₪)");
            allDatesInRange.forEach(date -> {
                Double revenue = uncancelledGrouped.getOrDefault(date, 0.0);
                XYChart.Data<String, Number> data = new XYChart.Data<>(date, revenue);
                uncancelledSeries.getData().add(data);
            });
            
            if (revenueLineChart != null) {
                // Ensure chart is empty before adding new data
                revenueLineChart.getData().clear();
                System.out.println("ReportsController: Cleared chart data, current series count: " + revenueLineChart.getData().size());
                
                revenueLineChart.getData().add(uncancelledSeries);
                System.out.println("ReportsController: Added " + uncancelledSeries.getData().size() + " data points to revenue chart (from " + fromDateValue + " to " + toDateValue + "), series count: " + revenueLineChart.getData().size());
            } else {
                System.err.println("ReportsController: ERROR - revenueLineChart is null when trying to add data!");
            }

            // Add cancelled orders series (for reference, not counted in total) - include all dates, 0 for dates with no cancelled orders
            if (revenueLineChart != null) {
                XYChart.Series<String, Number> cancelledSeries = new XYChart.Series<>();
                cancelledSeries.setName("Revenue - Cancelled Orders (₪)");
                allDatesInRange.forEach(date -> {
                    Double revenue = cancelledGrouped.getOrDefault(date, 0.0);
                    cancelledSeries.getData().add(new XYChart.Data<>(date, revenue));
                });
                revenueLineChart.getData().add(cancelledSeries);
                System.out.println("ReportsController: Added cancelled series, total series count: " + revenueLineChart.getData().size());
            }

            // Calculate totals and statistics
            double totalRevenue = uncancelledGrouped.values().stream().mapToDouble(Double::doubleValue).sum();
            double cancelledRevenue = cancelledGrouped.values().stream().mapToDouble(Double::doubleValue).sum();
            double avgOrderValue = uncancelledOrders.isEmpty() ? 0.0 : totalRevenue / uncancelledOrders.size();
            
            // Setup and populate orders table if available
            if (ordersTable != null) {
                setupOrdersTable();
                ObservableList<Object> tableData = FXCollections.observableArrayList();
                for (Order o : uncancelledOrders) {
                    String customerName = "Unknown";
                    try {
                        if (o.getUserAccount() != null) {
                            customerName = o.getUserAccount().getName();
                            if (customerName == null) {
                                customerName = "Unknown";
                            }
                        }
                    } catch (Exception e) {
                        // Handle LazyInitializationException gracefully
                        System.err.println("ReportsController: Could not load user account name for order " + o.getId() + ": " + e.getMessage());
                        customerName = "Unknown";
                    }
                    int itemCount = 0;
                    try {
                        if (o.getOrderLines() != null && !o.getOrderLines().isEmpty()) {
                            itemCount = o.getOrderLines().size();
                            System.out.println("ReportsController: Order " + o.getId() + " has " + itemCount + " order lines");
                        } else {
                            // If no OrderLines but order has a total, estimate from total price
                            // This handles old orders that may not have OrderLines saved
                            if (o.getTotalPrice() > 0) {
                                // Rough estimate: assume average item price of 30₪
                                itemCount = (int) Math.max(1, Math.round(o.getTotalPrice() / 30.0));
                                System.out.println("ReportsController: Order " + o.getId() + " has no OrderLines, estimated " + itemCount + " items from total " + o.getTotalPrice());
                            } else {
                                System.out.println("ReportsController: Order " + o.getId() + " has null/empty orderLines and no total");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("ReportsController: Error getting orderLines for order " + o.getId() + ": " + e.getMessage());
                        // Fallback: estimate from total if available
                        if (o.getTotalPrice() > 0) {
                            itemCount = (int) Math.max(1, Math.round(o.getTotalPrice() / 30.0));
                        }
                    }
                    String date = o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate().toString() : "Unknown";
                    String status = o.getStatus() != null ? o.getStatus() : "Pending";
                    // Collect item types from OrderLines
                    Set<String> itemTypesSet = new HashSet<>();
                    try {
                        if (o.getOrderLines() != null && !o.getOrderLines().isEmpty()) {
                            for (var line : o.getOrderLines()) {
                                if (line != null && line.getItem() != null && line.getItem().getType() != null) {
                                    itemTypesSet.add(line.getItem().getType());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("ReportsController: Error getting item types for order " + o.getId() + ": " + e.getMessage());
                    }
                    String itemTypes = itemTypesSet.isEmpty() ? "Unknown" : String.join(", ", itemTypesSet);
                    
                    tableData.add(new OrderRow(String.valueOf(o.getId()), date, customerName, itemCount, itemTypes, o.getTotalPrice(), status));
                }
                ordersTable.setItems(tableData);
                ordersTable.setManaged(true);
                ordersTable.setVisible(true);
                ordersTable.refresh();
            }
            
            // Enhanced summary text
            String infoText = String.format(
                "📊 Summary: Total Revenue (Active): %.2f₪ | Cancelled Revenue: %.2f₪ | Average Order Value: %.2f₪\n" +
                "📦 Orders: Total: %d | Active: %d | Cancelled: %d",
                totalRevenue, cancelledRevenue, avgOrderValue,
                event.getOrders().size(), uncancelledOrders.size(), cancelledOrders.size()
            );
            infoLabel.setText(infoText);
            infoLabel.setVisible(true);
        });
    }
    
    // Helper class for order table rows
    // Row class for complaints table
    private static class ComplaintRow {
        String complaintId;
        String date;
        String clientName;
        String clientEmail;
        String description;
        String status;
        
        ComplaintRow(String complaintId, String date, String clientName, String clientEmail, String description, String status) {
            this.complaintId = complaintId;
            this.date = date;
            this.clientName = clientName;
            this.clientEmail = clientEmail;
            this.description = description;
            this.status = status;
        }
    }
    
    private static class OrderRow {
        public final String orderId;
        public final String date;
        public final String customer;
        public final int itemsCount;
        public final String itemTypes;  // Comma-separated list of item types
        public final double total;
        public final String status;
        
        public OrderRow(String orderId, String date, String customer, int itemsCount, String itemTypes, double total, String status) {
            this.orderId = orderId;
            this.date = date;
            this.customer = customer;
            this.itemsCount = itemsCount;
            this.itemTypes = itemTypes;
            this.total = total;
            this.status = status;
        }
    }
    
    private void setupOrdersTable() {
        if (ordersTable == null) {
            System.err.println("ReportsController: setupOrdersTable - ordersTable is null!");
            return;
        }
        if (orderIdCol == null) {
            System.err.println("ReportsController: setupOrdersTable - orderIdCol is null!");
            return;
        }
        
        System.out.println("ReportsController: Setting up table columns for orders");
        
        orderIdCol.setCellValueFactory(d -> {
            Object row = d.getValue();
            if (row instanceof OrderRow) {
                return new SimpleStringProperty(((OrderRow) row).orderId);
            }
            return new SimpleStringProperty("");
        });
        orderIdCol.setText("Order ID");
        
        if (orderDateCol != null) {
            orderDateCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof OrderRow) {
                    return new SimpleStringProperty(((OrderRow) row).date);
                }
                return new SimpleStringProperty("");
            });
            orderDateCol.setText("Date");
        } else {
            System.err.println("ReportsController: orderDateCol is null!");
        }
        if (orderCustomerCol != null) {
            orderCustomerCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof OrderRow) {
                    return new SimpleStringProperty(((OrderRow) row).customer);
                }
                return new SimpleStringProperty("");
            });
            orderCustomerCol.setText("Customer");
        } else {
            System.err.println("ReportsController: orderCustomerCol is null!");
        }
        if (orderItemsCol != null) {
            orderItemsCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof OrderRow) {
                    return new SimpleObjectProperty<>(((OrderRow) row).itemsCount);
                }
                return new SimpleObjectProperty<>(0);
            });
            orderItemsCol.setText("Items");
        } else {
            System.err.println("ReportsController: orderItemsCol is null!");
        }
        if (orderTotalCol != null) {
            orderTotalCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof OrderRow) {
                    return new SimpleObjectProperty<>(((OrderRow) row).total);
                }
                return new SimpleObjectProperty<>(0.0);
            });
            orderTotalCol.setText("Total (₪)");
            orderTotalCol.setCellFactory(column -> new TableCell<Object, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else if (item instanceof Double) {
                        setText(String.format("%.2f₪", (Double) item));
                    } else {
                        setText(item.toString());
                    }
                }
            });
        } else {
            System.err.println("ReportsController: orderTotalCol is null!");
        }
        if (orderTypesCol != null) {
            orderTypesCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof OrderRow) {
                    return new SimpleStringProperty(((OrderRow) row).itemTypes);
                }
                return new SimpleStringProperty("");
            });
            orderTypesCol.setText("Item Types");
            orderTypesCol.setVisible(true);  // Show for orders
        } else {
            System.err.println("ReportsController: orderTypesCol is null!");
        }
        
        if (orderStatusCol != null) {
            orderStatusCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof OrderRow) {
                    return new SimpleStringProperty(((OrderRow) row).status);
                }
                return new SimpleStringProperty("");
            });
            orderStatusCol.setText("Status");
        } else {
            System.err.println("ReportsController: orderStatusCol is null!");
        }
        
        System.out.println("ReportsController: Table columns setup complete for orders");
    }
    
    private void setupComplaintsTable() {
        if (ordersTable == null) {
            System.err.println("ReportsController: setupComplaintsTable - ordersTable is null!");
            return;
        }
        if (orderIdCol == null) {
            System.err.println("ReportsController: setupComplaintsTable - orderIdCol is null!");
            return;
        }
        
        System.out.println("ReportsController: Setting up table columns for complaints");
        
        // Reuse the same columns but bind them to ComplaintRow fields
        orderIdCol.setCellValueFactory(d -> {
            Object row = d.getValue();
            if (row instanceof ComplaintRow) {
                return new SimpleStringProperty(((ComplaintRow) row).complaintId);
            }
            return new SimpleStringProperty("");
        });
        orderIdCol.setText("Complaint ID");
        
        if (orderDateCol != null) {
            orderDateCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof ComplaintRow) {
                    return new SimpleStringProperty(((ComplaintRow) row).date);
                }
                return new SimpleStringProperty("");
            });
            orderDateCol.setText("Date");
        }
        
        if (orderCustomerCol != null) {
            orderCustomerCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof ComplaintRow) {
                    return new SimpleStringProperty(((ComplaintRow) row).clientName);
                }
                return new SimpleStringProperty("");
            });
            orderCustomerCol.setText("Client Name");
        }
        
        if (orderItemsCol != null) {
            orderItemsCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof ComplaintRow) {
                    // Use description in the "Items" column for complaints
                    String desc = ((ComplaintRow) row).description;
                    // Truncate long descriptions
                    if (desc != null && desc.length() > 50) {
                        desc = desc.substring(0, 47) + "...";
                    }
                    return new SimpleObjectProperty<>(desc != null ? desc : "");
                }
                return new SimpleObjectProperty<>("");
            });
            orderItemsCol.setText("Description");
        }
        
        if (orderTotalCol != null) {
            orderTotalCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof ComplaintRow) {
                    return new SimpleObjectProperty<>(((ComplaintRow) row).clientEmail);
                }
                return new SimpleObjectProperty<>("");
            });
            orderTotalCol.setText("Email");
            // Set a plain text cell factory for complaints (no currency formatting)
            orderTotalCol.setCellFactory(column -> new TableCell<Object, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.toString());
                    }
                }
            });
        }
        
        if (orderTypesCol != null) {
            // Hide item types column for complaints (not applicable)
            orderTypesCol.setVisible(false);
        }
        
        if (orderStatusCol != null) {
            orderStatusCol.setCellValueFactory(d -> {
                Object row = d.getValue();
                if (row instanceof ComplaintRow) {
                    return new SimpleStringProperty(((ComplaintRow) row).status);
                }
                return new SimpleStringProperty("");
            });
            orderStatusCol.setText("Status");
        }
        
        System.out.println("ReportsController: Table columns setup complete for complaints");
    }

    // ========================== 🛍 דו"ח הזמנות ==========================
    @Subscribe
    public void onOrdersReportReceived(OrdersReportEvent event) {
        Platform.runLater(() -> {
            System.out.println("ReportsController: Received orders report with " + event.getOrders().size() + " orders");
            
            // Create LineChart for orders report - segmented by product types (with dots only, no connecting lines)
            setupLineChart("Orders Report - Items by Product Type", "Product Type", "Quantity of Items", "#a64f73", true); // true = hide lines, show only dots

            // Separate cancelled and uncancelled orders
            List<Order> uncancelledOrders = event.getOrders().stream()
                    .filter(o -> o.getStatus() == null || !o.getStatus().equalsIgnoreCase("Cancelled"))
                    .collect(Collectors.toList());
            
            List<Order> cancelledOrders = event.getOrders().stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().equalsIgnoreCase("Cancelled"))
                    .collect(Collectors.toList());

            // Group items by product type for uncancelled orders
            Map<String, Integer> typeQuantities = new HashMap<>();
            for (Order o : uncancelledOrders) {
                try {
                    if (o.getOrderLines() != null && !o.getOrderLines().isEmpty()) {
                        for (var line : o.getOrderLines()) {
                            if (line != null && line.getItem() != null) {
                                String itemType = line.getItem().getType();
                                if (itemType == null || itemType.isEmpty()) {
                                    itemType = "Unknown";
                                }
                                int quantity = line.getQuantity();
                                typeQuantities.put(itemType, typeQuantities.getOrDefault(itemType, 0) + quantity);
                            }
                        }
                    } else if (o.getTotalPrice() > 0) {
                        // Fallback for orders without OrderLines - estimate as "Mixed" type
                        int estimatedQty = (int) Math.max(1, Math.round(o.getTotalPrice() / 30.0));
                        typeQuantities.put("Mixed (Estimated)", typeQuantities.getOrDefault("Mixed (Estimated)", 0) + estimatedQty);
                    }
                } catch (Exception e) {
                    System.err.println("ReportsController: Error processing order " + o.getId() + " for type grouping: " + e.getMessage());
                }
            }

            // Group items by product type for cancelled orders (for reference)
            Map<String, Integer> cancelledTypeQuantities = new HashMap<>();
            for (Order o : cancelledOrders) {
                try {
                    if (o.getOrderLines() != null && !o.getOrderLines().isEmpty()) {
                        for (var line : o.getOrderLines()) {
                            if (line != null && line.getItem() != null) {
                                String itemType = line.getItem().getType();
                                if (itemType == null || itemType.isEmpty()) {
                                    itemType = "Unknown";
                                }
                                int quantity = line.getQuantity();
                                cancelledTypeQuantities.put(itemType, cancelledTypeQuantities.getOrDefault(itemType, 0) + quantity);
                            }
                        }
                    } else if (o.getTotalPrice() > 0) {
                        // Fallback for orders without OrderLines
                        int estimatedQty = (int) Math.max(1, Math.round(o.getTotalPrice() / 30.0));
                        cancelledTypeQuantities.put("Mixed (Estimated)", cancelledTypeQuantities.getOrDefault("Mixed (Estimated)", 0) + estimatedQty);
                    }
                } catch (Exception e) {
                    System.err.println("ReportsController: Error processing cancelled order " + o.getId() + " for type grouping: " + e.getMessage());
                }
            }

            // Create series for active orders by product type
            XYChart.Series<String, Number> activeSeries = new XYChart.Series<>();
            activeSeries.setName("Active Orders");
            
            // Sort types alphabetically for better display
            List<String> sortedTypes = new ArrayList<>(typeQuantities.keySet());
            sortedTypes.sort(String::compareTo);
            
            for (String type : sortedTypes) {
                activeSeries.getData().add(new XYChart.Data<>(type, typeQuantities.get(type)));
            }
            
            if (revenueLineChart != null) {
                revenueLineChart.getData().clear();
                revenueLineChart.getData().add(activeSeries);
            }

            // Add cancelled orders series (optional - for reference)
            if (revenueLineChart != null && !cancelledTypeQuantities.isEmpty()) {
                XYChart.Series<String, Number> cancelledSeries = new XYChart.Series<>();
                cancelledSeries.setName("Cancelled Orders");
                
                // Get all types (active + cancelled) and sort
                Set<String> allTypes = new HashSet<>(typeQuantities.keySet());
                allTypes.addAll(cancelledTypeQuantities.keySet());
                List<String> allSortedTypes = new ArrayList<>(allTypes);
                allSortedTypes.sort(String::compareTo);
                
                for (String type : allSortedTypes) {
                    int cancelledQty = cancelledTypeQuantities.getOrDefault(type, 0);
                    if (cancelledQty > 0) {
                        cancelledSeries.getData().add(new XYChart.Data<>(type, cancelledQty));
                    }
                }
                
                if (!cancelledSeries.getData().isEmpty()) {
                    revenueLineChart.getData().add(cancelledSeries);
                }
            }
            
            // Hide connecting lines - show only dots (apply after data is added)
            if (revenueLineChart != null) {
                Platform.runLater(() -> {
                    // Hide all line paths but keep the dots visible
                    revenueLineChart.lookupAll(".chart-series-line").forEach(node -> {
                        node.setStyle("-fx-stroke-width: 0; -fx-stroke: transparent;");
                    });
                    // Also target Path elements that represent the lines
                    revenueLineChart.lookupAll("Path").forEach(node -> {
                        String style = node.getStyle();
                        if (style == null || (!style.contains("stroke-width: 0") && !style.contains("chart-line-symbol"))) {
                            node.setStyle("-fx-stroke: transparent; -fx-stroke-width: 0;");
                        }
                    });
                });
            }

            // Calculate totals
            int totalActiveItems = typeQuantities.values().stream().mapToInt(Integer::intValue).sum();
            int totalCancelledItems = cancelledTypeQuantities.values().stream().mapToInt(Integer::intValue).sum();
            
            // Setup and populate orders table if available
            if (ordersTable != null) {
                System.out.println("ReportsController: Setting up orders table with " + event.getOrders().size() + " orders");
                setupOrdersTable();
                ObservableList<Object> tableData = FXCollections.observableArrayList();
                
                // Add all orders (both active and cancelled) to the table
                for (Order o : event.getOrders()) {
                    String customerName = "Unknown";
                    try {
                        if (o.getUserAccount() != null) {
                            customerName = o.getUserAccount().getName();
                            if (customerName == null) {
                                customerName = "Unknown";
                            }
                        }
                    } catch (Exception e) {
                        // Handle LazyInitializationException gracefully
                        System.err.println("ReportsController: Could not load user account name for order " + o.getId() + ": " + e.getMessage());
                        customerName = "Unknown";
                    }
                    int itemCount = 0;
                    try {
                        if (o.getOrderLines() != null && !o.getOrderLines().isEmpty()) {
                            itemCount = o.getOrderLines().size();
                            System.out.println("ReportsController: Order " + o.getId() + " has " + itemCount + " order lines");
                        } else {
                            // If no OrderLines but order has a total, estimate from total price
                            // This handles old orders that may not have OrderLines saved
                            if (o.getTotalPrice() > 0) {
                                // Rough estimate: assume average item price of 30₪
                                itemCount = (int) Math.max(1, Math.round(o.getTotalPrice() / 30.0));
                                System.out.println("ReportsController: Order " + o.getId() + " has no OrderLines, estimated " + itemCount + " items from total " + o.getTotalPrice());
                            } else {
                                System.out.println("ReportsController: Order " + o.getId() + " has null/empty orderLines and no total");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("ReportsController: Error getting orderLines for order " + o.getId() + ": " + e.getMessage());
                        // Fallback: estimate from total if available
                        if (o.getTotalPrice() > 0) {
                            itemCount = (int) Math.max(1, Math.round(o.getTotalPrice() / 30.0));
                        }
                    }
                    String date = o.getCreatedAt() != null ? o.getCreatedAt().toLocalDate().toString() : "Unknown";
                    String status = o.getStatus() != null ? o.getStatus() : "Pending";
                    // Collect item types from OrderLines
                    Set<String> itemTypesSet = new HashSet<>();
                    try {
                        if (o.getOrderLines() != null && !o.getOrderLines().isEmpty()) {
                            for (var line : o.getOrderLines()) {
                                if (line != null && line.getItem() != null && line.getItem().getType() != null) {
                                    itemTypesSet.add(line.getItem().getType());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("ReportsController: Error getting item types for order " + o.getId() + ": " + e.getMessage());
                    }
                    String itemTypes = itemTypesSet.isEmpty() ? "Unknown" : String.join(", ", itemTypesSet);
                    
                    tableData.add(new OrderRow(String.valueOf(o.getId()), date, customerName, itemCount, itemTypes, o.getTotalPrice(), status));
                }
                
                System.out.println("ReportsController: Populated table with " + tableData.size() + " rows");
                ordersTable.setItems(tableData);
                ordersTable.setManaged(true);
                ordersTable.setVisible(true);
                // Force table to refresh
                ordersTable.refresh();
                System.out.println("ReportsController: Table visible: " + ordersTable.isVisible() + ", managed: " + ordersTable.isManaged() + ", items count: " + ordersTable.getItems().size() + ", columns: " + ordersTable.getColumns().size());
            } else {
                System.err.println("ReportsController: ERROR - ordersTable is null!");
            }
            
            String infoText = String.format("🪷 Total Orders: %d (Active: %d, Cancelled: %d) | Total Items: %d (Active: %d, Cancelled: %d)",
                    event.getOrders().size(), uncancelledOrders.size(), cancelledOrders.size(),
                    totalActiveItems + totalCancelledItems, totalActiveItems, totalCancelledItems);
            infoLabel.setText(infoText);
            infoLabel.setVisible(true);
        });
    }


    // ========================== ⚙️ עזרות ==========================

    @Subscribe
    public void onBranchListReceived(BranchListEvent event) {
        Platform.runLater(() -> {
            System.out.println("ReportsController: Received branch list with " + event.getBranches().size() + " branches");
            
            // Cache the branches for future use
            cachedBranches = new ArrayList<>(event.getBranches());
            
            // Update combobox if it's initialized
            if (branchCombo != null) {
                branchCombo.getItems().setAll(event.getBranches());
                System.out.println("ReportsController: Branch combobox now has " + branchCombo.getItems().size() + " items");
                
                // Ensure visibility is set correctly (should already be set in initialize, but double-check)
                PublicUser currentUser = AppSession.getCurrentUser();
                if (currentUser != null && currentUser.getRole() == Role.NETWORK_MANAGER) {
                    isNetworkManager = true;
                    branchCombo.setVisible(true);
                    System.out.println("ReportsController: Branch combobox is visible for network manager");
                } else {
                    isNetworkManager = false;
                    branchCombo.setVisible(false);
                }
            } else {
                System.err.println("ReportsController: WARNING - branchCombo is null when trying to populate!");
            }
        });
    }

    // Handle report errors from server
    @Subscribe
    public void onReportError(Message msg) {
        if (msg.getType().equals("complaintsReportError") || 
            msg.getType().equals("ordersReportError") || 
            msg.getType().equals("revenueReportError")) {
            Platform.runLater(() -> {
                String errorMsg = (String) msg.getData();
                showAlert("Report generation failed: " + errorMsg, Alert.AlertType.ERROR);
            });
        }
    }

    private void requestBranches() {
        if (branchRequestRetries >= MAX_BRANCH_RETRIES) {
            System.err.println("ReportsController: Max retries reached for branch request");
            return;
        }
        
        // Refresh client reference
        client = SimpleClient.client;
        if (client == null) {
            System.err.println("ReportsController: Client is null, cannot request branches (retry " + (branchRequestRetries + 1) + "/" + MAX_BRANCH_RETRIES + ")");
            // Retry after a short delay if not connected
            branchRequestRetries++;
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(this::requestBranches);
                } catch (InterruptedException ignored) {}
            }).start();
            return;
        }
        
        try {
            System.out.println("ReportsController: Requesting branches from server");
            client.sendToServer("#getAllBranches");
            branchRequestRetries = 0; // Reset on success
        } catch (IOException e) {
            System.err.println("ReportsController: Failed to request branches: " + e.getMessage() + " (retry " + (branchRequestRetries + 1) + "/" + MAX_BRANCH_RETRIES + ")");
            e.printStackTrace();
            // Retry after a short delay
            branchRequestRetries++;
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(this::requestBranches);
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }

    private void setupChart(String title, String xLabel, String yLabel, String color) {
        // Remove existing chart completely and create new BarChart
        chartContainer.getChildren().clear();
        mainChart = null;  // Clear reference
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        mainChart = new BarChart<>(xAxis, yAxis);
        
        // Set chart properties
        mainChart.setTitle(title);
        mainChart.getXAxis().setLabel(xLabel);
        mainChart.getYAxis().setLabel(yLabel);
        mainChart.setLegendVisible(true);
        mainChart.setAnimated(false);
        
        // Set bar gap and category gap to make bars look better (not too wide)
        mainChart.setBarGap(10);  // Gap between bars in the same category
        mainChart.setCategoryGap(50);  // Gap between categories - larger gap prevents bars from being too wide

        // Auto-ranging for Y-axis
        yAxis.setAutoRanging(true);
        yAxis.setLowerBound(0);  // Start from 0

        // Add chart to container
        chartContainer.getChildren().add(mainChart);
        
        // צבעים עדינים לגרף (apply after chart is added)
        Platform.runLater(() -> {
            mainChart.lookupAll(".chart-bar")
                    .forEach(n -> n.setStyle("-fx-bar-fill: " + color + ";"));
        });
        
        System.out.println("ReportsController: Setup chart: " + title);
    }
    
    private void setupLineChart(String title, String xLabel, String yLabel, String color) {
        setupLineChart(title, xLabel, yLabel, color, false);
    }
    
    private void setupLineChart(String title, String xLabel, String yLabel, String color, boolean hideLines) {
        // Remove existing chart completely and create new LineChart
        chartContainer.getChildren().clear();
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        revenueLineChart = new LineChart<>(xAxis, yAxis);
        
        revenueLineChart.setTitle(title);
        revenueLineChart.getXAxis().setLabel(xLabel);
        revenueLineChart.getYAxis().setLabel(yLabel);
        revenueLineChart.setLegendVisible(true);
        revenueLineChart.setAnimated(false);
        revenueLineChart.setCreateSymbols(true);  // Show dots on the line
        revenueLineChart.setAlternativeRowFillVisible(false);
        
        // Auto-ranging for Y-axis
        yAxis.setAutoRanging(true);
        yAxis.setLowerBound(0);  // Start from 0
        
        // Clear any existing data
        revenueLineChart.getData().clear();
        
        chartContainer.getChildren().add(revenueLineChart);
        
        // If hideLines is true, apply CSS to hide the connecting lines (show only dots)
        if (hideLines) {
            Platform.runLater(() -> {
                // Hide the line strokes but keep the dots visible
                revenueLineChart.lookupAll(".chart-line-symbol").forEach(node -> {
                    // Keep symbols visible
                });
                revenueLineChart.lookupAll(".chart-series-line").forEach(node -> {
                    node.setStyle("-fx-stroke-width: 0;"); // Hide the line
                });
                // Also hide the path elements that draw the lines
                revenueLineChart.lookupAll("Path").forEach(node -> {
                    String style = node.getStyle();
                    if (style == null || !style.contains("stroke")) {
                        node.setStyle("-fx-stroke: transparent; -fx-stroke-width: 0;");
                    }
                });
            });
        }
        
        System.out.println("ReportsController: Created new LineChart, chart reference: " + revenueLineChart + (hideLines ? " (lines hidden, dots only)" : ""));
    }

    @FXML
    private void onBack(ActionEvent event) {
        // Unregister from EventBus when leaving the view
        try {
            EventBus.getDefault().unregister(this);
        } catch (IllegalArgumentException e) {
            // Not registered, that's okay
        }
        
        try {
            App.setRoot("MainPage");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
