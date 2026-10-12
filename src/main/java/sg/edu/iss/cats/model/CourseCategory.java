package sg.edu.iss.cats.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** The three specification-mandated category codes are stable; admins maintain metadata and availability. */
@Entity @Getter @Setter
@Table(name = "course_category")
public class CourseCategory {
    @Id @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "category_code", length = 32)
    private CourseType code;

    @NotBlank @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;
}
