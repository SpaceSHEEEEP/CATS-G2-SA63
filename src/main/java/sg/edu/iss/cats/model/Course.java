package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.Formula;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId;

    // apply validation annotations for application form checking
    @NotBlank(message = "Course name is required")
    private String courseName;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Course type is required")
    private CourseType courseType;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String location;
    private String trainingProvider;

    @NotNull(message = "Please input the course fee")
    private BigDecimal fee;

    // added course duration
    @Enumerated(EnumType.STRING)
    // @NotNull (message = "Course duration is required")
    private CourseDuration duration;

    // map course to application 
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Application> applications;

    public Course(String courseName, CourseType courseType, LocalDate startDate, LocalDate endDate, String location, String trainingProvider, BigDecimal fee, CourseDuration duration) {
        this.courseName = courseName;
        this.courseType = courseType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.trainingProvider = trainingProvider;
        this.fee = fee;
        this.duration = duration;
    }
    
}
