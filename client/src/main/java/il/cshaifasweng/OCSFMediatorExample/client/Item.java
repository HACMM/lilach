package il.cshaifasweng.OCSFMediatorExample.client;

import java.io.Serializable;

public class Item implements Serializable {
    private int id;
    private String name;
    private String type;
    private double price;

    public Item() {}
    public Item(int id, String name, String type, double price) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public double getPrice() { return price; }
    public void setPrice(double p) { price = p; }
}}
