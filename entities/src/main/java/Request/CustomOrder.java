package Request;
import java.io.Serializable;

public class CustomOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;  // User who is making the custom order
    private String itemType;
    private Double minPrice;
    private Double maxPrice;
    private String color;
    private String notes;

    public CustomOrder() {}  // Default constructor for serialization

    public CustomOrder(String itemType, Double minPrice, Double maxPrice, String color, String notes) {
        this.itemType = itemType;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.color = color;
        this.notes = notes;
    }

    public CustomOrder(int userId, String itemType, Double minPrice, Double maxPrice, String color, String notes) {
        this.userId = userId;
        this.itemType = itemType;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.color = color;
        this.notes = notes;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getItemType() { return itemType; }
    public Double getMinPrice() { return minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public String getColor() { return color; }
    public String getNotes() { return notes; }
}
