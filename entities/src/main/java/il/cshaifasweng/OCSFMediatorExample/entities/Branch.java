package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;

@Entity
@Table(name = "Branch")
public class Branch {

    // TODO: add branch inventory list

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id", nullable = false, unique = true)
    private int id;

    @Column(name = "name") private String name;

    @Column(name = "description") private String description;

    @Column(name = "schedule") private String schedule;


}
