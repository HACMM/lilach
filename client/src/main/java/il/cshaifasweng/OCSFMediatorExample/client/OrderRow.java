package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.OrderLine;
import java.util.List;

public class OrderRow {

    private final String orderId;
    private final String date;
    private final int itemsCount;
    private final double totalPrice;
    private final String status;
    private final String branchName;
    private final List<OrderLine> items;

    public OrderRow(String orderId, String date, int itemsCount,
                    double total, String status, String branchName,
                    List<OrderLine> items) {
        this.orderId = orderId;
        this.date = date;
        this.itemsCount = itemsCount;
        this.totalPrice = total;
        this.status = status;
        this.branchName = branchName;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public String getDate() { return date; }
    public int getItemsCount() { return itemsCount; }
    public double getTotal() { return totalPrice; }
    public String getStatus() { return status; }
    public String getBranchName() { return branchName; }
    public List<OrderLine> getItems() { return items; }
}
