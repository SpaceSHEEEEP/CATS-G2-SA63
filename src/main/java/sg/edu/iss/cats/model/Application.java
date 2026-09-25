package sg.edu.iss.cats.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY);
    private int applicationId;
    // TODO: add the rest of the attributes
}
