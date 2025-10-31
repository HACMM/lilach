package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import org.hibernate.SessionFactory;
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
