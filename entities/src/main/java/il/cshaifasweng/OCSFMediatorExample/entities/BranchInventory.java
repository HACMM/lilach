package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "branch_inventory")
@AssociationOverrides({
        @AssociationOverride(name = "primaryKey.item",
                joinColumns = @JoinColumn(name = "item_id")),
        @AssociationOverride(name = "primaryKey.branch",
                joinColumns = @JoinColumn(name = "branch_id"))
})
public class BranchInventory {
    @EmbeddedId
    private BranchInventoryId primaryKey = new BranchInventoryId();

    @Column(name = "amount", nullable = true)
    private int amount;

    public BranchInventory() {
    }

    @Transient
    public Item getItem() {
        return primaryKey.getItem();
    }
    public void setItem(Item item) {
        primaryKey.setItem(item);
    }

    @Transient
    public Branch getBranch() {
        return primaryKey.getBranch();
    }
    public void setBranch(Branch branch) {
        primaryKey.setBranch(branch);
    }

    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
}
