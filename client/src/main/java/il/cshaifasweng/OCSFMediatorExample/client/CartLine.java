package il.cshaifasweng.OCSFMediatorExample.client;

public class CartLine {
    private final CatalogItem item;
    private int qty;

    public CartLine(CatalogItem item, int qty) { this.item = item; this.qty = qty; }

    public CatalogItem getItem() { return item; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getUnitPrice() { return item.getPrice(); }
    public double getSubtotal()  { return qty * item.getPrice();
    }
}