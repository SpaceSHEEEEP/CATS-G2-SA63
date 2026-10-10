package sg.edu.iss.cats.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

 
@Entity 
@Getter 
@Setter 
@ToString 
@NoArgsConstructor 
public class Comment {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank (message = "Comment is required")
    private String commentText;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Course course;

    @OneToOne
    @JoinColumn(name = "application_id", unique = true)
    private Application application;

    public Comment(Application app) {
        // TODO: idk if this works
        this.course = app.getCourse();
        this.application = app;
    }
    
}
