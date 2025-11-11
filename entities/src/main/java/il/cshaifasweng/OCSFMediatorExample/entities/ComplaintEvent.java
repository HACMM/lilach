package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_event")
public class ComplaintEvent{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_event_id", nullable = false, unique = true)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ComplaintStatus status;

    @Column(name = "manager_commentary") private String managerCommentary;
    @Column(name = "client_commentary") private String clientCommentary;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Complaint complaint;

    @ManyToOne(optional = true)
    private UserAccount actor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    protected ComplaintEvent() {}

    public int getId() { return id; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getManagerCommentary() { return managerCommentary; }
    public void setManagerCommentary(String managerCommentary) { this.managerCommentary = managerCommentary; }

    public String getClientCommentary() { return clientCommentary; }
    public void setClientCommentary(String clientCommentary) { this.clientCommentary = clientCommentary; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public UserAccount getActor() { return actor; }
    public void setActor(UserAccount actor) { this.actor = actor; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
