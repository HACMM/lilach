package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class CartItem {
    private final Item item;
    private final IntegerProperty qty = new SimpleIntegerProperty(1);

    public CartItem(Item item, int qty) {
        this.item = item;
        this.qty.set(Math.max(1, qty));
    }

    public Item getItem() { return item; }
    public int getQty() { return qty.get(); }
    public void setQty(int q) { qty.set(Math.max(1, q)); }
    public IntegerProperty qtyProperty() { return qty; }

    public double getSubtotal() {
        return item.getPrice() * getQty();
    }
}
