package il.cshaifasweng.OCSFMediatorExample.client;

public class SearchCriteria {
    public final String type;
    public final String color;
    public final double minPrice;
    public final double maxPrice;

    public SearchCriteria(String type, String color, double minPrice, double maxPrice) {
        this.type = type;
        this.color = color;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    // 🔹 Getters
    public String getType() {
        return type;
    }

    public String getColor() {
        return color;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }
}

