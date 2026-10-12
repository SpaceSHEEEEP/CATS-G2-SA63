package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.iss.cats.controller.FeeClaimController;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.FeeClaimService;

class FeeClaimWorkflowTests {
    private final FeeClaimRepository claims = mock(FeeClaimRepository.class);
    private final AppRepo apps = mock(AppRepo.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FeeClaimService service = new FeeClaimService(claims, apps, users);

    private User user(int id, Role role) {
        User u = new User(); u.setUserId(id); u.setRole(role); return u;
    }
    private Application completedCourse(int employeeId) {
        Application a = new Application(); a.setId(8);
        a.setUser(user(employeeId, Role.ROLE_EMPLOYEE)); a.setStatus(Status.COMPLETED);
        Course course = new Course(); course.setCourseType(CourseType.EXTERNAL);
        course.setFee(new BigDecimal("450.00")); a.setCourse(course);
        return a;
    }
    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile(name, "evidence.pdf", "application/pdf",
                "%PDF-1.4\n1 0 obj\nendobj\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test void fakePdfRejectedWithoutSavingClaim() {
        when(users.findLockedById(4)).thenReturn(Optional.of(user(4, Role.ROLE_EMPLOYEE)));
        when(apps.findById(8)).thenReturn(Optional.of(completedCourse(4)));
        MockMultipartFile fake = new MockMultipartFile("receipt", "fake.pdf", "application/pdf",
                "not a pdf".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertThatThrownBy(() -> service.submit(4, 8, fake, pdf("certificate")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("signature");
        verify(claims, never()).save(any());
    }

    @Test void validSignedEvidenceIsPersisted() throws Exception {
        when(users.findLockedById(4)).thenReturn(Optional.of(user(4, Role.ROLE_EMPLOYEE)));
        when(apps.findById(8)).thenReturn(Optional.of(completedCourse(4)));
        when(claims.save(any(FeeClaim.class))).thenAnswer(inv -> inv.getArgument(0));
        FeeClaim c = service.submit(4, 8, pdf("receipt"), pdf("certificate"));
        assertThat(c.getStatus()).isEqualTo(FeeClaim.ClaimStatus.SUBMITTED);
        assertThat(c.getPaidOn()).isNull();
        // Submitting a receipt proves personal payment, not reimbursement.
        verify(apps, never()).save(any(Application.class));
        assertThat(c.getReceiptReference()).isEqualTo("application/pdf");
        assertThat(c.getReceiptData()).startsWith("%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test void previouslyRejectedClaimCannotBeSubmittedAgain() {
        when(users.findLockedById(4)).thenReturn(Optional.of(user(4, Role.ROLE_EMPLOYEE)));
        when(apps.findById(8)).thenReturn(Optional.of(completedCourse(4)));
        // The existence test intentionally includes REJECTED claims.
        when(claims.existsByApplication_Id(8)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(4, 8, pdf("receipt"), pdf("certificate")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a claim");
        verify(claims, never()).save(any(FeeClaim.class));
        verify(apps, never()).save(any(Application.class));
    }

    @Test void managerMayOnlyDecideDirectSubordinatesClaims() {
        User manager = user(2, Role.ROLE_MANAGER);
        User owner = user(4, Role.ROLE_EMPLOYEE);
        owner.setManager(user(99, Role.ROLE_MANAGER));
        FeeClaim claim = new FeeClaim(); claim.setEmployee(owner);
        when(users.findLockedById(2)).thenReturn(Optional.of(manager));
        when(claims.findById(5)).thenReturn(Optional.of(claim));
        assertThatThrownBy(() -> service.decide(2, 5, FeeClaim.ClaimStatus.APPROVED, "Relevant"))
                .isInstanceOf(SecurityException.class);
        verify(claims, never()).save(any());
    }

    @Test void adminCanRecordActualPaymentOnce() {
        User admin = user(1, Role.ROLE_ADMIN);
        FeeClaim claim = new FeeClaim(); claim.setStatus(FeeClaim.ClaimStatus.APPROVED);
        when(users.findLockedById(1)).thenReturn(Optional.of(admin));
        when(claims.findById(5)).thenReturn(Optional.of(claim));
        service.recordPayment(1, 5, " BANK-123 ");
        assertThat(claim.getStatus()).isEqualTo(FeeClaim.ClaimStatus.PAID);
        assertThat(claim.getPaidOn()).isNotNull();
        assertThat(claim.getPaymentReference()).isEqualTo("BANK-123");
        assertThatThrownBy(() -> service.recordPayment(1, 5, "BANK-123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void unauthorisedEvidenceDownloadReturnsHttp403() {
        User outsider = user(7, Role.ROLE_EMPLOYEE);
        when(claims.findById(5)).thenReturn(Optional.of(new FeeClaim() {{
            setEmployee(user(4, Role.ROLE_EMPLOYEE));
        }}));
        FeeClaimController controller = new FeeClaimController(service, apps, claims);
        assertThatThrownBy(() -> controller.download(outsider, 5, "receipt"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
