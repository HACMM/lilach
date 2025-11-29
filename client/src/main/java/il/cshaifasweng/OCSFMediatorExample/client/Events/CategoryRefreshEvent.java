package il.cshaifasweng.OCSFMediatorExample.client.Events;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;
import java.util.List;

public class CategoryRefreshEvent {

    private final List<Item> items;

    public CategoryRefreshEvent(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }
}
