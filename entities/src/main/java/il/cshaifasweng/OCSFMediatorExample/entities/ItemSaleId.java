package il.cshaifasweng.OCSFMediatorExample.entities;


import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;

// ItemSaleId acts like a composite key to ItemSale table
// It ensures the user would not add an item to the certain sale
// twice

@Embeddable
public class ItemSaleId implements Serializable {
    private Item item;
    private Sale sale;

    @ManyToOne(cascade = CascadeType.ALL)
    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
    }

    @ManyToOne(cascade = CascadeType.ALL)
    public Sale getSale() {
        return sale;
    }
    public void setSale(Sale sale) {
        this.sale = sale;
    }
}
