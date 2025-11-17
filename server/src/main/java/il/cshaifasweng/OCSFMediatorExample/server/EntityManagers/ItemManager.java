package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import Request.Filter;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ItemManager extends BaseManager {

    public ItemManager(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    // ---------- helpers ----------

    private byte[] loadImageFromResources(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("❌ Image NOT found in resources: " + path);
                return null;
            }
            return is.readAllBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Item createItem(String name, String type, String description,
                            double price, String imagePath, String color, String flowerType) {

        Item item = new Item();
        item.setName(name);
        item.setType(type);
        item.setDescription(description);
        item.setPrice(price);
        item.setImagePath(imagePath);
        item.setColor(color);
        item.setFlowerType(flowerType);
        return item;
    }

    // ---------- public API ----------

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

    public boolean AddItem(Item item) {
        return write(session -> {
            session.persist(item);
            return true;
        });
    }

    public boolean AddItem(ArrayList<Item> items) {
        return write(session -> {
            for (Item i : items) {
                session.persist(i);
            }
            return true;
        });
    }

    public boolean EditItem(Item editedItem) {
        return write(session -> {
            Item i = session.get(Item.class, editedItem.getId());
            if (i != null) {
                i.setName(editedItem.getName());
                i.setDescription(editedItem.getDescription());
                i.setType(editedItem.getType());
                i.setPrice(editedItem.getPrice());
                i.setColor(editedItem.getColor());
                i.setFlowerType(editedItem.getFlowerType());
                i.setImagePath(editedItem.getImagePath());
                session.update(i);
            }
            return true;
        });
    }

    public boolean RemoveItem(Item itemToRemove) {
        return write(session -> {
            Item persistentItem = session.get(Item.class, itemToRemove.getId());
            if (persistentItem != null) {
                session.delete(persistentItem);
            }
            return true;
        });
    }

    public boolean RemoveItem(ArrayList<Item> itemListToRemove) {
        return write(session -> {
            for (Item item : itemListToRemove) {
                Item persistentItem = session.get(Item.class, item.getId());
                if (persistentItem != null) {
                    session.delete(persistentItem);
                }
            }
            return true;
        });
    }

    public Item GetItem(int id) {
        return read(session -> session.get(Item.class, id));
    }

    public ArrayList<Item> GetItem(ArrayList<Integer> idList) {
        return read(session -> {
            ArrayList<Item> result = new ArrayList<>();
            for (int id : idList) {
                Item item = session.get(Item.class, id);
                if (item != null) result.add(item);
            }
            return result;
        });
    }

    public List<Item> GetItemList(List<Filter> filterList) {
        return read(session -> {
            List<Item> result = session.createQuery("FROM Item", Item.class).list();
            for (Item item : result) {
                System.out.println("Item path = " + item.getImagePath());
                if (item.getImagePath() != null) {
                    item.setImageData(loadImageFromResources(item.getImagePath()));
                    System.out.println("Loaded image for: " + item.getName()
                            + " | path=" + item.getImagePath()
                            + " | bytes=" + (item.getImageData() == null ? "NULL" : item.getImageData().length));
                }
            }
            return result;
        });
    }
}