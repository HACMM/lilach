package il.cshaifasweng.OCSFMediatorExample.entities;

import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import java.util.List;

public class RevenueReportEvent {
    private final List<Order> orders;
    public RevenueReportEvent(List<Order> orders) { this.orders = orders; }
    public List<Order> getOrders() { return orders; }
}
