package Request;

import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NewOrderRequest implements Serializable {
    public static class Line implements Serializable {
        public long itemId;
        public int qty;
        public double unitPrice;
        public Line() {}
        public Line(long itemId, int qty, double unitPrice) {
            this.itemId = itemId;
            this.qty = qty;
            this.unitPrice = unitPrice;
        }
    }

    // who is ordering
    public int userId;                  // from PublicUser.getUserId()
    public Integer branchId;            // optional – null if N/A

    // fulfillment
    public String deliveryType;         // "Pickup" | "Delivery"
    public LocalDateTime deliveryDateTime;
    public Double deliveryFee;          // nullable

    // recipient and address (for Delivery)
    public String recipientName;
    public String recipientPhone;
    public String city;
    public String street;
    public String building;

    // extras
    public String greeting;
    public PaymentMethod paymentMethod; // snapshot

    public List<Line> lines = new ArrayList<>();

    public NewOrderRequest() {}
}
