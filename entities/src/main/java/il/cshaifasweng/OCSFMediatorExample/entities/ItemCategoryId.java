package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import java.io.Serializable;

public class ItemCategoryId implements Serializable {

    @ManyToOne(cascade = CascadeType.ALL)
    private Item item;

    @ManyToOne(cascade = CascadeType.ALL)
    private Category category;

    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
    }

    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
}
