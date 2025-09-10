package il.cshaifasweng.OCSFMediatorExample.entities;

public class Filter {
    private String searchText;
    private String category;     //  (Bouquet, Flower, Plant, Accessory)
    private String flowerType;
    private String color;
    private Double minPrice;
    private Double maxPrice;

    public Filter() {
        this.category = "All";
    }

    public boolean filter(Item item) {
        if (searchText != null && !searchText.isEmpty()) {
            String lower = searchText.toLowerCase();
            if (!(item.getName().toLowerCase().contains(lower) ||
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains(lower)) ||
                    item.getType().toLowerCase().contains(lower))) {
                return false;
            }
        }

        if (category != null && !"All".equalsIgnoreCase(category)) {
            if (!item.getType().equalsIgnoreCase(category)) {
                return false;
            }
        }

        if (flowerType != null && !flowerType.isEmpty()) {
            if (item.getFlowerType() == null || !item.getFlowerType().equalsIgnoreCase(flowerType)) {
                return false;
            }
        }

        if (color != null && !color.isEmpty()) {
            if (item.getColor() == null || !item.getColor().equalsIgnoreCase(color)) {
                return false;
            }
        }

        if (minPrice != null && item.getPrice() < minPrice) {
            return false;
        }

        if (maxPrice != null && item.getPrice() > maxPrice) {
            return false;
        }

        return true;
    }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFlowerType() { return flowerType; }
    public void setFlowerType(String flowerType) { this.flowerType = flowerType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
}

