package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import Request.SignupResult;
import il.cshaifasweng.OCSFMediatorExample.entities.Role;
import il.cshaifasweng.OCSFMediatorExample.entities.UserAccount;
import il.cshaifasweng.OCSFMediatorExample.entities.UserBranchType;
import il.cshaifasweng.OCSFMediatorExample.entities.PaymentMethod;
import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import Request.SignupRequest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.util.List;

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
        write(session -> {
            Long count = session.createQuery(
                            "select count(u) from UserAccount u where u.login = :login",
                            Long.class)
                    .setParameter("login", "manager")
                    .uniqueResult();

            if (count == null || count == 0) {
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
                System.out.println("✅ Default manager created: " + manager.getEmail());
            } else {
                System.out.println("✅ Default manager already exists, skipping creation");
            }

            return null; // required because write() returns T
        });
    }


    public SignupResult signup(SignupRequest req) {
        return write(session -> {
            // 1) Check if username is already taken
            Long cnt = session.createQuery(
                            "select count(u) from UserAccount u where u.login = :login",
                            Long.class
                    )
                    .setParameter("login", req.getUsername())
                    .uniqueResult();

            if (cnt != null && cnt > 0) {
                // someone already uses this login
                return SignupResult.usernameTaken();
            }

            // 2) Create and save the user
            UserAccount ua = new UserAccount(
                    req.getUsername(),
                    req.getPassword(),
                    req.getName(),
                    req.getEmail(),
                    req.getPayment(),     // PaymentMethod
                    req.getBranchType()   // UserBranchType
            );

            if (req.getIdNumber() != null && !req.getIdNumber().isEmpty()) {
                ua.setIdNumber(req.getIdNumber());
            }

            session.save(ua);  // or session.persist(ua);
            System.out.println(
                    "User created: " + ua.getLogin() +
                            " with payment method: " +
                            (ua.getDefaultPaymentMethod() != null ? "saved" : "none")
            );

            return SignupResult.ok();
        });
    }


    public UserAccount findByUsername(String username) {
        return findByLogin(username);
    }

    public UserAccount create(UserAccount ua) {
        return write(s -> {

            // Reattach Branch if present (avoid detached entity error)
            if (ua.getBranch() != null) {
                int branchId = ua.getBranch().getId();
                Branch managedBranch = s.get(Branch.class, branchId);
                ua.setBranch(managedBranch);
            }

            s.persist(ua);
            s.flush();
            return ua;
        });
    }

    public void deleteById(int userId) {
        write(session -> {
            UserAccount user = session.get(UserAccount.class, userId);
            if (user == null) {
                System.out.println("User with ID " + userId + " not found, nothing to delete");
                return null;
            }

            // check if this user has any orders
            Long ordersCount = session.createQuery(
                            "select count(o) from Order o where o.userAccount.userId = :uid",
                            Long.class
                    )
                    .setParameter("uid", userId)
                    .uniqueResult();

            if (ordersCount != null && ordersCount > 0) {
                // don’t even try to delete
                throw new IllegalStateException(
                        "Cannot delete customer: they have " + ordersCount + " order(s)."
                );
            }

            System.out.println("Deleting user with ID " + userId + " (" + user.getLogin() + ")");
            session.delete(user);
            return null;
        });
    }



    public void delete(UserAccount ua) {
        if (ua == null) return;
        deleteById(ua.getUserId());
    }



    public UserAccount update(UserAccount ua) {
        return write(s -> (UserAccount) s.merge(ua));
    }

    public UserAccount updateDetails(int userId, String name, String email, String idNumber) {
        return write(session -> {
            UserAccount user = session.get(UserAccount.class, userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            user.setName(name);
            user.setEmail(email);
            user.setIdNumber(idNumber);

            session.merge(user); // make sure changes are saved
            System.out.println("User details updated successfully for user: " + user.getLogin());
            return user;
        });
    }

    public UserAccount purchaseSubscription(int userId, PaymentMethod paymentMethod) {
        return write(session -> {
            UserAccount user = session.get(UserAccount.class, userId);
            if (user == null) {
                throw new RuntimeException("User with ID " + userId + " not found");
            }

            // Save payment method
            user.setDefaultPaymentMethod(paymentMethod);

            // Activate subscription (sets subscriptionUser=true and expiration date)
            user.activateSubscription();

            session.update(user);
            session.flush();
            return user;
        });
    }

    public UserAccount renewSubscription(int userId) {
        return write(session -> {
            UserAccount user = session.get(UserAccount.class, userId);
            if (user == null) {
                throw new RuntimeException("User with ID " + userId + " not found");
            }


            if (user.getSubscriptionExpirationDate() != null &&
                    user.getSubscriptionExpirationDate().isAfter(LocalDate.now())) {
                // Extend from current expiration date
                user.setSubscriptionExpirationDate(user.getSubscriptionExpirationDate().plusYears(1));
            } else {
                // Start new subscription from today
                user.activateSubscription();
            }

            session.update(user);
            session.flush();
            return user;
        });
    }


    public UserAccount updatePaymentMethod(int userId, PaymentMethod paymentMethod) {
        return write(session -> {
            UserAccount user = session.get(UserAccount.class, userId);
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            user.setDefaultPaymentMethod(paymentMethod);
            session.merge(user);

            System.out.println("Payment method updated successfully for user: " + user.getLogin());
            return user;
        });
    }


    public List<UserAccount> listCustomers() {
        return read(s -> s.createQuery(
                "select u from UserAccount u where u.role = :role",
                UserAccount.class
        ).setParameter("role", Role.CUSTOMER).getResultList());
    }

    public List<UserAccount> listEmployees() {
        return read(s -> s.createQuery(
                        "select u from UserAccount u " +
                                "where u.role = :r1 or u.role = :r2 or u.role = :r3",
                        UserAccount.class
                )
                .setParameter("r1", Role.EMPLOYEE)
                .setParameter("r2", Role.MANAGER)
                .setParameter("r3", Role.NETWORK_MANAGER)
                .getResultList());
    }

}
