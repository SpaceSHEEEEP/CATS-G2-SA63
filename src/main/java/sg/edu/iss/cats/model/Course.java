package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int courseId;
    private String courseName;

    @Enumerated(EnumType.STRING)
    private CourseType courseType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private String trainingProvider;
    private BigDecimal fee;

    // map course to application 
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Application> applications;
    
}
