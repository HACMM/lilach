package il.cshaifasweng.OCSFMediatorExample.entities;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(name = "ItemOrder")
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, unique = true)
    private int id;
    @Column(name = "status") private String status;


    @ManyToOne(optional = false)
    private UserAccount userAccount;

    @ManyToMany
    @JoinTable(
            name = "items_in_order",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private Set<Item> itemSet = new HashSet<>();

    @Embedded
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "order")
    private Collection<Complaint> complaints = new HashSet<>();

    @ManyToOne(optional = false)
    private Branch branch;

    @Column(length = 50)
    private String greeting;                        // ברכה להזמנה

    @Column(name = "delivery_type")
    private String deliveryType;                    // "Pickup" או "Delivery"

    @Embedded
    private Address deliveryAddress;                // כתובת משלוח (עיר, רחוב, בניין)

    @Column(name = "recipient_name")
    private String recipientName;                   // שם המקבלת

    @Column(name = "recipient_phone")
    private String recipientPhone;                  // טלפון של המקבלת

    @Column(name = "delivery_datetime")
    private LocalDateTime deliveryDateTime;         // תאריך ושעת המשלוח

    @Column(name = "delivery_fee")
    private double deliveryFee;                     // מחיר קבוע למשלוח

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "total_price")
    private double totalPrice; // סה״כ מחיר ההזמנה (כולל משלוח)


    protected Order() {

    }

    // Using client default payment method
    public Order(UserAccount userAccount, Set<Item> itemSet) {
        this.userAccount = userAccount;
        this.itemSet = itemSet;
        this.useDefaultPaymentMethod();
    }

    // Using payment method other than users default payment method
    public Order(UserAccount userAccount, Set<Item> itemSet, PaymentMethod paymentMethod) {
        this.userAccount = userAccount;
        this.itemSet = itemSet;
        this.paymentMethod = paymentMethod;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<Item> getItemSet() {
        return itemSet;
    }

    public void setItemSet(Set<Item> items) {
        this.itemSet = items;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void useDefaultPaymentMethod() {
        this.paymentMethod = this.userAccount.getDefaultPaymentMethod();
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public Collection<Complaint> getComplaints() {
        return complaints;
    }

    public void setComplaints(Collection<Complaint> complaints) {
        this.complaints = complaints;
    }

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

    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

}
