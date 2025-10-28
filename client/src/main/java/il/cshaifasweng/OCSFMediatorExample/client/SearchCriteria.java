package il.cshaifasweng.OCSFMediatorExample.client;

public class SearchCriteria {
    public final String type;
    public final String color;
    public final String minPrice;
    public final String maxPrice;

    public SearchCriteria(String type, String color, String minPrice, String maxPrice) {
        this.type = type;
        this.color = color;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }
}

