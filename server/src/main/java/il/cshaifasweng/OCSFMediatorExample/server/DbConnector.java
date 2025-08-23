package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.*;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class DbConnector implements AutoCloseable {
    private static DbConnector instance;
    private static SessionFactory sessionFactory;

    private DbConnector() {
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Item.class);
        configuration.addAnnotatedClass(PaymentMethod.class);
        configuration.addAnnotatedClass(ComplaintEvent.class);
        configuration.addAnnotatedClass(Complaint.class);
        configuration.addAnnotatedClass(Order.class);
        configuration.addAnnotatedClass(UserAccount.class);
        configuration.addAnnotatedClass(Sale.class);
        configuration.addAnnotatedClass(ItemSale.class);
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();
        sessionFactory = configuration.buildSessionFactory(serviceRegistry);

        ItemManager itemManager = new ItemManager(sessionFactory);
        itemManager.AddTestItems();
    }

    public static synchronized DbConnector getInstance() {
        if (instance == null) {
            instance = new DbConnector();
        }
        return instance;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public void close() throws Exception {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

}
