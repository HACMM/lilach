package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Branch")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id", nullable = false, unique = true)
    private int branch_id;

    @Column(name = "name") private String name;

    @Column(name = "description") private String description;

    @Column(name = "schedule") private String schedule;

    @OneToMany(mappedBy = "branch",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private Set<Order> orders = new HashSet<Order>();

    @OneToMany(mappedBy = "primaryKry.branch",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private Set<BranchInventory> branchInventory = new HashSet<BranchInventory>();

    public Branch() {
    }

    public int getId() {
        return branch_id;
    }

    public void setId(int id) {
        this.branch_id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public void setOrders(Set<Order> orders) {
        this.orders = orders;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public void removeOrder(Order order) {
        orders.remove(order);
    }
}
