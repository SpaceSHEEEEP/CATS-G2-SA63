package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
import lombok.Getter;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter 
@Setter 
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId;

    // apply validation annotations for application form checking
    @NotBlank(message = "Course name is required")
    private String courseName;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Course type is required")
    @jakarta.persistence.Column(length = 32)
    private CourseType courseType;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotBlank(message = "Location is required")
    private String location;
    
    /** Historical display snapshot; authority comes from the provider catalogue. */
    @jakarta.persistence.Column(nullable = false)
    private String trainingProvider;

    @jakarta.persistence.ManyToOne(fetch = FetchType.EAGER)
    @jakarta.persistence.JoinColumn(name = "provider_id")
    private TrainingProvider provider;

    /** UI-only selector; provider itself is resolved server-side. */
    @jakarta.persistence.Transient
    private Integer providerId;

    @NotNull(message = "Please input the course fee")
    @PositiveOrZero(message="Course fee cannot be negative")
    private BigDecimal fee;

    // Half-day sessions are only permitted for single-day internal training.
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Course duration is required")
    @jakarta.persistence.Column(length = 32)
    private CourseDuration duration;
    // Existing historical applications keep their snapshots after catalogue edits.
    // NULL means active for courses created before this flag was introduced.
    private Boolean archived;

    // map course to application 
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Application> applications;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Comment> comments;

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
