package il.cshaifasweng.OCSFMediatorExample.client;

import java.util.Objects;

public class CatalogItem {
    private String name;
    private String type;
    private double price;
    private String imageUrl;

    public CatalogItem() {}
    public CatalogItem(String name, String type, double price, String imageUrl) {
        this.name = name; this.type = type; this.price = price; this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }

    public void setName(String v) { name = v; }
    public void setType(String v) { type = v; }
    public void setPrice(double v) { price = v; }
    public void setImageUrl(String v) { imageUrl = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogItem)) return false;
        CatalogItem that = (CatalogItem) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }
    @Override public int hashCode() { return Objects.hash(name, type); }
}
