package sg.edu.iss.cats.model;

import lombok.Data;
import lombok.NoArgsConstructor;

// @Entity // not necessary
@Data
@NoArgsConstructor
public class LoginForm {
    private String username;
    private String password;
}
