package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ItemManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.BranchManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.UserAccountManager;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import java.util.Iterator;

public class DbConnector implements AutoCloseable {
    private static DbConnector instance;
    private static SessionFactory sessionFactory;

    private DbConnector() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().build();
        try {
            MetadataSources sources = new MetadataSources(registry)
                    // ONLY @Entity classes here
                    .addAnnotatedClass(Item.class)
                    .addAnnotatedClass(ComplaintEvent.class)
                    .addAnnotatedClass(Complaint.class)
                    .addAnnotatedClass(Order.class)
                    .addAnnotatedClass(OrderLine.class)
                    .addAnnotatedClass(UserAccount.class)
                    .addAnnotatedClass(Sale.class)
                    .addAnnotatedClass(ItemSale.class)
                    .addAnnotatedClass(Branch.class)
                    .addAnnotatedClass(BranchInventory.class)
                    .addAnnotatedClass(ItemCategory.class)
                    .addAnnotatedClass(Category.class)
                    .addAnnotatedClass(CustomItem.class);

            Metadata metadata = sources.buildMetadata();

            sessionFactory = metadata.buildSessionFactory();

            DemoDataSeeder.seed(sessionFactory);

        } catch (Throwable t) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw t;
        }
    }

    public static synchronized DbConnector getInstance() {
        if (instance == null) instance = new DbConnector();
        return instance;
    }

    public SessionFactory getSessionFactory() { return sessionFactory; }

    @Override public void close() {
        if (sessionFactory != null) sessionFactory.close();
    }
}
