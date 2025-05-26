package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import il.cshaifasweng.OCSFMediatorExample.entities.Filter;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.ArrayList;
import java.util.List;

public class DbConnector implements AutoCloseable {
    private static SessionFactory sessionFactory;
    public DbConnector(){
        Configuration configuration = new Configuration();
        configuration.addAnnotatedClass(Item.class);
        ServiceRegistry serviceRegistry = new
                StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();
        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    }
    @Override
    public void close() throws Exception {
        sessionFactory.close();
    }
    public boolean AddTestData()
    {
        List<Item> catalog = GetItemList(new ArrayList<>());
        if(catalog.size() >= 5)
            return true;
        
        ArrayList<Item> toAdd = new ArrayList<>();
        Item flower = new Item();
        flower.setName("Magnolia grandiflora");
        flower.setDescription("Magnolia grandiflora, commonly known as the southern magnolia or bull bay, is a tree of the family Magnoliaceae native to the Southeastern United States, from Virginia to central Florida, and west to East Texas.");
        flower.setType("Flower");
        flower.setPrice(12.50);
        flower.setImageLink("https://upload.wikimedia.org/wikipedia/commons/d/dc/Magn%C3%B2lia_a_Verbania.JPG");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Common sunflower");
        flower.setDescription("The common sunflower (Helianthus annuus) is a species of large annual forb of the daisy family Asteraceae.");
        flower.setType("Flower");
        flower.setPrice(7.50);
        flower.setImageLink("https://upload.wikimedia.org/wikipedia/commons/4/40/Sunflower_sky_backdrop.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Rose");
        flower.setDescription("A rose is either a woody perennial flowering plant of the genus Rosa in the family Rosaceae or the flower it bears. ");
        flower.setType("Flower");
        flower.setPrice(20.00);
        flower.setImageLink("https://upload.wikimedia.org/wikipedia/commons/3/3a/Rosa_Precious_platinum.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Daisy");
        flower.setDescription("Bellis perennis, the daisy, is a European species of the family Asteraceae, often considered the archetypal species of the name daisy.");
        flower.setType("Flower");
        flower.setPrice(5.80);
        flower.setImageLink("https://upload.wikimedia.org/wikipedia/commons/2/27/Bellis_perennis_001.JPG");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Poppy");
        flower.setDescription("A poppy is a flowering plant in the subfamily Papaveroideae of the family Papaveraceae. Poppies are herbaceous plants, often grown for their colourful flowers.");
        flower.setType("Flower");
        flower.setPrice(10.50);
        flower.setImageLink("https://upload.wikimedia.org/wikipedia/commons/7/71/Poppies_in_the_Sunset_on_Lake_Geneva.jpg");
        toAdd.add(flower);

        return AddItem(toAdd);
    }
    public boolean AddItem(Item item)
    {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try{
            tx = session.beginTransaction();
            session.persist(item);
            tx.commit();
        }
        catch(Exception e){
            tx.rollback();
            throw e;
        }
        finally {
            session.close();
        }
        return true;
    }
    public boolean AddItem(ArrayList<Item> item)
    {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try{
            tx = session.beginTransaction();
            for (Item toAdd : item)
                session.persist(toAdd);
            tx.commit();
        }
        catch(Exception e){
            tx.rollback();
            throw e;
        }
        finally {
            session.close();
        }
        return true;
    }
    public boolean EditItem(Item editedItem)
    {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try{
            tx = session.beginTransaction();
            Item i = session.get(Item.class, editedItem.getId());
            i.setPrice(editedItem.getPrice());
            i.setImageLink(editedItem.getImageLink());
            i.setDescription(editedItem.getDescription());
            i.setName(editedItem.getName());
            i.setType(editedItem.getType());
            session.update(i);
            tx.commit();
        }
        catch(Exception e){
            tx.rollback();
            throw e;
        }
        finally {
            session.close();
        }
        return true;
    }

    public boolean RemoveItem(Item itemToRemove)
    {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try{
            tx = session.beginTransaction();
            session.delete(itemToRemove);
            tx.commit();
        }
        catch(Exception e){
            tx.rollback();
            throw e;
        }
        finally {
            session.close();
        }
        return true;
    }
    public boolean RemoveItem(ArrayList<Item> itemListToRemove)
    {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try{
            tx = session.beginTransaction();
            for (Item toRemove : itemListToRemove)
                session.remove(toRemove);
            tx.commit();
        }
        catch(Exception e){
            tx.rollback();
            throw e;
        }
        finally {
            session.close();
        }
        return true;
    }

    public Item GetItem(int id)
    {
        Item result = null;
        try(Session session = sessionFactory.openSession()){
            Transaction tx = session.beginTransaction();
            result = session.get(Item.class, id);
            session.close();
        }
        return result;
    }
    public ArrayList<Item> GetItem(ArrayList<Integer> idList)
    {
        ArrayList<Item> result = new ArrayList<>();
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try{
            tx = session.beginTransaction();
            for (int id : idList)
                result.add(session.get(Item.class, id));
            tx.commit();
        }
        catch(Exception e){
            tx.rollback();
            throw e;
        }
        finally {
            session.close();
        }
        return result;
    }

    // filterList is not currently in use
    public List<Item> GetItemList(List<Filter> filterList)
    {
        // TODO: implement filtering
        List<Item> result = new ArrayList<>();
        Session session = sessionFactory.openSession();
        try{
            result = session.createQuery("FROM Item", Item.class).list();
           // result = (List<Item>)session.createSQLQuery("SELECT * FROM Item").list();
        }
        catch(Exception e){
            throw e;
        }
        finally {
            session.close();
        }
        return result;
    }

}
