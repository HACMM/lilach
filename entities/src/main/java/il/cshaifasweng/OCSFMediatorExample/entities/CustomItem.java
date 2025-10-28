package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_items")
public class CustomItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount customer; // מי עשה את ההזמנה

    @Column(nullable = false)
    private String itemType;  // סוג הפריט (זר, עציץ, סידור...)

    private Double minPrice;  // מחיר מינימלי רצוי
    private Double maxPrice;  // מחיר מקסימלי רצוי
    private String color;     // צבע שולט (אופציונלי)

    @Column(length = 500)
    private String notes;     // הערות נוספות

    @Column(nullable = false)
    private LocalDateTime dateCreated = LocalDateTime.now(); // מתי נוצרה ההזמנה

    @Column(nullable = false)
    private String status = "Pending"; // סטטוס ההזמנה (ברירת מחדל: ממתינה)

    public CustomItem() {}

    public CustomItem(UserAccount customer, String itemType, Double minPrice,
                      Double maxPrice, String color, String notes) {
        this.customer = customer;
        this.itemType = itemType;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.color = color;
        this.notes = notes;
        this.dateCreated = LocalDateTime.now();
        this.status = "Pending";
    }

    // 🔹 Getters & Setters
    public int getId() { return id; }
    public UserAccount getCustomer() { return customer; }
    public void setCustomer(UserAccount customer) { this.customer = customer; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "CustomItem{" +
                "id=" + id +
                ", customer=" + (customer != null ? customer.getEmail() : "null") +
                ", itemType='" + itemType + '\'' +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", color='" + color + '\'' +
                ", notes='" + notes + '\'' +
                ", dateCreated=" + dateCreated +
                ", status='" + status + '\'' +
                '}';
    }
}
