package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.*;

class TrainingReportServiceTests {
    @Test void attendanceReportUsesPaginatedDatabaseFiltering() {
        AppRepo apps = mock(AppRepo.class);
        UserRepository users = mock(UserRepository.class);
        FeeClaimRepository claims = mock(FeeClaimRepository.class);
        TrainingAccountService accounts = mock(TrainingAccountService.class);
        TrainingReportService service = new TrainingReportService(apps, users, claims, accounts);
        LocalDate first = LocalDate.of(2026, 10, 1);
        LocalDate last = LocalDate.of(2026, 10, 31);
        PageRequest requested = PageRequest.of(0, 10);
        when(apps.findAttendanceForReporting(eq(List.of(Status.APPROVED, Status.COMPLETED)),
                eq(first), eq(last), eq(CourseType.EXTERNAL), eq(3), eq(requested)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.attendancePage(first, last, CourseType.EXTERNAL, 3, requested)).isEmpty();
        verify(apps).findAttendanceForReporting(anyCollection(), eq(first), eq(last),
                eq(CourseType.EXTERNAL), eq(3), eq(requested));
        verify(apps, never()).findAllForReporting();
    }

    @Test void paidClaimsAreQueriedByPaymentDate() {
        AppRepo apps = mock(AppRepo.class);
        UserRepository users = mock(UserRepository.class);
        FeeClaimRepository claims = mock(FeeClaimRepository.class);
        TrainingAccountService accounts = mock(TrainingAccountService.class);
        TrainingReportService service = new TrainingReportService(apps, users, claims, accounts);
        User employee = new User(); employee.setUserId(9);
        PageRequest requested = PageRequest.of(0, 10);
        when(users.findByUserId(9, requested)).thenReturn(new PageImpl<>(List.of(employee)));
        service.utilisationPage(2027, 9, requested);
        verify(claims).findByEmployee_UserIdInAndPaidOnBetween(eq(List.of(9)),
                eq(LocalDate.of(2027, 1, 1)), eq(LocalDate.of(2027, 12, 31)));
    }
}
