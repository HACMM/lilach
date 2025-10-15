package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;   // <-- IMPORTANT
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Optional;

/**
 * Singleton cart manager aligned with the existing codebase:
 * - Uses entities.Item (NOT client.CatalogItem)
 * - ObservableList<CartItem> for CartView table
 * - Read-only properties for bindings
 * - Compatibility methods used by CartViewController: items(), total(), remove(CartItem)
 */
public final class CartService {

    /* ---------- Singleton ---------- */
    private static final CartService INSTANCE = new CartService();
    public static CartService get() { return INSTANCE; }

    private CartService() {
        lines.addListener((ListChangeListener<CartItem>) c -> recalc());
    }

    /* ---------- State ---------- */
    private final ObservableList<CartItem> lines = FXCollections.observableArrayList();
    private final ReadOnlyIntegerWrapper itemsCount = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyDoubleWrapper  total      = new ReadOnlyDoubleWrapper(0.0);

    /* ---------- Primary API ---------- */
    /** Bind your TableView to this list. */
    public ObservableList<CartItem> getLines() { return lines; }

    /** Bindable read-only properties. */
    public ReadOnlyIntegerProperty itemsCountProperty() { return itemsCount.getReadOnlyProperty(); }
    public ReadOnlyDoubleProperty  totalProperty()      { return total.getReadOnlyProperty(); }

    /** Convenience getters. */
    public int getItemsCount() { return itemsCount.get(); }
    public double getTotal()   { return total.get(); }

    /** Add one unit of the given Item. */
    public void addOne(Item item) {
        Optional<CartItem> line = findLine(item);
        if (line.isPresent()) {
            line.get().setQty(line.get().getQty() + 1);
        } else {
            lines.add(new CartItem(item, 1));   // CartItem(Item, int)
        }
        recalc();
    }

    /** Remove one unit; if qty becomes zero, remove the line. */
    public void removeOne(Item item) {
        findLine(item).ifPresent(l -> {
            if (l.getQty() > 1) l.setQty(l.getQty() - 1);
            else lines.remove(l);
            recalc();
        });
    }

    /** Set exact quantity; 0 removes the line. */
    public void setQty(Item item, int qty) {
        if (qty < 0) qty = 0;
        Optional<CartItem> line = findLine(item);
        if (qty == 0) {
            line.ifPresent(lines::remove);
        } else if (line.isPresent()) {
            line.get().setQty(qty);
        } else {
            lines.add(new CartItem(item, qty));
        }
        recalc();
    }

    /** Clear the entire cart. */
    public void clear() {
        lines.clear();
        recalc();
    }

    /* ---------- Compatibility with your CartViewController ---------- */
    public ObservableList<CartItem> items() { return lines; }          // used by controller
    public double total() { return getTotal(); }                        // used by controller
    public void remove(CartItem ci) { if (ci != null) removeOne(ci.getItem()); } // used by controller

    /* ---------- Helpers ---------- */
    private Optional<CartItem> findLine(Item item) {
        return lines.stream().filter(l -> l.getItem().equals(item)).findFirst();
    }

    private void recalc() {
        int count = lines.stream().mapToInt(CartItem::getQty).sum();
        double sum = lines.stream().mapToDouble(CartItem::getSubtotal).sum();
        itemsCount.set(count);
        total.set(sum);
}
}
