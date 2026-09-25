package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private String username;
    private String password;
    private boolean isActive;
    private boolean isAdmin;
    private String email;
    // private Optional<Integer> reportsToId; // this doesn't work
    private int reportsToId; // points to himself/herself if he/she is CEO
}
