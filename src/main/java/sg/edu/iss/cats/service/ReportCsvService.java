package sg.edu.iss.cats.service;

import java.time.LocalDate;
import java.util.function.IntFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.iss.cats.model.CourseType;

/** CSV exports of the exact datasets that the manager sees in the three report sections. */
@Service
public class ReportCsvService {
    private static final int BATCH = 200;
    private static final int MAX_ROWS = 20_000;
    private final TrainingReportService reports;
    public ReportCsvService(TrainingReportService reports) { this.reports = reports; }

    private <T> void writePages(StringBuilder out, IntFunction<Page<T>> loader,
            java.util.function.Consumer<T> addRow) {
        int count = 0;
        for (int page = 0; ; page++) {
            Page<T> batch = loader.apply(page);
            if (count + batch.getNumberOfElements() > MAX_ROWS)
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "CSV exceeds 20,000 rows; narrow the filters");
            for (T row : batch.getContent()) addRow.accept(row);
            count += batch.getNumberOfElements();
            if (!batch.hasNext()) return;
        }
    }

    public byte[] attendance(LocalDate first, LocalDate last, CourseType category, Integer employeeId) {
        StringBuilder csv = new StringBuilder();
        CsvEncoder.append(csv, "Employee ID", "Employee", "Course", "Category", "From", "To", "Status", "Course fee");
        writePages(csv, page -> reports.attendancePage(first, last, category, employeeId,
                PageRequest.of(page, BATCH)), a -> CsvEncoder.append(csv,
                    a.getUser().getUserId(), a.getUser().getName(), a.getCourse().getCourseName(),
                    a.getCourse().getCourseType(), a.getCourse().getStartDate(),
                    a.getCourse().getEndDate(), a.getStatus(), a.getCourse().getFee()));
        return CsvEncoder.bytes(csv);
    }

    public byte[] budgets(int year, Integer employeeId) {
        StringBuilder csv = new StringBuilder();
        CsvEncoder.append(csv, "Employee ID", "Employee", "Days limit", "Days committed", "Days completed",
                "Days remaining", "Budget allocation", "Fees committed", "Fees completed", "Budget remaining",
                "Claims paid", "Claims approved awaiting payment", "Claims pending");
        writePages(csv, page -> reports.utilisationPage(year, employeeId,
                PageRequest.of(page, BATCH, Sort.by("name"))), row -> {
                    TrainingAccountService.Summary s = row.getSummary();
                    CsvEncoder.append(csv, row.getEmployee().getUserId(), row.getEmployee().getName(),
                        s.getDayLimit(), s.getDaysCommitted(), s.getDaysCompleted(), s.getDaysRemaining(),
                        s.getFeeLimit(), s.getFeesCommitted(), s.getFeesCompleted(), s.getBudgetRemaining(),
                        row.getClaimsReimbursed(), row.getClaimsApprovedAwaitingPayment(), row.getClaimsPending());
                });
        return CsvEncoder.bytes(csv);
    }

    public byte[] claims(int year, Integer employeeId) {
        StringBuilder csv = new StringBuilder();
        CsvEncoder.append(csv, "Employee ID", "Employee", "Course", "Submitted", "Claim amount",
                "Status", "Manager reason", "Paid on", "Payment reference");
        writePages(csv, page -> reports.claimsPage(year, employeeId,
                PageRequest.of(page, BATCH)), claim -> CsvEncoder.append(csv,
                    claim.getEmployee().getUserId(), claim.getEmployee().getName(),
                    claim.getApplication().getCourse().getCourseName(), claim.getSubmittedOn(),
                    claim.getAmount(), claim.getStatus(), claim.getManagerReason(),
                    claim.getPaidOn(), claim.getPaymentReference()));
        return CsvEncoder.bytes(csv);
    }
}
