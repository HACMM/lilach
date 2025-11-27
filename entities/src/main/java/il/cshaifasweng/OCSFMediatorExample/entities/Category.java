package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Category")
public class Category implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false, unique = true)
    private int category_id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "primaryKey.category",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private Set<ItemCategory> itemCategories = new HashSet<>();

    protected Category() {}

    public Category(final String name, final String description) {
        this.name = name;
        this.description = description;
    }
    public int getCategory_id() {
        return category_id;
    }
    public void setCategory_id(final int category_id) {
        this.category_id = category_id;
    }
    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description = description;
    }
    public Set<ItemCategory> getItemCategories() {
        return itemCategories;
    }
    public void setItemCategories(final Set<ItemCategory> itemCategories) {
        this.itemCategories = itemCategories;
    }
}
