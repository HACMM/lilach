package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "items_sales")
@AssociationOverrides({
        @AssociationOverride(name = "primaryKey.item",
        joinColumns = @JoinColumn(name = "item_id")),
        @AssociationOverride(name = "primaryKey.sale",
                joinColumns = @JoinColumn(name = "sale_id"))
})
public class ItemSale {
    @EmbeddedId
    private ItemSaleId primaryKey;

    @Column(name = "discount_value") private double discount;

    @Enumerated(EnumType.STRING) private DiscountType discountType;

    public ItemSaleId getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(ItemSaleId primaryKey) {
        this.primaryKey = primaryKey;
    }
    @Transient
    public Item getItem() {
        return primaryKey.getItem();
    }
    public void setItem(Item item) {
        primaryKey.setItem(item);
    }

    @Transient
    public Sale getSale() {
        return primaryKey.getSale();
    }
    public void setSale(Sale sale) {
        primaryKey.setSale(sale);
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }
}
