package Request;
import java.io.Serializable;

public class CustomOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemType;
    private Double minPrice;
    private Double maxPrice;
    private String color;
    private String notes;

    public CustomOrder(String itemType, Double minPrice, Double maxPrice, String color, String notes) {
        this.itemType = itemType;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.color = color;
        this.notes = notes;
    }

    public String getItemType() { return itemType; }
    public Double getMinPrice() { return minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public String getColor() { return color; }
    public String getNotes() { return notes; }
}
