package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "items_categories")
@AssociationOverrides({
        @AssociationOverride(name = "primaryKey.item",
                joinColumns = @JoinColumn(name = "item_id")),
        @AssociationOverride(name = "primaryKey.category",
                joinColumns = @JoinColumn(name = "category_id"))
})
public class ItemCategory implements Serializable {
    @EmbeddedId
    private ItemCategoryId primaryKey = new ItemCategoryId();

    public ItemCategory() {
    }

    // We can add some specific information here

    @Transient
    public Item getItem() {
        return primaryKey.getItem();
    }
    public void setItem(Item item) {
        primaryKey.setItem(item);
    }

    @Transient
    public Category getCategory() {
        return primaryKey.getCategory();
    }
    public void setCategory(Category category) {
        primaryKey.setCategory(category);
    }

    public ItemCategoryId getPrimaryKey() {
        return primaryKey;
    }
    public void setPrimaryKey(ItemCategoryId primaryKey) {
        this.primaryKey = primaryKey;
    }
}