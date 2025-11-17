package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
import il.cshaifasweng.OCSFMediatorExample.entities.ComplaintStatus;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import org.hibernate.SessionFactory;

import java.util.List;

public class ComplaintManager extends BaseManager {
    public ComplaintManager(SessionFactory sessionFactory) { super(sessionFactory); }

    public Complaint submit(Complaint c) {
        return write(s -> { s.persist(c); s.flush(); return c; });
    }

    public List<Complaint> listAll() {
        return read(s -> s.createQuery("from Complaint", Complaint.class).getResultList());
    }

    public Complaint findById(int id) {
        return read(s -> s.get(Complaint.class, id));
    }

    public Complaint resolveComplaint(
            int complaintId,
            Integer managerUserId,
            String response,
            Double compensation
    ) {
        return write(s -> {
            Complaint c = s.get(Complaint.class, complaintId);
            if (c == null) {
                return null;
            }

            // Set manager account if managerUserId is provided
            if (managerUserId != null) {
                UserAccount manager = s.get(UserAccount.class, managerUserId);
                if (manager != null) {
                    c.setManagerAccount(manager);
                }
            }

            c.setResponse(response);
            c.setCompensation(compensation != null ? compensation : 0.0);
            c.setRespondedAt(java.time.LocalDateTime.now());

            // Status + event logic
            if (compensation != null && compensation > 0) {
                c.setStatus(ComplaintStatus.Approved);
                c.addEvent(ComplaintStatus.Approved, response, c.getManagerAccount());
            } else {
                c.setStatus(ComplaintStatus.Rejected);
                c.addEvent(ComplaintStatus.Rejected, response, c.getManagerAccount());
            }

            s.update(c);
            return c;
        });
    }

}
