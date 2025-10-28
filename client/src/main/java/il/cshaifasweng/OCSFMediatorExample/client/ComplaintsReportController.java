package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import il.cshaifasweng.OCSFMediatorExample.client.Events.BranchListEvent;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import il.cshaifasweng.OCSFMediatorExample.entities.ComplaintsReportEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static il.cshaifasweng.OCSFMediatorExample.client.SimpleClient.client;

public class ComplaintsReportController {

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private BarChart<String, Number> complaintsChart;
    @FXML private Button generateBtn;
    @FXML private Label infoLabel;

    private boolean isNetworkManager = false; // נקבע לפי סוג המשתמש המחובר

    @FXML
    public void initialize() {
        EventBus.getDefault().register(this);

        complaintsChart.setAnimated(false);
        complaintsChart.setTitle("Complaints per Day");
        complaintsChart.getXAxis().setLabel("Date");
        complaintsChart.getYAxis().setLabel("Number of Complaints");

        // נבקש מהשרת רשימת סניפים
        try {
            client.sendToServer("#getAllBranches");
        } catch (IOException e) {
            System.err.println("⚠️ Failed to load branches: " + e.getMessage());
        }

        // נקבע ברירת מחדל לתאריכים (שבוע אחורה)
        fromDate.setValue(LocalDate.now().minusDays(7));
        toDate.setValue(LocalDate.now());
    }

    @FXML
    private void onGenerateReport() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        if (from == null || to == null || from.isAfter(to)) {
            showAlert("Invalid date range. Please select valid dates.", Alert.AlertType.WARNING);
            return;
        }

        Branch selectedBranch = branchCombo.isVisible() ? branchCombo.getValue() : AppSession.getCurrentUser().getBranch();
        if (selectedBranch == null) {
            showAlert("Please select a branch.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Message msg = new Message("getComplaintsReport", List.of(selectedBranch, from, to));
            client.sendToServer(msg);
        } catch (IOException e) {
            showAlert("Failed to fetch report from server.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onComplaintsReportReceived(ComplaintsReportEvent event) {
        Platform.runLater(() -> {
            complaintsChart.getData().clear();

            Map<String, Long> grouped = event.getComplaints().stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getCreatedAt().toLocalDate().toString(),
                            Collectors.counting()
                    ));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Complaints Count");

            grouped.forEach((date, count) -> series.getData().add(new XYChart.Data<>(date, count)));
            complaintsChart.getData().add(series);

            infoLabel.setText("🗓 " + grouped.size() + " days shown (" + event.getComplaints().size() + " total complaints)");
            infoLabel.setVisible(true);
        });
    }

    @Subscribe
    public void onBranchListReceived(BranchListEvent event) {
        Platform.runLater(() -> {
            branchCombo.getItems().setAll(event.getBranches());

            if (AppSession.getCurrentUser() != null &&
                    AppSession.getCurrentUser().getRole().equalsIgnoreCase("NetworkManager")) {
                isNetworkManager = true;
                branchCombo.setVisible(true);
            } else {
                branchCombo.setVisible(false);
            }
        });
    }

    @FXML
    private void onBack() {
        try {
            if (AppSession.getCurrentUser() != null &&
                    AppSession.getCurrentUser().getRole().equalsIgnoreCase("NetworkManager")) {
                App.setRoot("NetworkManagerDashboard");
            } else {
                App.setRoot("ManagerDashboard");
            }
        } catch (IOException e) {
            showAlert("Navigation failed.", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
