package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "complaints")
public class Complaint implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id", nullable = false, unique = true)
    private int complaint_id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;

    /** Explicit FK column so it’s "order_id" (not a generated name). */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    /** Avoid spaces in column names. Keep the label on the UI layer. */
    @Column(name = "order_number", length = 2000)
    private String orderNumber;

    @Column(name = "client_name", nullable = false, length = 2000)
    private String clientName;

    @Column(name = "client_email", nullable = false, length = 2000)
    private String clientEmail;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** If you want cascade on history deletes when complaint is removed, add cascade = CascadeType.ALL */
    @OneToMany(mappedBy = "complaint", orphanRemoval = true)
    private Set<ComplaintEvent> complaintHistory = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id")
    private UserAccount managerAccount;

    @Column(name = "response", length = 2000)
    private String response;

    @Column(name = "compensation")
    private Double compensation;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    protected Complaint() {}

    public Complaint(Branch branch, String orderNumber, String clientName, String clientEmail, String description) {
        this.branch = branch;
        this.orderNumber = orderNumber;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // --- getters/setters ---

    public int getComplaintId() { return complaint_id; }

    public UserAccount getManagerAccount() { return managerAccount; }
    public void setManagerAccount(UserAccount managerAccount) { this.managerAccount = managerAccount; }

    public UserAccount getUserAccount() { return userAccount; }
    public void setUserAccount(UserAccount userAccount) { this.userAccount = userAccount; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<ComplaintEvent> getComplaintHistory() { return complaintHistory; }
    public void setComplaintHistory(Set<ComplaintEvent> complaintHistory) { this.complaintHistory = complaintHistory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Double getCompensation() { return compensation; }
    public void setCompensation(Double compensation) { this.compensation = compensation; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    @Override
    public String toString() {
        return "Complaint #" + complaint_id + " - " + clientName;
    }
}
