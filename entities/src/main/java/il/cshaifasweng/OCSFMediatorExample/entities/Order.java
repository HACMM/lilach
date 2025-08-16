package il.cshaifasweng.OCSFMediatorExample.entities;


import java.io.Serializable;
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
}
