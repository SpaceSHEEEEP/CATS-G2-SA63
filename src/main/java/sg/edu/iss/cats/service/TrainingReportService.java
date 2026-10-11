package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;

@Service
public class TrainingReportService {
    private final AppRepo apps;
    private final UserRepository users;
    private final FeeClaimRepository claims;
    private final TrainingAccountService account;

    public TrainingReportService(AppRepo apps, UserRepository users, FeeClaimRepository claims,
                                 TrainingAccountService account) {
        this.apps = apps; this.users = users; this.claims = claims; this.account = account;
    }

    @Getter @AllArgsConstructor
    public static class BudgetRow {
        private final User employee;
        private final TrainingAccountService.Summary summary;
        private final BigDecimal claimsReimbursed;
        private final BigDecimal claimsApprovedAwaitingPayment;
        private final BigDecimal claimsPending;
    }

    /** Only approved/completed attendance, date ranges overlap inclusively. */
    @Transactional(readOnly = true)
    public Page<Application> attendancePage(LocalDate from, LocalDate to,
            CourseType category, Integer employeeId, Pageable pageable) {
        if (from == null || to == null || to.isBefore(from))
            throw new IllegalArgumentException("Invalid reporting period");
        return apps.findAttendanceForReporting(List.of(Status.APPROVED, Status.COMPLETED),
                from, to, category, employeeId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FeeClaim> claimsPage(int year, Integer employeeId, Pageable pageable) {
        LocalDate first = LocalDate.of(year, 1, 1);
        LocalDate last = LocalDate.of(year, 12, 31);
        return claims.findReportClaims(first, last, employeeId, pageable);
    }

    @Transactional
    public Page<BudgetRow> utilisationPage(int year, Integer employeeId, Pageable pageable) {
        Page<User> employees = employeeId == null
                ? users.findAll(pageable) : users.findByUserId(employeeId, pageable);
        List<Integer> ids = employees.getContent().stream().map(User::getUserId).toList();
        List<FeeClaim> yearClaims = ids.isEmpty() ? List.of()
                : claims.findByEmployee_UserIdInAndSubmittedOnBetween(ids,
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        List<FeeClaim> paymentClaims = ids.isEmpty() ? List.of()
                : claims.findByEmployee_UserIdInAndPaidOnBetween(ids,
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return employees.map(employee -> {
            BigDecimal paid = BigDecimal.ZERO, approved = BigDecimal.ZERO, pending = BigDecimal.ZERO;
            for (FeeClaim claim : yearClaims) {
                if (!employee.getUserId().equals(claim.getEmployee().getUserId())) continue;
                if (claim.getStatus() == FeeClaim.ClaimStatus.APPROVED) approved = approved.add(claim.getAmount());
                if (claim.getStatus() == FeeClaim.ClaimStatus.SUBMITTED) pending = pending.add(claim.getAmount());
            }
            for (FeeClaim payment : paymentClaims) {
                if (employee.getUserId().equals(payment.getEmployee().getUserId())
                        && payment.getStatus() == FeeClaim.ClaimStatus.PAID)
                    paid = paid.add(payment.getAmount());
            }
            return new BudgetRow(employee, account.summary(employee, year), paid, approved, pending);
        });
    }
}
