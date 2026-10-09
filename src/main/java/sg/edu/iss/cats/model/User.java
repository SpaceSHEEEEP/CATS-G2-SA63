package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter 
@Setter 
@NoArgsConstructor
public class User implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;
    private String name;
    private BigDecimal budgetedAllowance;
    private BigDecimal actualAllowance;
    private Double budgetedDays;
    private Double actualDays;

    // need usernames to be unique
    // this is a potential test unit to check when we add the "create 
    // user" additional additional feature in phase 3
    @Column(unique = true, nullable = false) 
    private String username;
    @Column(nullable = false, length = 100)
    private String password;
    private String role;
    private boolean isAdmin;
    private String email;
    // private int reportsToId; // using @OneToMany and @ManyToOne now

    // FetchType is EAGEr because I want .countPendingApplications() to work
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER) // each application gets mapped to a "user"
    private List<Application> applications;

    // 'mappedBy = "manager" tells Hibernate: "To find the list of subordinates for a user, look at the manager field on the other side."' ~ Gemini
    @OneToMany(mappedBy = "manager", fetch = FetchType.EAGER) // TODO: make lazy next time
    private List<User> subordinates = new ArrayList<>(); // each subordinate get mappedBy manager
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
        return List.of(new SimpleGrantedAuthority(role != null ? role : "EMPLOYEE"));
    }
    @Override public boolean isAccountNonExpired()     {return true;}
    @Override public boolean isAccountNonLocked()      {return true;}
    @Override public boolean isCredentialsNonExpired() {return true;}
    @Override public boolean isEnabled()               {return true;}
}
