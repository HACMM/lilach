package il.cshaifasweng.OCSFMediatorExample.server.EntityManagers;

import Request.Filter;
import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemManager extends BaseManager {

    public ItemManager(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public boolean AddTestItems() {
        List<Item> catalog = GetItemList(new ArrayList<>());
        if (catalog.size() >= 5) return true;

        ArrayList<Item> toAdd = new ArrayList<>();
        Item flower;

        flower = new Item();
        flower.setName("Magnolia grandiflora");
        flower.setDescription("Magnolia grandiflora...");
        flower.setType("Flower");
        flower.setPrice(12.50);
        flower.setImageLink("magnolia.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Common sunflower");
        flower.setDescription("The common sunflower...");
        flower.setType("Flower");
        flower.setPrice(7.50);
        flower.setImageLink("sunflower.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Rose");
        flower.setDescription("A rose is either...");
        flower.setType("Flower");
        flower.setPrice(20.00);
        flower.setImageLink("rose.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Daisy");
        flower.setDescription("Bellis perennis...");
        flower.setType("Flower");
        flower.setPrice(5.80);
        flower.setImageLink("daisy.jpg");
        toAdd.add(flower);

        flower = new Item();
        flower.setName("Poppy");
        flower.setDescription("A poppy is a flowering plant...");
        flower.setType("Flower");
        flower.setPrice(10.50);
        flower.setImageLink("poppy.jpg");
        toAdd.add(flower);

        return AddItem(toAdd);
    }

    public boolean AddItem(Item item) {
        Objects.requireNonNull(item, "item is null");
        write(s -> { s.persist(item); return null; });
        return true;
    }

    public boolean AddItem(ArrayList<Item> items) {
        Objects.requireNonNull(items, "items is null");
        write(s -> { for (Item it : items) s.persist(it); return null; });
        return true;
    }

    public boolean EditItem(Item editedItem) {
        Objects.requireNonNull(editedItem, "editedItem is null");
        write(s -> {
            Item i = s.get(Item.class, editedItem.getId());
            if (i == null) throw new IllegalArgumentException("Item id=" + editedItem.getId() + " not found");
            i.setPrice(editedItem.getPrice());
            i.setImageLink(editedItem.getImageLink());
            i.setDescription(editedItem.getDescription());
            i.setName(editedItem.getName());
            i.setType(editedItem.getType());
            i.setColor(editedItem.getColor());
            // s.update(i); // not needed: managed entity
            return null;
        });
        return true;
    }

    public boolean RemoveItem(Item itemToRemove) {
        Objects.requireNonNull(itemToRemove, "itemToRemove is null");
        write(s -> {
            Item managed = s.get(Item.class, itemToRemove.getId());
            if (managed != null) s.remove(managed);
            return null;
        });
        return true;
    }

    public boolean RemoveItem(ArrayList<Item> itemListToRemove) {
        Objects.requireNonNull(itemListToRemove, "itemListToRemove is null");
        write(s -> {
            for (Item toRemove : itemListToRemove) {
                Item managed = s.get(Item.class, toRemove.getId());
                if (managed != null) s.remove(managed);
            }
            return null;
        });
        return true;
    }

    public Item GetItem(int id) {
        return read(s -> s.get(Item.class, id));
    }

    public ArrayList<Item> GetItem(ArrayList<Integer> idList) {
        return read(s -> {
            ArrayList<Item> result = new ArrayList<>();
            for (int id : idList) {
                Item it = s.get(Item.class, id);
                if (it != null) result.add(it);
            }
            return result;
        });
    }

    /** filterList not currently used: return all for now (kept your behavior). */
    public List<Item> GetItemList(List<Filter> filterList) {
        return read(s -> s.createQuery("from Item", Item.class).getResultList());
    }

    /** Optional: typed filtering using Request.Filter (one filter). */
    public List<Item> GetItemList(Filter f) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("select i from Item i");
            List<Object> params = new ArrayList<>();
            List<String> where = new ArrayList<>();

            if (f != null) {
                if (nz(f.getSearchText())) {
                    where.add("(lower(i.name) like ?1 or lower(i.description) like ?1)");
                    params.add("%" + f.getSearchText().toLowerCase() + "%");
                }
                if (nz(f.getCategory()) && !"all".equalsIgnoreCase(f.getCategory())) {
                    where.add("lower(i.type) = ?" + (params.size() + 1));
                    params.add(f.getCategory().toLowerCase());
                }
                if (nz(f.getColor())) {
                    where.add("lower(i.color) = ?" + (params.size() + 1));
                    params.add(f.getColor().toLowerCase());
                }
                if (f.getMinPrice() != null) {
                    where.add("i.price >= ?" + (params.size() + 1));
                    params.add(f.getMinPrice());
                }
                if (f.getMaxPrice() != null) {
                    where.add("i.price <= ?" + (params.size() + 1));
                    params.add(f.getMaxPrice());
                }
            }
            if (!where.isEmpty()) hql.append(" where ").append(String.join(" and ", where));

            Query<Item> q = s.createQuery(hql.toString(), Item.class);
            for (int i = 0; i < params.size(); i++) q.setParameter(i + 1, params.get(i));
            return q.getResultList();
        });
    }

    private static boolean nz(String s) { return s != null && !s.isBlank(); }
}
