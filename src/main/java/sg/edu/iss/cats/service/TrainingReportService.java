package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
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
        private final BigDecimal claimsApproved;
        private final BigDecimal claimsPending;
    }

    /** Only approved/completed attendance, date ranges overlap inclusively. */
    @Transactional(readOnly = true)
    public List<Application> attendance(Integer managerId, LocalDate from, LocalDate to,
                                        CourseType category, Integer employeeId) {
        if (from == null || to == null || to.isBefore(from))
            throw new IllegalArgumentException("Invalid reporting period");
        return apps.findAllForReporting().stream()
            .filter(app -> app.getStatus() == Status.APPROVED || app.getStatus() == Status.COMPLETED)
            .filter(app -> !app.getCourse().getStartDate().isAfter(to)
                    && !app.getCourse().getEndDate().isBefore(from))
            .filter(app -> category == null || app.getCourse().getCourseType() == category)
            .filter(app -> employeeId == null || app.getUser().getUserId().equals(employeeId))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeClaim> claims(Integer managerId, int year, Integer employeeId) {
        return claims.findAllForReporting().stream()
            .filter(claim -> claim.getSubmittedOn().getYear() == year)
            .filter(claim -> employeeId == null || claim.getEmployee().getUserId().equals(employeeId))
            .sorted(Comparator.comparing(FeeClaim::getSubmittedOn).reversed())
            .toList();
    }

    @Transactional
    public List<BudgetRow> utilisation(Integer managerId, int year, Integer employeeId) {
        List<FeeClaim> allClaims = claims(managerId, year, employeeId);
        List<BudgetRow> result = new ArrayList<>();
        for (User employee : users.findAll()) {
            if (employeeId != null && !employee.getUserId().equals(employeeId)) continue;
            BigDecimal approved = BigDecimal.ZERO, pending = BigDecimal.ZERO;
            for (FeeClaim claim : allClaims) {
                if (!claim.getEmployee().getUserId().equals(employee.getUserId())) continue;
                if (claim.getStatus() == FeeClaim.ClaimStatus.APPROVED) approved = approved.add(claim.getAmount());
                if (claim.getStatus() == FeeClaim.ClaimStatus.SUBMITTED) pending = pending.add(claim.getAmount());
            }
            result.add(new BudgetRow(employee, account.summary(employee, year), approved, pending));
        }
        result.sort(Comparator.comparing(row -> row.getEmployee().getName()));
        return result;
    }
}
