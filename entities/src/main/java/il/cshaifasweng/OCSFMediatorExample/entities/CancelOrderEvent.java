package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class CancelOrderEvent implements Serializable {
    private final String orderNumber;
    private Order order;
    private double refundAmount;
    private String Status;
    private String customerEmail;

    public CancelOrderEvent(int orderNumber, Order order, double refundAmount) {
        this.orderNumber = String.valueOf(orderNumber);
        this.order = order;
        this.refundAmount = refundAmount;
    }

    public CancelOrderEvent(String orderNumber, String customerEmail) {
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
    }

    public CancelOrderEvent(Order order, String orderNumber) {
        this.order = order;
        this.orderNumber = orderNumber;
    }

    public CancelOrderEvent(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String Status) {
        this.Status =Status;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
}