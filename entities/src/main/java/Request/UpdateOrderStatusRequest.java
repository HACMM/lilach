package Request;

import java.io.Serializable;

public class UpdateOrderStatusRequest implements Serializable {

    private int orderId;
    private String newStatus;
    private int requestingUserId; // למקרה שתרצי לדעת מי עדכן

    public UpdateOrderStatusRequest(int orderId, String newStatus, int requestingUserId) {
        this.orderId = orderId;
        this.newStatus = newStatus;
        this.requestingUserId = requestingUserId;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public int getRequestingUserId() {
        return requestingUserId;
    }

    @Override
    public String toString() {
        return "UpdateOrderStatusRequest{" +
                "orderId=" + orderId +
                ", newStatus='" + newStatus + '\'' +
                ", requestingUserId=" + requestingUserId +
                '}';
    }
}
