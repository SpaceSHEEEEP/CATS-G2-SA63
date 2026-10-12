package sg.edu.iss.cats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity @Getter @Setter
@Table(name = "training_provider", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class TrainingProvider {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean enabled = true;
}
