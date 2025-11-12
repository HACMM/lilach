package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.entities.UserBranchType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class UserAccountManager extends BaseManager {

    public UserAccountManager(SessionFactory sf) { super(sf); }

    public UserAccount findByLogin(String login) {
        return read(s -> {
            Query<UserAccount> q = s.createQuery(
                    "select ua from UserAccount ua where ua.login = :u",
                    UserAccount.class
            );
            q.setParameter("u", login);
            return q.uniqueResult();
        });
    }

    public void addDefaultManager() {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            // בדיקה אם יש כבר מנהלת
            Long count = session.createQuery(
                            "select count(u) from UserAccount u where u.role = :role", Long.class)
                    .setParameter("role", Role.NETWORK_MANAGER)
                    .uniqueResult();

            if (count == 0) {
                UserAccount manager = new UserAccount(
                        "manager",
                        "1234",
                        "Lilach Manager",
                        "manager@lilach.com",
                        null,
                        UserBranchType.ALL_BRANCHES
                );
                manager.setRole(Role.MANAGER);
                manager.setIdNumber("123456789");
                session.persist(manager);
                tx.commit();
                System.out.println("✅ Default manager created: " + manager.getEmail());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UserAccount findByUsername(String username) {
        return findByLogin(username);
    }

    public UserAccount create(UserAccount ua) {
        return write(s -> { s.persist(ua); s.flush(); return ua; });
    }

    public UserAccount update(UserAccount ua) {
        return write(s -> (UserAccount) s.merge(ua));
    }
}
