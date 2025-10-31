package Request.reports;

public enum ItemGrouping {
    ALL,       // aggregate everything
    BY_TYPE,   // group by Item.type
    BY_ITEM    // group by Item.id (or name)
}
