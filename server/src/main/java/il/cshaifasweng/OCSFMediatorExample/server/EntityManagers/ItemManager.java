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

        toAdd.add(createItem(
                "Elegant White Lily Bridal Bouquet",
                "Bridal Bouquet",
                "A luxurious bridal bouquet crafted with fresh white lilies, soft greenery, and delicate baby's breath. Designed to complement any wedding dress with timeless elegance and pure sophistication.",
                149.90,
                "/images/white_lily_bridal_bouquet.jpeg",
                "White",
                "Lily Bouquet"
        ));

        toAdd.add(createItem(
                "Soft Blush Calla Lily Bridal Bouquet",
                "Bridal Bouquet",
                "A romantic bridal bouquet designed with elegant white calla lilies, soft blush mini roses, and airy pink astilbe. This bouquet creates a delicate, feminine look that complements any bridal style with grace and sophistication.",
                169.90,
                "/images/blush_calla_lily_bridal_bouquet.jpeg",
                "Pink",
                "Calla & Rose Bridal Mix"
        ));

        toAdd.add(createItem(
                "Blush Pink Calla Lily Bridal Bouquet",
                "Bridal Bouquet",
                "A refined and elegant bridal bouquet featuring premium blush pink calla lilies. Its clean, modern design offers a soft and romantic touch, making it a perfect choice for brides seeking timeless simplicity and luxury.",
                159.90,
                "/images/blush_pink_calla_lily_bouquet.jpeg",
                "Pink",
                "Calla Lily"
        ));

        toAdd.add(createItem(
                "Classic White Rose Bridal Bouquet",
                "Bridal Bouquet",
                "An elegant and timeless bridal bouquet crafted with premium white roses, delicate baby's breath, and soft pearl accents. Perfect for brides seeking a classic, pure, and sophisticated look on their special day.",
                179.90,
                "/images/classic_white_rose_bridal_bouquet.jpeg",
                "White",
                "Rose Bouquet"
        ));

        toAdd.add(createItem(
                "Pure White Calla Lily Bridal Bouquet",
                "Bridal Bouquet",
                "A sleek and elegant bridal bouquet crafted entirely from premium white calla lilies. Its modern, minimalist style creates a clean and sophisticated look, perfect for brides seeking purity and timeless elegance.",
                169.90,
                "/images/pure_white_calla_lily_bridal_bouquet.jpeg",
                "White",
                "Calla Lily"
        ));

        toAdd.add(createItem(
                "Pastel Tulip Bridal Bouquet",
                "Bridal Bouquet",
                "A dreamy bridal bouquet featuring a stunning selection of pastel tulips in soft shades of cream, peach, and lavender. This romantic and modern bouquet adds a gentle touch of elegance and color, perfect for spring weddings and brides who love a fresh, natural look.",
                169.90,
                "/images/pastel_tulip_bridal_bouquet.jpeg",
                "Peach, Cream & White",
                "Tulip"
        ));

        toAdd.add(createItem(
                "Pure White Tulip Bridal Bouquet",
                "Bridal Bouquet",
                "A clean and elegant bridal bouquet featuring fresh white tulips tied with a soft satin ribbon. Its minimalist and timeless design makes it a perfect choice for modern brides who love simplicity, purity, and classic beauty.",
                159.90,
                "/images/pure_white_tulip_bridal_bouquet.jpeg",
                "White",
                "Tulip"
        ));

        toAdd.add(createItem(
                "Trailing Pothos Plant",
                "Plant",
                "A lush and easy-care trailing pothos plant with vibrant green heart-shaped leaves. Perfect for shelves, desks, and hanging spaces, it thrives in low to medium light and adds a fresh, natural look to any room.",
                59.90,
                "/images/trailing_pothos_plant.jpeg",
                "Green",
                "Pothos"
        ));

        toAdd.add(createItem(
                "Snake Plant (Sansevieria)",
                "Plant",
                "A stylish and extremely low-maintenance snake plant with tall variegated leaves. Known for its air-purifying qualities and durability, it thrives in low light and requires minimal watering — perfect for homes, offices, and modern interior spaces.",
                79.90,
                "/images/snake_plant_sansevieria.jpeg",
                "Green with Yellow Edges",
                "Sansevieria"
        ));

        toAdd.add(createItem(
                "Monstera Deliciosa",
                "Plant",
                "A large and stylish Monstera Deliciosa with its iconic split leaves. Perfect for modern homes and offices, this tropical plant adds bold character and thrives in bright, indirect light. Easy to care for and highly decorative.",
                129.90,
                "/images/monstera_deliciosa_plant.jpeg",
                "Deep Green",
                "Monstera"
        ));

        toAdd.add(createItem(
                "Mini Blooming Cactus Arrangement",
                "Plant",
                "A charming mini cactus arrangement featuring a mix of small cacti varieties, some blooming with delicate white flowers. Low-maintenance and perfect for desks, shelves, or as a small gift, this arrangement brings a touch of natural beauty to any space.",
                39.90,
                "/images/mini_blooming_cactus_arrangement.jpeg",
                "Green",
                "Cactus Mix"
        ));

        toAdd.add(createItem(
                "Sansevieria Trifasciata (Snake Plant)",
                "Plant",
                "A classic Sansevieria Trifasciata with tall upright leaves in natural variegated green tones. This durable and air-purifying plant is perfect for both beginners and plant lovers, thriving in low light and requiring minimal care.",
                74.90,
                "/images/sansevieria_trifasciata_plant.jpeg",
                "Green",
                "Sansevieria"
        ));

        toAdd.add(createItem(
                "Philodendron Pink Princess",
                "Plant",
                "A stunning Philodendron Pink Princess with vibrant pink and deep green variegated leaves. This highly desirable tropical plant adds a bold and artistic touch to any space. Easy to grow in bright, indirect light and perfect for modern interior styling.",
                149.90,
                "/images/philodendron_pink_princess.jpeg",
                "Pink & Green",
                "Philodendron"
        ));

        toAdd.add(createItem(
                "String of Nickels (Dischidia Nummularia)",
                "Plant",
                "A beautiful trailing plant known as 'String of Nickels,' featuring round, coin-like leaves cascading elegantly from the pot. Perfect for shelves, hanging baskets, and modern interior décor. Easy-care and thrives in bright, indirect light.",
                54.90,
                "/images/string_of_nickels_plant.jpeg",
                "Green",
                "Dischidia"
        ));

        toAdd.add(createItem(
                "Philodendron White Knight",
                "Plant",
                "A rare and elegant Philodendron White Knight featuring bold variegated leaves in pure white and deep green patterns. This premium tropical plant is perfect for plant collectors and modern interiors, thriving in bright, indirect light.",
                189.90,
                "/images/philodendron_white_knight.jpeg",
                "Green & White Variegated",
                "Philodendron"
        ));

        toAdd.add(createItem(
                "Peach & Cream Romantic Bouquet",
                "Bouquet",
                "A soft and romantic bouquet featuring peach and cream roses, delicate alstroemeria, and seasonal white flowers. Perfect for birthdays, anniversaries, thank-you gifts, or any moment worth celebrating with elegance and warmth.",
                119.90,
                "/images/peach_cream_romantic_bouquet.jpeg",
                "Peach, Cream & White",
                "Rose & Alstroemeria Mix"
        ));

        toAdd.add(createItem(
                "Classic Red Rose Bouquet",
                "Bouquet",
                "A luxurious bouquet of fresh deep-red roses paired with delicate baby's breath. A timeless expression of love, passion, and elegance — perfect for birthdays, anniversaries, romantic gestures, and special celebrations.",
                149.90,
                "/images/classic_red_rose_bouquet.jpeg",
                "Red",
                "Rose & Baby’s Breath"
        ));

        toAdd.add(createItem(
                "Lavender & White Matthiola Bouquet",
                "Bouquet",
                "A fragrant and elegant bouquet made of fresh Matthiola flowers in soft lavender and pure white tones. Known for their long stems, full blossoms, and gentle scent — perfect for birthdays, celebrations, and thoughtful floral gifts.",
                109.90,
                "/images/lavender_white_matthiola_bouquet.jpeg",
                "Lavender & White",
                "Matthiola"
        ));

        toAdd.add(createItem(
                "Pink Tulip Deluxe Bouquet",
                "Bouquet",
                "A luxurious bouquet of fresh pink tulips wrapped in soft pastel paper. Elegant, modern, and perfect for celebrations, birthdays, romantic gestures, or simply brightening someone's day with beauty and charm.",
                139.90,
                "/images/pink_tulip_deluxe_bouquet.jpeg",
                "Pink",
                "Tulip"
        ));

        toAdd.add(createItem(
                "Sunny Meadow Bouquet",
                "Bouquet",
                "A bright and joyful bouquet featuring sunflowers, yellow roses, and delicate white daisies. This cheerful arrangement brings warmth, positivity, and natural beauty — perfect for birthdays, congratulations, and uplifting gifts.",
                129.90,
                "/images/sunny_meadow_bouquet.jpeg",
                "Yellow",
                "Sunflower, Rose & Daisy Mix"
        ));

        toAdd.add(createItem(
                "Pink Garden Rose Elegance Bouquet",
                "Bouquet",
                "A luxurious bouquet of premium pink garden roses beautifully combined with fresh eucalyptus leaves. Soft, elegant, and romantic — perfect for birthdays, anniversaries, thank-you gifts, or any moment that deserves a touch of refined beauty.",
                159.90,
                "/images/pink_garden_rose_elegance_bouquet.jpeg",
                "Pink",
                "Garden Rose & Eucalyptus"
        ));

        toAdd.add(createItem(
                "Pink Tulip",
                "Flower",
                "Soft and elegant pink tulips with gentle pastel tones. Perfect for creating your own bouquet, adding a romantic touch to arrangements, or enjoying them as a stand-alone flower. A classic choice for expressing love, gratitude, and gentle beauty.",
                6.90,
                "/images/pink_tulip_single.jpeg",
                "Pink",
                "Tulip"
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
                String imagePath = item.getImagePath();
                System.out.println("Item path = " + imagePath);
                
                if (imagePath != null && !imagePath.trim().isEmpty()) {
                    // Try to load from the specified path
                    byte[] imageData = loadImageFromResources(imagePath);
                    item.setImageData(imageData);
                    System.out.println("Loaded image for: " + item.getName()
                            + " | path=" + imagePath
                            + " | bytes=" + (imageData == null ? "NULL" : imageData.length));
                } else {
                    // If no path, try to load a default image based on item name/type
                    String defaultPath = getDefaultImagePath(item);
                    if (defaultPath != null) {
                        byte[] imageData = loadImageFromResources(defaultPath);
                        item.setImageData(imageData);
                        System.out.println("Loaded default image for: " + item.getName() + " (path was null, using: " + defaultPath + ")");
                    } else {
                        // Try to load a generic no-image placeholder
                        byte[] imageData = loadImageFromResources("/images/no_image.jpg");
                        item.setImageData(imageData);
                    }
                }
            }
            return result;
        });
    }

    public List<Item> GetItemListForBranch(Integer branchId) {
        return read(session -> {
            List<Item> result;

            if (branchId == null) {
                result = session.createQuery("FROM Item", Item.class).list();
            } else {
                // Only items available in the given branch
                result = session.createQuery(
                                "select distinct bi.primaryKey.item " +
                                        "from BranchInventory bi " +
                                        "where bi.primaryKey.branch.id = :bid",
                                Item.class
                        )
                        .setParameter("bid", branchId)
                        .list();
            }

            for (Item item : result) {
                String imagePath = item.getImagePath();
                System.out.println("Item path = " + imagePath);

                if (imagePath != null && !imagePath.trim().isEmpty()) {
                    byte[] imageData = loadImageFromResources(imagePath);
                    item.setImageData(imageData);
                    System.out.println("Loaded image for: " + item.getName()
                            + " | path=" + imagePath
                            + " | bytes=" + (imageData == null ? "NULL" : imageData.length));
                } else {
                    String defaultPath = getDefaultImagePath(item);
                    if (defaultPath != null) {
                        byte[] imageData = loadImageFromResources(defaultPath);
                        item.setImageData(imageData);
                        System.out.println("Loaded default image for: " + item.getName()
                                + " (path was null, using: " + defaultPath + ")");
                    } else {
                        byte[] imageData = loadImageFromResources("/images/no_image.jpg");
                        item.setImageData(imageData);
                    }
                }
            }

            return result;
        });
    }
    
    private String getDefaultImagePath(Item item) {
        // Try to infer image path from item name
        String name = item.getName().toLowerCase();
        if (name.contains("magnolia")) return "/images/magnolia.jpg";
        if (name.contains("sunflower")) return "/images/sunflower.jpg";
        if (name.contains("rose")) return "/images/rose.jpg";
        if (name.contains("daisy")) return "/images/daisy.jpg";
        if (name.contains("poppy")) return "/images/poppy.jpg";
        // Default fallback
        return "/images/no_image.jpg";
    }

    public List<String> getCategories() {
        return read(session -> {
            List<String> categories =
                    session.createQuery("SELECT DISTINCT i.type FROM Item i", String.class)
                            .list();

            System.out.println("Loaded categories: " + categories);
            return categories;
        });
    }

    public List<Item> getItemsByCategory(int categoryId) {
        try (Session session = sessionFactory.openSession()) {

            List<Item> result = session.createQuery(
                            "SELECT ic.primaryKey.item FROM ItemCategory ic " +
                                    "WHERE ic.primaryKey.category.category_id = :catId",
                            Item.class
                    )
                    .setParameter("catId", categoryId)
                    .getResultList();

            // 🔥 Load images (this was missing!)
            for (Item item : result) {
                String path = item.getImagePath();

                if (path != null && !path.trim().isEmpty()) {
                    byte[] data = loadImageFromResources(path);
                    item.setImageData(data);
                } else {
                    byte[] data = loadImageFromResources("/images/no_image.jpg");
                    item.setImageData(data);
                }
            }

            return result;
        }
    }



}