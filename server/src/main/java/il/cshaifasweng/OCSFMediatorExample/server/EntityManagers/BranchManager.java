package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.BranchInventory;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import org.hibernate.SessionFactory;

import java.util.List;

public class BranchManager extends BaseManager {

    public BranchManager(SessionFactory sf) { super(sf); }

    /** List all branches. */
    public List<Branch> listAll() {
        return read(s -> s.createQuery("from Branch", Branch.class).getResultList());
    }

    /** Get branch by id. */
    public Branch getById(int id) {
        return read(s -> s.get(Branch.class, id));
    }

    /** List inventory rows for a branch. */
    public List<BranchInventory> listInventory(int branchId) {
        return read(s -> s.createQuery(
                        "select bi from BranchInventory bi where bi.primaryKey.branch.id = :b",
                        BranchInventory.class)
                .setParameter("b", branchId)
                .getResultList());
    }

    /** Set absolute amount for (branch,item). Creates row if missing. */
    public BranchInventory setInventoryAmount(int branchId, int itemId, int amount) {
        return write(s -> {
            Branch branch = s.get(Branch.class, branchId);
            Item item = s.get(Item.class, itemId);
            if (branch == null) throw new IllegalArgumentException("Branch id=" + branchId + " not found");
            if (item == null) throw new IllegalArgumentException("Item id=" + itemId + " not found");

            // Try to find an existing row
            BranchInventory row = s.createQuery(
                            "select bi from BranchInventory bi " +
                                    "where bi.primaryKey.branch.id = :b and bi.primaryKey.item.id = :i",
                            BranchInventory.class)
                    .setParameter("b", branchId)
                    .setParameter("i", itemId)
                    .uniqueResult();

            if (row == null) {
                row = new BranchInventory();
                row.setBranch(branch);
                row.setItem(item);
            }

            row.setAmount(amount);
            s.merge(row);
            return row;
        });
    }

    /** Add/subtract amount (negative deltas subtract). Throws if result would be < 0. */
    public BranchInventory changeInventoryAmount(int branchId, int itemId, int delta) {
        return write(s -> {
            BranchInventory row = s.createQuery(
                            "select bi from BranchInventory bi " +
                                    "where bi.primaryKey.branch.id = :b and bi.primaryKey.item.id = :i",
                            BranchInventory.class)
                    .setParameter("b", branchId)
                    .setParameter("i", itemId)
                    .uniqueResult();

            if (row == null) {
                if (delta < 0) throw new IllegalStateException("No inventory row to decrement");
                // create new row if increasing from zero
                Branch branch = s.get(Branch.class, branchId);
                Item item = s.get(Item.class, itemId);
                if (branch == null) throw new IllegalArgumentException("Branch id=" + branchId + " not found");
                if (item == null) throw new IllegalArgumentException("Item id=" + itemId + " not found");
                row = new BranchInventory();
                row.setBranch(branch);
                row.setItem(item);
                row.setAmount(0);
            }

            int newAmount = row.getAmount() + delta;
            if (newAmount < 0) throw new IllegalStateException("Insufficient stock");
            row.setAmount(newAmount);
            s.merge(row);
            return row;
        });
    }

    /** Initialize test branches if database is empty. */
    public void AddTestBranches() {
        List<Branch> existing = listAll();
        if (!existing.isEmpty()) {
            System.out.println("Branches already exist, skipping initialization");
            return;
        }

        write(s -> {
            Branch branch1 = new Branch();
            branch1.setName("Tel Aviv Branch");
            branch1.setDescription("Our main branch in the heart of Tel Aviv");
            branch1.setSchedule("Sunday-Thursday: 9:00-20:00, Friday: 9:00-14:00");
            s.persist(branch1);

            Branch branch2 = new Branch();
            branch2.setName("Jerusalem Branch");
            branch2.setDescription("Located in the beautiful city of Jerusalem");
            branch2.setSchedule("Sunday-Thursday: 9:00-19:00, Friday: 9:00-13:00");
            s.persist(branch2);

            Branch branch3 = new Branch();
            branch3.setName("Haifa Branch");
            branch3.setDescription("Serving the northern region");
            branch3.setSchedule("Sunday-Thursday: 8:00-19:00, Friday: 8:00-14:00");
            s.persist(branch3);

            Branch branch4 = new Branch();
            branch4.setName("Beer Sheva Branch");
            branch4.setDescription("Our southern branch");
            branch4.setSchedule("Sunday-Thursday: 9:00-18:00, Friday: 9:00-13:00");
            s.persist(branch4);

            System.out.println("Initialized 4 test branches");
            return null;
        });
    }
}
