package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ItemSaleId implements Serializable {

    @ManyToOne(optional = false)
    private Item item;

    @ManyToOne(optional = false)
    private Sale sale;

    public ItemSaleId() {}

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemSaleId)) return false;
        ItemSaleId that = (ItemSaleId) o;
        // compare by identifiers if available; otherwise by entity equality is fine
        return Objects.equals(item, that.item) &&
                Objects.equals(sale, that.sale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, sale);
    }
}
