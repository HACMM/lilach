package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(name = "Item")
public class Item implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", nullable = false, unique = true)
    private int id;
    // Item name: "Rose"
    @Column(name = "name") private String name;
    @Column(name = "description") private String description;
    @Column(name = "type") private String type;
    @Column(name = "price") private double price;
    @Column(name = "image_link") private String imageLink;
    @Column(name = "color") private String color;
    @Column(name = "flower_type") private String flowerType;

    @OneToMany(mappedBy = "primaryKey.item",
            cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<ItemSale> sales = new HashSet<>();

    @OneToMany(mappedBy = "primaryKey.item",
            cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<ItemCategory> categories = new HashSet<>();

    @OneToMany(mappedBy = "primaryKey.item",
            cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BranchInventory> branchInventory = new HashSet<>();

    public Item() {}
    public Item(String name, String type, double price, String imageLink, String color, String flowerType) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.imageLink = imageLink;
        this.color = color;
        this.flowerType = flowerType;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getPrice() { return price; }
    public void setPrice(double p) { this.price = p; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getFlowerType() { return flowerType; }
    public void setFlowerType(String flowerType) { this.flowerType = flowerType; }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public Set<ItemSale> getSales() {
        return sales;
    }

    public void setSales(Set<ItemSale> sales) {
        this.sales = sales;
    }

    public void addSale(ItemSale sale) {
        sales.add(sale);
    }

    public Set<ItemCategory> getCategories() {
        return categories;
    }

    public void setCategories(Set<ItemCategory> categories) {
        this.categories = categories;
    }

    public void addCategory(ItemCategory category) {
        categories.add(category);
    }

    public void removeCategory(ItemCategory category) {
        categories.remove(category);
    }

    public Set<BranchInventory> getBranchInventory() {
        return branchInventory;
    }

    public void setBranchInventory(Set<BranchInventory> branchInventory) {
        this.branchInventory = branchInventory;
    }
}

