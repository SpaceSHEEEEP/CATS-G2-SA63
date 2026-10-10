package sg.edu.iss.cats.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import sg.edu.iss.cats.model.*;

/** Form DTO: never bind an untrusted POST directly to User/UserDetails. */
@Getter @Setter
public class AdminUserForm {
    private Integer userId;
    @NotBlank private String name;
    @NotBlank private String username;
    @NotBlank @Email private String email;
    private String password;
    @NotNull private Role role = Role.ROLE_EMPLOYEE;
    @NotNull private Designation designation = Designation.PROFESSIONAL;
    private Integer managerId;
    @Min(2000) @Max(9999) private int year = java.time.Year.now().getValue();
    @NotNull @PositiveOrZero private Double annualDays;
    @NotNull @PositiveOrZero private BigDecimal annualAllowance;
    private boolean enabled = true;
}
