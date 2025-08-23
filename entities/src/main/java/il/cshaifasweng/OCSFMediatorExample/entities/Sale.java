package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

@Entity
@Table(name = "Sale")
public class Sale {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id", nullable = false, unique = true)
    private int id;

    @Column(name = "name") private String name;

    @Column(name = "description") private String description;

    // Date and time of sale start
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date") private Date startDate;

    // Date and time of sale end
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date") private Date endDate;

    @Enumerated(EnumType.STRING) private SaleStatus status;

    @Column(name = "image_link") private String imageLink;

    @OneToMany(mappedBy = "primaryKey.sale", cascade = CascadeType.ALL)
    private Set<ItemSale> items = new HashSet<>();

    protected Sale() {}

    public Sale(String name, String description, Date startDate, Date endDate) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Set<ItemSale> getItems() {
        return items;
    }

    public void setItems(Set<ItemSale> items) {
        this.items = items;
    }

    public void addItem(ItemSale item) {
        items.add(item);
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }
}
