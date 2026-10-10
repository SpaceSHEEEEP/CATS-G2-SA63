package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "training_year", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "calendar_year"}))
public class TrainingYear {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "calendar_year", nullable = false)
    private int year;

    @NotNull @PositiveOrZero @Column(nullable = false)
    private Double dayLimit;

    @NotNull @PositiveOrZero @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal feeLimit;
}
