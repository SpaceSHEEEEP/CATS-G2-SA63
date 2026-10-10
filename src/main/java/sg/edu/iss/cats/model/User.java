package sg.edu.iss.cats.model;

import sg.edu.iss.cats.model.Role;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.ToString;

@Entity
@Getter 
@Setter 
@NoArgsConstructor
public class User implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @NotBlank(message="Name must not be empty")
    private String name;

    @PositiveOrZero(message="Allowance cannot be negative")
    private BigDecimal budgetedAllowance;
    @PositiveOrZero(message="Allowance cannot be negative")
    private BigDecimal actualAllowance;
    @PositiveOrZero(message="Days cannot be negative")
    private Double budgetedDays;
    @PositiveOrZero(message="Days cannot be negative")
    private Double actualDays;

    // need usernames to be unique
    // this is a potential test unit to check when we add the "create 
    // user" additional additional feature in phase 3
    @Column(unique = true, nullable = false) 
    private String username;
    @Column(nullable = false, length = 100)
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    // Legacy flag retained for seeded data; role is the authority source.
    private boolean isAdmin;
    @Enumerated(EnumType.STRING)
    private Designation designation = Designation.PROFESSIONAL;
    // NULL means enabled to preserve older seed data that did not set this column.
    private Boolean active;
    @NotBlank(message="Email must not be empty")
    @jakarta.validation.constraints.Email
    private String email;
    // private int reportsToId; // using @OneToMany and @ManyToOne now

    // Collections are lazy to avoid expensive and conflicting bag fetches.
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY) // each application gets mapped to a "user"
    private List<Application> applications = new ArrayList<>();

    // 'mappedBy = "manager" tells Hibernate: "To find the list of subordinates for a user, look at the manager field on the other side."' ~ Gemini
    @ToString.Exclude
    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY) // loaded explicitly by manager queries
    private List<User> subordinates = new ArrayList<>(); // each subordinate get mappedBy manager
    @ToString.Exclude
    @ManyToOne // the other side
    private User manager;

    public Integer countPendingApplications() {
        if (applications.isEmpty()) return 0;
        Integer count = 0;
        for (Application app : applications) {
            if (app.getStatus() == Status.APPLIED || 
                app.getStatus() == Status.UPDATED) count++;
        }
        System.out.println("DEBUG: COUNT WAS CALLED. # OF PENDING APPS: " + count);
        return count;
    }

    // To implement UserDetails
    @Override  
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role == null ? List.of() : List.of(new SimpleGrantedAuthority(role.name()));
    }
    @Override public boolean isAccountNonExpired()     {return true;}
    @Override public boolean isAccountNonLocked()      {return true;}
    @Override public boolean isCredentialsNonExpired() {return true;}
    @Override public boolean isEnabled()               {return active == null || active;}
}
