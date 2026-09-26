package sg.edu.iss.cats.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int applicationId;
    private int courseId;
    private int userId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;
    private String userReason;
    private String managerReason;
    private Boolean hasBeenPaid;
}
