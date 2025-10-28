package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "complaints")
public class Complaint implements Serializable {

    // Unique ID for each complaint
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id", nullable = false, unique = true)
    private int complaint_id;

    @ManyToOne(optional = false)
    private UserAccount userAccount;

//    // Category of the complaint (e.g., Service, Product Quality, Delivery Delay)
//    @Column(nullable = false)
//    private String type;

    @ManyToOne(optional = false)
    private Order order;

    // Branch
    @ManyToOne
    private Branch branch;

    // Full description of what went wrong
    @Column(name = "Order Number", nullable = true, length = 2000)
    private String orderNumber;

    // Full description of what went wrong
    @Column(name = "Client Name", nullable = false, length = 2000)
    private String clientName;

    // Full description of what went wrong
    @Column(name = "Client Email", nullable = false, length = 2000)
    private String clientEmail;

    // Full description of what went wrong
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    // Timestamp when the complaint was submitted
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "complaint", orphanRemoval = true)
    private Set<ComplaintEvent> complaintHistory = new HashSet<ComplaintEvent>();

    @ManyToOne(optional = true)
    private UserAccount managerAccount;

    @Column(name = "response", length = 2000)
    private String response;

    @Column(name = "compensation")
    private Double compensation;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // Default constructor for JPA and serialization
    protected Complaint() {
    }

    public Complaint(Branch branch, String orderNumber, String clientName, String clientEmail, String description) {
        this.branch = branch;
        this.orderNumber = orderNumber;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public int getComplaintId() {
        return complaint_id;
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
//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Double getCompensation() {
        return compensation;
    }

    public void setCompensation(Double compensation) {
        this.compensation = compensation;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    @Override
    public String toString() {
        return "Complaint #" + complaint_id + " - " + clientName;
    }

}