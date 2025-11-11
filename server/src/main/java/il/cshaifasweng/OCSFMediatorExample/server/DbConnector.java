package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.ItemManager;
import il.cshaifasweng.OCSFMediatorExample.server.EntityManagers.BranchManager;
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
                    .addAnnotatedClass(Category.class);

            Metadata metadata = sources.buildMetadata();

//            // 🔎 Diagnostic: list bound entities + their properties
//            for (PersistentClass pc : metadata.getEntityBindings()) {
//                System.out.println(">> Entity: " + pc.getClassName() +
//                        " -> table=" + (pc.getTable() != null ? pc.getTable().getName() : "<null>"));
//                for (Iterator<Property> it = pc.getPropertyIterator(); it.hasNext();) {
//                    Property p = it.next();
//                    System.out.println("   - prop: " + p.getName());
//                }
//            }

            sessionFactory = metadata.buildSessionFactory();

            new ItemManager(sessionFactory).AddTestItems();
            new BranchManager(sessionFactory).AddTestBranches();

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
