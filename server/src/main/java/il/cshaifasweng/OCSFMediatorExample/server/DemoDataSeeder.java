package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
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
        seedCategories(sf);
        seedItemCategoryRelations(sf);

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

    private static Category detectCategoryForItem(Item item, List<Category> categories) {
        String type = item.getType().toLowerCase();
        String name = item.getName().toLowerCase();

        Category flowers = categories.stream().filter(c -> c.getName().equals("Flowers")).findFirst().orElse(null);
        Category bouquets = categories.stream().filter(c -> c.getName().equals("Bouquets")).findFirst().orElse(null);
        Category bridal = categories.stream().filter(c -> c.getName().equals("Bridal")).findFirst().orElse(null);
        Category plants = categories.stream().filter(c -> c.getName().equals("Plants")).findFirst().orElse(null);

        // --- Bridal FIRST: must be equals only ---
        if (type.equals("bridal bouquet") || type.equals("bridal") ||
                name.contains("bridal bouquet")) {
            return bridal;
        }

        // --- Bouquet ---
        if (type.equals("bouquet")) {
            return bouquets;
        }

        // --- Plants ---
        if (type.equals("plant")) {
            return plants;
        }

        // --- Flowers (default) ---
        if (type.equals("flower")) {
            return flowers;
        }

        // --- Safety fallback ---
        return flowers;
    }


    private static void seedCategories(SessionFactory sf) {
        try (var session = sf.openSession()) {
            var tx = session.beginTransaction();

            Long count = (Long) session.createQuery("SELECT COUNT(c) FROM Category c").uniqueResult();
            if (count > 0) {
                System.out.println("Categories already exist, skipping initialization.");
                tx.commit();
                return;
            }

            System.out.println("Seeding categories...");

            session.save(new Category("Flowers", "All types of flowers"));
            session.save(new Category("Bouquets", "Ready-made bouquets"));
            session.save(new Category("Bridal", "Bridal bouquets and arrangements"));
            session.save(new Category("Plants", "Indoor and outdoor plants"));

            tx.commit();
            System.out.println("Categories seeded successfully.");
        }
    }

    private static void seedItemCategoryRelations(SessionFactory sf) {
        try (var session = sf.openSession()) {
            var tx = session.beginTransaction();

            Long count = (Long) session.createQuery("SELECT COUNT(ic) FROM ItemCategory ic").uniqueResult();
            if (count > 0) {
                System.out.println("ItemCategory relations already exist, skipping.");
                tx.commit();
                return;
            }

            System.out.println("Seeding ItemCategory relations based on item TYPE...");

            List<Item> items = session.createQuery("FROM Item", Item.class).list();
            List<Category> categories = session.createQuery("FROM Category", Category.class).list();

            if (categories.isEmpty()) {
                System.out.println("No categories found! Cannot seed relations.");
                tx.commit();
                return;
            }

            for (Item item : items) {

                Category detected = detectCategoryForItem(item, categories);
                if (detected == null) {
                    System.out.println("❌ No detected category for " + item.getName());
                    continue;
                }

                ItemCategory ic = new ItemCategory();
                ic.setItem(item);
                ic.setCategory(detected);

                session.save(ic);

                System.out.println("✔ Assigned item '" + item.getName() +
                        "' → category '" + detected.getName() + "'");
            }

            tx.commit();
            System.out.println("ItemCategory relations created successfully.");
        }
    }


}
