package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "order_lines")
public class OrderLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Back-reference to Order. Field name MUST be "order" to match mappedBy in Order */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Snapshot unit price at time of ordering */
    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    protected OrderLine() {}  // JPA

    public OrderLine(Order order, Item item, int quantity, double unitPrice) {
        this.order = order;
        this.item = item;
        this.quantity = Math.max(1, quantity);
        this.unitPrice = unitPrice;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    // Getters / setters
    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = Math.max(1, quantity); }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
