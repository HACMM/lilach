package il.cshaifasweng.OCSFMediatorExample.entities;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(name = "Order")
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, unique = true)
    private int id;
    @Column(name = "status") private String status;

    @Column(name = "client_id") private String clientId;

    @ManyToMany
    @JoinTable(
            name = "items_in_order",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )

    private Set<Item> itemSet = new HashSet<>();

    @Embedded
    private PaymentMethod paymentMethod;

    protected Order() {

    }

    // Using client default payment method
    public Order(String ClientId, Set<Item> itemSet) {
        this.clientId = ClientId;
        this.itemSet = itemSet;
        this.setPaymentMethod(clientId);
    }

    // Using payment method other than users default payment method
    public Order(String ClientId, Set<Item> itemSet, PaymentMethod paymentMethod) {
        this.clientId = ClientId;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentMethod(String clientId) {
        // TODO: implement using UserAccountManager
    }
}
