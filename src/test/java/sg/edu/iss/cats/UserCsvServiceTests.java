package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.service.UserCsvService;

class UserCsvServiceTests {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserCsvService service = new UserCsvService(repository);

    @Test
    void exportsCurrentRolesEnabledStateAndManagersWithoutPasswords() {
        User manager = new User();
        manager.setName("@Manager");

        User staff = new User();
        staff.setUserId(3);
        staff.setName(" =SUM(1,2)");
        staff.setEmail("staff@example.org");
        staff.setPassword("DO_NOT_EXPORT");
        staff.setRole(Role.ROLE_EMPLOYEE);
        staff.setActive(false);
        staff.setManager(manager);

        User admin = new User();
        admin.setUserId(5);
        admin.setName("Jan \"QA\"\nDoe");
        admin.setEmail("admin@example.org");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setAdmin(false); // Legacy isAdmin flag is not the authority source.
        admin.setActive(null); // Legacy null active value means enabled.

        when(repository.findAllByOrderByUserIdAsc()).thenReturn(List.of(staff, admin));

        String csv = new String(service.generateCsv(), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("userId,name,isActive,isAdmin,email,managerName\r\n");
        assertThat(csv).contains("3,\"' =SUM(1,2)\",false,false,\"staff@example.org\",\"'@Manager\"\r\n");
        assertThat(csv).contains("5,\"Jan \"\"QA\"\"\nDoe\",true,true,\"admin@example.org\",\"\"\r\n");
        assertThat(csv).doesNotContain("DO_NOT_EXPORT");
        verify(repository).findAllByOrderByUserIdAsc();
    }

    @Test
    void emptyDirectoryStillHasHeaders() {
        when(repository.findAllByOrderByUserIdAsc()).thenReturn(List.of());
        assertThat(new String(service.generateCsv(), StandardCharsets.UTF_8))
                .isEqualTo("userId,name,isActive,isAdmin,email,managerName\r\n");
    }
}
