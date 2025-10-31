package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "complaint_event")
public class ComplaintEvent{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_event_id", nullable = false, unique = true)
    private int id;

    @Enumerated(EnumType.STRING) private ComplaintStatus status;

    @Column(name = "manager_commentary") private String menager_commentary;
    @Column(name = "client_commentary") private String client_commentary;

    @ManyToOne(optional = false)
    private Complaint complaint;

    protected ComplaintEvent() {}

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }
}
