package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.*;

@Entity
@Table(name = "complaints")
public class Complaint implements Serializable {

    // Unique ID for each complaint
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Category of the complaint (e.g., Service, Product Quality, Delivery Delay)
    @Column(nullable = false)
    private String type;

    // Full description of what went wrong
    @Column(nullable = false, length = 2000)
    private String description;

    // Timestamp when the complaint was submitted
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Default constructor for JPA and serialization
    public Complaint() {}

    // Convenience constructor sets type, description and creation time
    public Complaint(String type, String description) {
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // ---- Getters and setters ----

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}