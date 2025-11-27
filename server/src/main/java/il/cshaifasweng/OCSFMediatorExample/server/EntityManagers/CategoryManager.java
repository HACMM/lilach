package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import il.cshaifasweng.OCSFMediatorExample.entities.Category;
import org.hibernate.SessionFactory;

import java.util.List;

public class CategoryManager extends BaseManager {

    public CategoryManager(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Category getById(int id) {
        return read(session -> session.get(Category.class, id));
    }

    public List<Category> listAll() {
        return read(session ->
                session.createQuery("SELECT c FROM Category c", Category.class)
                        .list()
        );
    }

    public void createDefaultCategories() {
        write(session -> {
            // Check if categories already exist
            Long count = session.createQuery(
                    "SELECT COUNT(c) FROM Category c", Long.class
            ).getSingleResult();

            if (count > 0) {
                System.out.println("Categories already exist, skipping initialization.");
                return null;
            }

            System.out.println("Initializing default categories...");

            Category c1 = new Category("Flowers", "All types of flowers");
            Category c2 = new Category("Plants", "Indoor and outdoor decorative plants");
            Category c3 = new Category("Bouquets", "Gift and celebration bouquets");
            Category c4 = new Category("Bridal Bouquets", "Wedding and bridal bouquets");

            session.persist(c1);
            session.persist(c2);
            session.persist(c3);
            session.persist(c4);

            System.out.println("Default categories created successfully.");
            return null;
        });
    }

}
