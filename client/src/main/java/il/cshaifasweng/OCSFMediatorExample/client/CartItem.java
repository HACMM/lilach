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
        // Use sale price if available, otherwise use original price
        SalePriceHelper.SalePriceResult priceResult = SalePriceHelper.calculateSalePrice(item);
        double priceToUse = priceResult.hasActiveSale() ? priceResult.getSalePrice() : priceResult.getOriginalPrice();
        return priceToUse * getQty();
    }
    
    /**
     * Gets the effective price (sale price if active, otherwise original price).
     */
    public double getEffectivePrice() {
        SalePriceHelper.SalePriceResult priceResult = SalePriceHelper.calculateSalePrice(item);
        return priceResult.hasActiveSale() ? priceResult.getSalePrice() : priceResult.getOriginalPrice();
    }
    
    /**
     * Gets the original price (before any sales).
     */
    public double getOriginalPrice() {
        return item.getPrice();
    }
    
    /**
     * Checks if this item has an active sale.
     */
    public boolean hasActiveSale() {
        SalePriceHelper.SalePriceResult priceResult = SalePriceHelper.calculateSalePrice(item);
        return priceResult.hasActiveSale();
    }

}