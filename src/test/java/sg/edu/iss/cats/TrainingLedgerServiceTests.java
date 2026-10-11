package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.*;

class TrainingLedgerServiceTests {
    @Test void ledgerSeparatesApprovedCourseFeesFromConfirmedReimbursements() {
        TrainingAccountService accounts = mock(TrainingAccountService.class);
        AppRepo apps = mock(AppRepo.class);
        FeeClaimRepository claims = mock(FeeClaimRepository.class);
        TrainingLedgerService service = new TrainingLedgerService(accounts, apps, claims);
        User employee = new User(); employee.setUserId(4);
        Course course = new Course("External", CourseType.EXTERNAL,
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21),
                "ISS", "Provider", new BigDecimal("500.00"), CourseDuration.FULLDAY);
        Application app = new Application(); app.setId(8); app.setStatus(Status.COMPLETED);
        app.setUser(employee); app.setCourse(course);
        FeeClaim paid = new FeeClaim(); paid.setApplication(app); paid.setEmployee(employee);
        paid.setAmount(new BigDecimal("500.00")); paid.setStatus(FeeClaim.ClaimStatus.PAID);
        paid.setPaidOn(LocalDate.of(2026, 10, 30)); paid.setPaymentReference("REF100");
        FeeClaim merelyApproved = new FeeClaim(); merelyApproved.setApplication(app);
        merelyApproved.setEmployee(employee); merelyApproved.setAmount(new BigDecimal("500.00"));
        merelyApproved.setStatus(FeeClaim.ClaimStatus.APPROVED);
        when(accounts.summary(employee, 2026)).thenReturn(new TrainingAccountService.Summary(
                2026, 10.0, new BigDecimal("2000.00"), 2.0, new BigDecimal("500.00"), 2.0,
                new BigDecimal("500.00")));
        when(apps.findByUser_UserId(4)).thenReturn(List.of(app));
        when(claims.findByEmployee_UserId(4)).thenReturn(List.of(paid, merelyApproved));
        var ledger = service.forEmployee(employee, 2026);
        assertThat(ledger.approvedCourseFees()).isEqualByComparingTo("500.00");
        assertThat(ledger.reimbursementsPaid()).isEqualByComparingTo("500.00");
        assertThat(ledger.entries()).hasSize(2);
    }
}
