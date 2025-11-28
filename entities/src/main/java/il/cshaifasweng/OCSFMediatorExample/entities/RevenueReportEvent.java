package il.cshaifasweng.OCSFMediatorExample.entities;

import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import java.io.Serializable;
import java.util.List;

public class RevenueReportEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Order> orders;
    public RevenueReportEvent(List<Order> orders) { this.orders = orders; }
    public List<Order> getOrders() { return orders; }
}
