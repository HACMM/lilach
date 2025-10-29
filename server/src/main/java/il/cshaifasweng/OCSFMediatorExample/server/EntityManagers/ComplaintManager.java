package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Complaint;
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
}
