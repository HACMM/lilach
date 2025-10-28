package il.cshaifasweng.OCSFMediatorExample.entities;

import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import java.util.List;

public class OrdersReportEvent {
    private final List<Order> orders;
    public OrdersReportEvent(List<Order> orders) { this.orders = orders; }
    public List<Order> getOrders() { return orders; }
}

