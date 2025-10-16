package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;

public class CartLine {
    private final Item item;
    private int qty;

    public CartLine(Item item, int qty) { this.item = item; this.qty = qty; }

    public Item getItem() { return item; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getUnitPrice() { return item.getPrice(); }
    public double getSubtotal()  { return qty * item.getPrice();
    }
}