package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Sale;
import il.cshaifasweng.OCSFMediatorExample.entities.SaleStatus;
import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class SaleManager extends BaseManager {

    public SaleManager(SessionFactory sf) { super(sf); }

    public Sale getById(int id) {
        return read(s -> s.get(Sale.class, id));
    }

    public List<Sale> listAll() {
        return read(s -> s.createQuery("from Sale", Sale.class).getResultList());
    }

    /** Active at a specific instant (inclusive). */
    public List<Sale> listActiveAt(Date instant) {
        Objects.requireNonNull(instant, "instant is null");
        String hql = "select s from Sale s where s.startDate <= :t and s.endDate >= :t";
        return read(s -> s.createQuery(hql, Sale.class)
                .setParameter("t", instant)
                .getResultList());
    }

    /** Active now. */
    public List<Sale> listActiveNow() {
        return listActiveAt(new Date());
    }

    public List<Sale> listActiveNowVisible() {
        List<Sale> active = listActiveNow();
        List<Sale> result = new ArrayList<>();
        for (Sale s : active) {
            if (s.getStatus() != SaleStatus.Stashed) {
                result.add(s);
            }
        }
        return result;
    }

    /** Any sale that overlaps [from, to] (inclusive). */
    public List<Sale> listOverlapping(Date from, Date to) {
        Objects.requireNonNull(from, "from is null");
        Objects.requireNonNull(to, "to is null");
        // overlap if start <= to AND end >= from
        String hql = "select s from Sale s where s.startDate <= :to and s.endDate >= :from";
        return read(s -> s.createQuery(hql, Sale.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList());
    }

    public Sale create(Sale sale) {
        Objects.requireNonNull(sale, "sale is null");
        return write(s -> { s.persist(sale); s.flush(); return sale; });
    }

    public Sale update(Sale sale) {
        Objects.requireNonNull(sale, "sale is null");
        return write(s -> (Sale) s.merge(sale));
    }

    public void delete(int id) {
        write(s -> { Sale sale = s.get(Sale.class, id); if (sale != null) s.remove(sale); return null; });
    }

    public void stashSale(int id) {
        write(s -> {
            Sale sale = s.get(Sale.class, id);
            if (sale == null) {
                System.out.println("stashSale: sale " + id + " not found");
                return null;
            }

            System.out.println("stashSale: current status = " + sale.getStatus());
            sale.setStatus(SaleStatus.Stashed);
            System.out.println("stashSale: new status = " + sale.getStatus());

            return null;
        });
    }

}
