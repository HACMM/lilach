package il.cshaifasweng.OCSFMediatorExample.client.Events;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;

import java.io.Serializable;

public class AddItemEvent implements Serializable {
    private long itemId;
    private Item item;

    public AddItemEvent() {
    }

    public AddItemEvent(long itemId, Item item) {
        this.itemId = itemId;
        this.item = item;
    }

    public long getItemId() {
        return itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}