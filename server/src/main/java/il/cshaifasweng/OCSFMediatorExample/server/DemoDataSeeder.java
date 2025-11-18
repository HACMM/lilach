package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.Branch;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import il.cshaifasweng.OCSFMediatorExample.entities.BranchInventory;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.BranchManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ItemManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.UserAccountManager;
import org.hibernate.SessionFactory;

import java.util.ArrayList;
import java.util.List;

public class DemoDataSeeder {

    public static void seed(SessionFactory sf) {
        ItemManager itemManager         = new ItemManager(sf);
        BranchManager branchManager     = new BranchManager(sf);
        UserAccountManager userManager  = new UserAccountManager(sf);

        // 1. Seed base data (idempotent)
        itemManager.AddTestItems();
        branchManager.AddTestBranches();
        userManager.addDefaultManager();

        // 2. Seed branch inventory if empty
        List<Branch> branches = branchManager.listAll();
        List<Item> items      = itemManager.GetItemList(new ArrayList<>());

        System.out.println("DemoDataSeeder: branches=" + branches.size() + ", items=" + items.size());

        if (branches.isEmpty() || items.isEmpty()) {
            System.out.println("DemoDataSeeder: skipping inventory seeding (no branches or no items).");
            return;
        }

        // Check inventory of first branch as a proxy
        Branch first = branches.get(0);
        List<BranchInventory> invFirst = branchManager.listInventory(first.getId());

        if (!invFirst.isEmpty()) {
            System.out.println("DemoDataSeeder: inventory already exists, skipping inventory seeding.");
            return;
        }

        System.out.println("DemoDataSeeder: seeding inventory for all branches...");
        for (Branch b : branches) {
            for (Item it : items) {
                branchManager.setInventoryAmount(b.getId(), it.getId(), 20);
            }
        }
        System.out.println("DemoDataSeeder: inventory seeding done.");
    }
}
