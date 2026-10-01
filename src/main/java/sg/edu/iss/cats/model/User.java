package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;
    private String name;
    private BigDecimal budget;
    private double trainingDays;

    // need usernames to be unique
    // this is a potential test unit to check when we add the "create 
    // user" additional additional feature in phase 3
    @Column(unique = true) 
    private String username;
    private String password;
    private boolean isActive;
    private boolean isAdmin;
    private String email;
    // private int reportsToId; // using @OneToMany and @ManyToOne now

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY) // each application gets mapped to a "user"
    private List<Application> applications;

    // 'mappedBy = "manager" tells Hibernate: "To find the list of subordinates for a user, look at the manager field on the other side."' ~ Gemini
    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    private List<User> subordinates = new ArrayList<>(); // each subordinate get mappedBy manager
    @ManyToOne // the other side
    private User manager;
}
