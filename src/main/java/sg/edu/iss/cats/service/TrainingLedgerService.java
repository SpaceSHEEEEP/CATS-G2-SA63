package sg.edu.iss.cats.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.FeeClaim;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.FeeClaimRepository;

/** Source-backed financial ledger. Application fees use the course start year;
 * actual reimbursement disbursements use their payment date. Approved claims
 * do not count as reimbursements until an administrator records payment.
 * This is a financial projection, not a historical status-change audit trail. */
@Service
public class TrainingLedgerService {
    private final TrainingAccountService accounts;
    private final AppRepo applications;
    private final FeeClaimRepository claims;

    public TrainingLedgerService(TrainingAccountService accounts, AppRepo applications,
                                 FeeClaimRepository claims) {
        this.accounts = accounts;
        this.applications = applications;
        this.claims = claims;
    }

    public record Entry(LocalDate date, String category, String description,
                        BigDecimal approvedFee, BigDecimal reimbursement) {}
    public record Ledger(int year, TrainingAccountService.Summary allocation,
                         List<Entry> entries, BigDecimal approvedCourseFees,
                         BigDecimal reimbursementsPaid) {}

    @Transactional
    public Ledger forEmployee(User employee, int year) {
        if (year < 2000 || year > 9999) throw new IllegalArgumentException("Invalid ledger year");
        var allocation = accounts.summary(employee, year);
        List<Entry> entries = new ArrayList<>();
        BigDecimal approvedTotal = BigDecimal.ZERO;
        BigDecimal paidTotal = BigDecimal.ZERO;

        for (Application app : applications.findByUser_UserId(employee.getUserId())) {
            if (app.getStatus() != Status.APPROVED && app.getStatus() != Status.COMPLETED) continue;
            var course = app.getCourse();
            if (course.getStartDate().getYear() != year || course.getFee().signum() <= 0) continue;
            approvedTotal = approvedTotal.add(course.getFee());
            entries.add(new Entry(course.getStartDate(), "Approved course fee",
                    course.getCourseName() + " (application #" + app.getId() + ")",
                    course.getFee(), BigDecimal.ZERO));
        }
        for (FeeClaim claim : claims.findByEmployee_UserId(employee.getUserId())) {
            if (claim.getStatus() != FeeClaim.ClaimStatus.PAID || claim.getPaidOn() == null
                    || claim.getPaidOn().getYear() != year) continue;
            paidTotal = paidTotal.add(claim.getAmount());
            entries.add(new Entry(claim.getPaidOn(), "Reimbursement paid",
                    claim.getApplication().getCourse().getCourseName() + " (" + claim.getPaymentReference() + ")",
                    BigDecimal.ZERO, claim.getAmount()));
        }
        entries.sort(Comparator.comparing(Entry::date).thenComparing(Entry::category));
        return new Ledger(year, allocation, List.copyOf(entries), approvedTotal, paidTotal);
    }
}
