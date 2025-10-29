package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.function.Function;

public abstract class BaseManager {
    protected final SessionFactory sessionFactory;

    protected BaseManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /** Run a read-only unit of work (no explicit tx). */
    protected <T> T read(Function<Session, T> work) {
        try (Session s = sessionFactory.openSession()) {
            return work.apply(s);
        }
    }

    /** Run a write unit of work inside a transaction. */
    protected <T> T write(Function<Session, T> work) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                T out = work.apply(s);
                tx.commit();
                return out;
            } catch (RuntimeException ex) {
                if (tx != null && tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }
}
