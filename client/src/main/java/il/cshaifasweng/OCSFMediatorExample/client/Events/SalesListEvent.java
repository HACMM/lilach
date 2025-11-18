package il.cshaifasweng.OCSFMediatorExample.client.Events;

import il.cshaifasweng.OCSFMediatorExample.entities.Sale;

import java.util.List;

public class SalesListEvent {

    private final List<Sale> sales;

    public SalesListEvent(List<Sale> sales) {
        this.sales = sales;
    }

    public List<Sale> getSales() {
        return sales;
    }
}
