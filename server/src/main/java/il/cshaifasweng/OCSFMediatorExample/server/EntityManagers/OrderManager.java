package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Order;
import org.hibernate.SessionFactory;

import java.util.List;

public class OrderManager extends BaseManager {

    public OrderManager(SessionFactory sf) { super(sf); }

    public Order getById(int id) {
        return read(s -> s.get(Order.class, id));
    }

    public Order create(Order o) {
        return write(s -> { s.persist(o); s.flush(); return o; });
    }

    public Order update(Order o) {
        return write(s -> (Order) s.merge(o));
    }

    /** List orders by UserAccount (NOT 'user'). */
    public List<Order> listByUserAccount(int userAccountId) {
        return read(s -> s.createQuery(
                        "select o from Order o where o.userAccount.id = :u",
                        Order.class
                )
                .setParameter("u", userAccountId)
                .getResultList());
    }

    /** List orders by Branch. */
    public List<Order> listByBranch(int branchId) {
        return read(s -> s.createQuery(
                        "select o from Order o where o.branch.id = :b",
                        Order.class
                )
                .setParameter("b", branchId)
                .getResultList());
    }

    /** Optional: list all orders. */
    public List<Order> listAll() {
        return read(s -> s.createQuery("from Order", Order.class).getResultList());
    }
}
