package sg.edu.iss.cats.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int applicationId;
    // private int courseId; // added ManyToOne relationship below
    // private int userId; // added ManyToOne relationship below

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private ApplicationStatus applicationStatus;

    @Column(nullable=false)
    @Size(max=1000, message="Reason cannot exceed 1000 characters.")
    @NotBlank(message="Please provide a reason or justification for your application.")
    private String userReason;
    private String managerReason;
    private Boolean hasBeenPaid;

    @ManyToOne(fetch=FetchType.LAZY, cascade={CascadeType.PERSIST, CascadeType.MERGE})
    @Valid
    private Course course;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id")
    @Valid
    private User user;
}
