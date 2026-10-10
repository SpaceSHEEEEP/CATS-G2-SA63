package sg.edu.iss.cats.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"userId", "name", "isActive", "isAdmin", "email", "managerName"})
public class UserDTO {
	
    private Integer userId;
    private String name;
    private boolean isActive;
    private boolean isAdmin;
    private String email;
    private String managerName;

}
