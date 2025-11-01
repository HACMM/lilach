package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
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

    private boolean isNetworkManager = false;

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);

        // יצירת הגרף
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        mainChart = new BarChart<>(xAxis, yAxis);
        mainChart.setAnimated(false);
        chartContainer.getChildren().add(mainChart);

        // ברירת מחדל לתאריכים
        fromDate.setValue(LocalDate.now().minusDays(7));
        toDate.setValue(LocalDate.now());

        // בקשת רשימת סניפים
        try {
            client.sendToServer("#getAllBranches");
        } catch (IOException e) {
            e.printStackTrace();
        }

        infoLabel.setVisible(false);

        reportTypeCombo.getItems().addAll(
                "Complaints Report",
                "Revenue Report",
                "Orders Report"
        );
        reportTypeCombo.setPromptText("Select report type");

    }

    @FXML
    private void onGenerateReport() {
        if (AppSession.getCurrentUser() == null) {
            showAlert("Please log in first.", Alert.AlertType.WARNING);
            try { App.setRoot("Login"); } catch (IOException ignored) {}
            return;
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

        Branch branch = branchCombo.isVisible() ? branchCombo.getValue() : AppSession.getCurrentUser().getBranch();
        if (branch == null) {
            showAlert("Please select a branch.", Alert.AlertType.WARNING);
            return;
        }

        try {
            switch (type) {
                case "Complaints Report" -> client.sendToServer(new Message("getComplaintsReport", List.of(branch, from, to)));
                case "Revenue Report" -> client.sendToServer(new Message("getRevenueReport", List.of(branch, from, to)));
                case "Orders Report" -> client.sendToServer(new Message("getOrdersReport", List.of(branch, from, to)));
                default -> showAlert("Unknown report type.", Alert.AlertType.ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Failed to send request to server.", Alert.AlertType.ERROR);
        }
    }

    // ========================== 📊 תלונות ==========================
    @Subscribe
    public void onComplaintsReportReceived(ComplaintsReportEvent event) {
        Platform.runLater(() -> {
            setupChart("Complaints per Day", "Date", "Number of Complaints", "#f5a7b8");

            Map<String, Long> grouped = event.getComplaints().stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getCreatedAt().toLocalDate().toString(),
                            Collectors.counting()
                    ));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Complaints");

            grouped.forEach((date, count) -> series.getData().add(new XYChart.Data<>(date, count)));
            mainChart.getData().add(series);

            infoLabel.setText("📅 " + grouped.size() + " days shown (" + event.getComplaints().size() + " total complaints)");
            infoLabel.setVisible(true);
        });
    }

    // ========================== 💰 דו"ח הכנסות ==========================
    @Subscribe
    public void onRevenueReportReceived(RevenueReportEvent event) {
        Platform.runLater(() -> {
            setupChart("Revenue Report", "Date", "Revenue (₪)", "#b97a95");

            Map<String, Double> grouped = event.getOrders().stream()
                    .collect(Collectors.groupingBy(
                            o -> o.getCreatedAt().toLocalDate().toString(),
                            Collectors.summingDouble(Order::getTotalPrice)
                    ));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Revenue (₪)");

            grouped.forEach((date, revenue) -> series.getData().add(new XYChart.Data<>(date, revenue)));
            mainChart.getData().add(series);

            double totalRevenue = grouped.values().stream().mapToDouble(Double::doubleValue).sum();
            infoLabel.setText("💵 Total Revenue: " + String.format("%.2f₪", totalRevenue));
            infoLabel.setVisible(true);
        });
    }

    // ========================== 🛍 דו"ח הזמנות ==========================
    @Subscribe
    public void onOrdersReportReceived(OrdersReportEvent event) {
        Platform.runLater(() -> {
            setupChart("Orders Report", "Product Type", "Number of Orders", "#a64f73");

            // Group by item type using OrderLines
            Map<String, Long> grouped = event.getOrders().stream()
                    .flatMap(o -> o.getOrderLines().stream()
                            .map(ol -> {
                                Item it = ol.getItem();
                                String t = (it != null && it.getType() != null) ? it.getType() : "UNKNOWN";
                                return t;
                            }))
                    .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Orders per Type");

            grouped.forEach((type, count) -> series.getData().add(new XYChart.Data<>(type, count)));
            mainChart.getData().add(series);

            infoLabel.setText("🪷 Total orders: " + event.getOrders().size());
            infoLabel.setVisible(true);
        });
    }


    // ========================== ⚙️ עזרות ==========================

    @Subscribe
    public void onBranchListReceived(BranchListEvent event) {
        Platform.runLater(() -> {
            branchCombo.getItems().setAll(event.getBranches());

            if (AppSession.getCurrentUser() != null &&
                    AppSession.getCurrentUser().getRole() == Role.NETWORK_MANAGER) {
                isNetworkManager = true;
                branchCombo.setVisible(true);
            } else {
                branchCombo.setVisible(false);
            }
        });
    }

    private void setupChart(String title, String xLabel, String yLabel, String color) {
        mainChart.getData().clear();
        mainChart.setTitle(title);
        mainChart.getXAxis().setLabel(xLabel);
        mainChart.getYAxis().setLabel(yLabel);

        // צבעים עדינים לגרף
        mainChart.lookupAll(".chart-bar")
                .forEach(n -> n.setStyle("-fx-bar-fill: " + color + ";"));
    }

    @FXML
    private void onBack(ActionEvent event) {
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
