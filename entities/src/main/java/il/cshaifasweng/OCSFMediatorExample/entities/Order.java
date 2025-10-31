package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ItemOrder")
public class Order implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, unique = true)
    private int id;

    @Column(name = "status")
    private String status;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;

    /** Replaces old ManyToMany with explicit order lines */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();


    @Embedded
    private PaymentMethod paymentMethod;

    /** Requires Complaint to have: @ManyToOne(fetch=LAZY) @JoinColumn(name="order_id") private Order order; */
    @OneToMany(mappedBy = "order")
    private List<Complaint> complaints = new ArrayList<>();

    /** If your schema requires a branch, keep optional=false and ensure you set it before persist */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(length = 50)
    private String greeting;

    @Column(name = "delivery_type")
    private String deliveryType;

    /** Use @Embeddable on Address class */
    @Embedded
    private Address deliveryAddress;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "delivery_datetime")
    private LocalDateTime deliveryDateTime;

    @Column(name = "delivery_fee", nullable = false)
    private double deliveryFee = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "total_price", nullable = false)
    private double totalPrice = 0.0;

    /** JPA ctor */
    protected Order() {}

    public Order(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    /** Convenience factory for a single line */
    public static Order of(UserAccount user, Item item, int qty, double unitPrice) {
        Order o = new Order(user);
        o.addLine(item, qty, unitPrice);
        return o;
    }

    /** Ensure createdAt is set */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // --- business helpers ---
    public void addLine(Item item, int qty, double unitPrice) {
        // assumes OrderLine has ctor (Order, Item, qty, unitPrice); otherwise set fields then add
        OrderLine line = new OrderLine(this, item, Math.max(1, qty), unitPrice);
        this.orderLines.add(line);
        recomputeTotal();
    }

    public void removeLine(OrderLine line) {
        this.orderLines.remove(line);
        if (line != null) line.setOrder(null);
        recomputeTotal();
    }

    /** Recompute totalPrice from lines + delivery fee. */
    public void recomputeTotal() {
        double itemsTotal = orderLines.stream()
                .mapToDouble(OrderLine::getSubtotal)
                .sum();
        this.totalPrice = itemsTotal + (Double.isNaN(deliveryFee) ? 0.0 : deliveryFee);
    }

    // --- getters/setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UserAccount getUserAccount() { return userAccount; }
    public void setUserAccount(UserAccount userAccount) { this.userAccount = userAccount; }

    public List<OrderLine> getOrderLines() { return orderLines; }
    public void setOrderLines(List<OrderLine> orderLines) { this.orderLines = orderLines != null ? orderLines : new ArrayList<>(); }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<Complaint> getComplaints() { return complaints; }
    public void setComplaints(List<Complaint> complaints) { this.complaints = complaints != null ? complaints : new ArrayList<>(); }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public Address getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(Address deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public LocalDateTime getDeliveryDateTime() { return deliveryDateTime; }
    public void setDeliveryDateTime(LocalDateTime deliveryDateTime) { this.deliveryDateTime = deliveryDateTime; }

    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; recomputeTotal(); }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
