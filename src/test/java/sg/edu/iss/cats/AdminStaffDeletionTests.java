package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.*;

class AdminStaffDeletionTests {
    private final UserRepository users = mock(UserRepository.class);
    private final AppRepo apps = mock(AppRepo.class);
    private final FeeClaimRepository claims = mock(FeeClaimRepository.class);
    private final TrainingYearRepository years = mock(TrainingYearRepository.class);
    private final AdminStaffService service = new AdminStaffService(users,
            mock(TrainingAccountService.class), mock(PasswordEncoder.class), apps, claims, years);

    @Test void refusesToDestroyAnEmployeesTrainingHistory() {
        User employee = new User(); employee.setUserId(10);
        when(users.findById(10)).thenReturn(Optional.of(employee));
        when(apps.existsByUser_UserId(10)).thenReturn(true);
        assertThatThrownBy(() -> service.deleteUnused(10, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("deactivate");
        verify(users, never()).delete(any());
    }

    @Test void unusedAccountCanBeDeletedAlongWithEmptyYearAllocations() {
        User employee = new User(); employee.setUserId(10);
        when(users.findById(10)).thenReturn(Optional.of(employee));
        service.deleteUnused(10, 1);
        verify(years).deleteByUser_UserId(10);
        verify(users).delete(employee);
        verify(users).flush();
    }
}
