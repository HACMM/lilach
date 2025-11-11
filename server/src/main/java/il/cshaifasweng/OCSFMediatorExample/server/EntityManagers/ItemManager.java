package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import Request.Filter;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private final SessionFactory sessionFactory;

    public ItemManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public boolean AddTestItems() {
        List<Item> catalog = GetItemList(new ArrayList<>());
        System.out.println("AddTestItems: Current catalog size = " + catalog.size());
        if (catalog.size() >= 5) {
            System.out.println("AddTestItems: Catalog already has 5+ items, skipping initialization");
            return true;
        }

        System.out.println("AddTestItems: Initializing test items...");
        ArrayList<Item> toAdd = new ArrayList<>();

        toAdd.add(createItem(
                "Magnolia grandiflora",
                "Flower",
                "Magnolia grandiflora, commonly known as the southern magnolia or bull bay, is a tree native to the Southeastern United States.",
                12.50,
                "/images/magnolia.jpg",
                "White",
                "Tree Flower"
        ));

        toAdd.add(createItem(
                "Common Sunflower",
                "Flower",
                "The common sunflower (Helianthus annuus) is a species of large annual forb of the daisy family Asteraceae.",
                7.50,
                "/images/sunflower.jpg",
                "Yellow",
                "Annual"
        ));

        toAdd.add(createItem(
                "Rose",
                "Flower",
                "A rose is a woody perennial flowering plant of the genus Rosa in the family Rosaceae.",
                20.00,
                "/images/rose.jpg",
                "Red",
                "Shrub"
        ));

        toAdd.add(createItem(
                "Daisy",
                "Flower",
                "Bellis perennis, the daisy, is a European species of the family Asteraceae, often considered the archetypal species of the name daisy.",
                5.80,
                "/images/daisy.jpg",
                "White",
                "Perennial"
        ));

        toAdd.add(createItem(
                "Poppy",
                "Flower",
                "A poppy is a flowering plant in the subfamily Papaveroideae of the family Papaveraceae.",
                10.50,
                "/images/poppy.jpg",
                "Red",
                "Wildflower"
        ));

        boolean success = AddItem(toAdd);
        if (success) {
            System.out.println("AddTestItems: Successfully added " + toAdd.size() + " test items");
        } else {
            System.err.println("AddTestItems: Failed to add test items!");
        }
        return success;
    }

    /** טוען תמונה מקובץ resources לתוך byte[] */
    private byte[] loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            return is.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** יוצר אובייקט Item חדש עם נתונים ותמונה */
    private Item createItem(String name, String type, String description,
                            double price, String imagePath, String color, String flowerType) {
        byte[] imageData = loadImage(imagePath);
        return new Item(name, type, description, price, imageData, color);
    }

    public boolean AddItem(Item item) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(item);
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean AddItem(ArrayList<Item> items) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            for (Item i : items) {
                session.persist(i);
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean EditItem(Item editedItem) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Item i = session.get(Item.class, editedItem.getId());
            if (i != null) {
                i.setName(editedItem.getName());
                i.setDescription(editedItem.getDescription());
                i.setType(editedItem.getType());
                i.setPrice(editedItem.getPrice());
                i.setColor(editedItem.getColor());
                i.setFlowerType(editedItem.getFlowerType());
                i.setImageData(editedItem.getImageData());
                session.update(i);
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean RemoveItem(Item itemToRemove) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            Item persistentItem = session.get(Item.class, itemToRemove.getId());
            if (persistentItem != null) {
                session.delete(persistentItem);
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean RemoveItem(ArrayList<Item> itemListToRemove) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            for (Item item : itemListToRemove) {
                Item persistentItem = session.get(Item.class, item.getId());
                if (persistentItem != null) {
                    session.delete(persistentItem);
                }
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Item GetItem(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Item.class, id);
        }
    }

    public ArrayList<Item> GetItem(ArrayList<Integer> idList) {
        ArrayList<Item> result = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            for (int id : idList) {
                Item item = session.get(Item.class, id);
                if (item != null) result.add(item);
            }
            tx.commit();
        }
        return result;
    }

    public List<Item> GetItemList(List<Filter> filterList) {
        List<Item> result = new ArrayList<>();
        try (Session session = sessionFactory.openSession()) {
            result = session.createQuery("FROM Item", Item.class).list();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
