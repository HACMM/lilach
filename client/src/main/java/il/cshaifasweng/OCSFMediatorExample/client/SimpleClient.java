package il.cshaifasweng.OCSFMediatorExample.client;

import Request.Message;
import Request.SignupResult;
import il.cshaifasweng.OCSFMediatorExample.client.Events.*;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import Request.LoginResult;
import jdk.jfr.Event;
import org.greenrobot.eventbus.EventBus;
import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import Request.Warning;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.List;

public class SimpleClient extends AbstractClient {

    public static SimpleClient client;

    private SimpleClient(String host, int port) {
        super(host, port);
    }


    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg.getClass().equals(Warning.class)) {
            EventBus.getDefault().post(new WarningEvent((Warning) msg));
        } else {
            String message = msg.toString();
            System.out.println("Server: " + message);

            if (message.startsWith("showCatalog")) {
                EventBus.getDefault().post("showCatalog");
            }
            else if (msg instanceof LoginResult) {
                LoginResult loginResult = (LoginResult) msg;

                EventBus.getDefault().post(new LoginResponseEvent(loginResult.isSuccess(),
                        loginResult.getMessage(), loginResult.getUser()));

            } else if (msg instanceof SignupResult) {
                SignupResult signupResult = (SignupResult) msg;
               EventBus.getDefault().post(SignupResponseEvent.from(signupResult));

            } else if (msg instanceof il.cshaifasweng.OCSFMediatorExample.entities.ComplaintsReportEvent) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof il.cshaifasweng.OCSFMediatorExample.entities.OrdersReportEvent) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof il.cshaifasweng.OCSFMediatorExample.entities.RevenueReportEvent) {
                EventBus.getDefault().post(msg);

            } else if (msg instanceof Message && ((Message) msg).getType().equals("item added successfully")) {
                Item item = (Item) ((Message) msg).getData();
                long itemId = item.getId();
                EventBus.getDefault().post(new AddItemEvent(itemId, item));

            } else if (msg instanceof Message && ((Message) msg).getType().equals("branch list")) {
                @SuppressWarnings("unchecked")
                List<Branch> branches = (List<Branch>) ((Message) msg).getData();
                System.out.println("SimpleClient: Received branch list with " + branches.size() + " branches");
                EventBus.getDefault().post(new BranchListEvent(branches));

            } else if (msg instanceof Message && ((Message) msg).getType().equals("newOrderOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("newComplaintOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("newComplaintError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("complaintResolved")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("resolveComplaintError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("updateUserDetailsOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("updateUserDetailsError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("updatePaymentMethodOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("updatePaymentMethodError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("purchaseSubscriptionOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("purchaseSubscriptionError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("renewSubscriptionOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("renewSubscriptionError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("customOrderOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("customOrderError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("item removed successfully")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("item remove error")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("cancelOrderOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("cancelOrderError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("customersList")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("employeesList")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("addEmployeeOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("addEmployeeError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("removeEmployeeOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("removeCustomerOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("removeCustomerError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("newsletterSendOk")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof Message && ((Message) msg).getType().equals("newsletterSendError")) {
                EventBus.getDefault().post(msg);
            } else if (msg instanceof List<?>) {
                List<?> list = (List<?>) msg;
                if (!list.isEmpty()) {
                    if (list.get(0) instanceof Item) {
                        List<Item> items = (List<Item>) list;
                        System.out.println("Received catalog with " + items.size() + " items.");
                        EventBus.getDefault().post(items);
                    } else if (list.get(0) instanceof il.cshaifasweng.OCSFMediatorExample.entities.Complaint) {
                        @SuppressWarnings("unchecked")
                        List<il.cshaifasweng.OCSFMediatorExample.entities.Complaint> complaints = 
                            (List<il.cshaifasweng.OCSFMediatorExample.entities.Complaint>) list;
                        EventBus.getDefault().post(complaints);
                    } else if (list.get(0) instanceof il.cshaifasweng.OCSFMediatorExample.entities.Order) {
                        @SuppressWarnings("unchecked")
                        List<il.cshaifasweng.OCSFMediatorExample.entities.Order> orders = 
                            (List<il.cshaifasweng.OCSFMediatorExample.entities.Order>) list;
                        System.out.println("SimpleClient: Received " + orders.size() + " orders");
                        EventBus.getDefault().post(orders);
                    } else if (list.get(0) instanceof il.cshaifasweng.OCSFMediatorExample.entities.Category) {
                        @SuppressWarnings("unchecked")
                        List<il.cshaifasweng.OCSFMediatorExample.entities.Category> categories =
                                (List<il.cshaifasweng.OCSFMediatorExample.entities.Category>) list;

                        System.out.println("Received " + categories.size() + " categories from server");
                        EventBus.getDefault().post(categories);
                    }

                }
            }
        }
    }

    public static SimpleClient getClient(String host, int port) {
        if (client == null) {
            client = new SimpleClient(host, port);
        }
        return client;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}
