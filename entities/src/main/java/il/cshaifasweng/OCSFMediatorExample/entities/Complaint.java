package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Complaint")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id", nullable = false, unique = true)
    private int compalint_id;

    @ManyToOne(optional = false)
    private UserAccount userAccount;

    @ManyToOne(optional = false)
    private Order order;

    @Column(name = "description") private String description;

    @OneToMany(mappedBy = "complaint", orphanRemoval = true)
    private Set<ComplaintEvent> complaintHistory = new HashSet<ComplaintEvent>();

    @ManyToOne(optional = false)
    private UserAccount managerAccount;

    public int getCompalint_id() {
        return compalint_id;
    }
    public UserAccount getManagerAccount() {
        return managerAccount;
    }

    public void setManagerAccount(UserAccount managerAccount) {
        this.managerAccount = managerAccount;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<ComplaintEvent> getComplaintHistory() {
        return complaintHistory;
    }

    public void setComplaintHistory(Set<ComplaintEvent> complaintHistory) {
        this.complaintHistory = complaintHistory;
    }
}
