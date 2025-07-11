package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.Filter;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    private final SessionFactory sessionFactory;


    public ItemManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    public boolean AddTestItems()
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
        flower.setImageLink("magnolia.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Common sunflower");
        flower.setDescription("The common sunflower (Helianthus annuus) is a species of large annual forb of the daisy family Asteraceae.");
        flower.setType("Flower");
        flower.setPrice(7.50);
        flower.setImageLink("sunflower.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Rose");
        flower.setDescription("A rose is either a woody perennial flowering plant of the genus Rosa in the family Rosaceae or the flower it bears. ");
        flower.setType("Flower");
        flower.setPrice(20.00);
        flower.setImageLink("rose.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Daisy");
        flower.setDescription("Bellis perennis, the daisy, is a European species of the family Asteraceae, often considered the archetypal species of the name daisy.");
        flower.setType("Flower");
        flower.setPrice(5.80);
        flower.setImageLink("daisy.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Poppy");
        flower.setDescription("A poppy is a flowering plant in the subfamily Papaveroideae of the family Papaveraceae. Poppies are herbaceous plants, often grown for their colourful flowers.");
        flower.setType("Flower");
        flower.setPrice(10.50);
        flower.setImageLink("poppy.jpg");
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
