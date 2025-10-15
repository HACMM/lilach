package il.cshaifasweng.OCSFMediatorExample.entities;

// BranchInventoryId acts like a composite key to BranchInventory table
// It ensures the user would not add an item to the certain branch
// twice

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Embeddable
public class BranchInventoryId implements Serializable {

    @ManyToOne(cascade = CascadeType.ALL)
    private Item item;

    @ManyToOne(cascade = CascadeType.ALL)
    private Branch branch;

    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
    }

    public Branch getBranch() {
        return branch;
    }
    public void setBranch(Branch branch) {
        this.branch = branch;
    }
}