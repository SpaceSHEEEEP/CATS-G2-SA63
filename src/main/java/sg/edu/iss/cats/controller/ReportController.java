package sg.edu.iss.cats.controller;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.ArrayList;
import java.util.function.IntFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import sg.edu.iss.cats.service.ReportCsvService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.service.TrainingReportService;

@Controller
@RequestMapping("/manager/reports")
public class ReportController {
    private final TrainingReportService reports;
    private final ReportCsvService exports;
    public ReportController(TrainingReportService reports, ReportCsvService exports) {
        this.reports = reports; this.exports = exports;
    }

    @GetMapping
    public String reports(@AuthenticationPrincipal User manager,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) CourseType category,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int attendancePage,
            @RequestParam(defaultValue = "0") int claimsPage,
            @RequestParam(defaultValue = "0") int budgetPage,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            return "redirect:/staff/index";
        int selectedYear = year == null ? Year.now(java.time.ZoneId.of("Asia/Singapore")).getValue() : year;
        LocalDate from = startDate == null ? LocalDate.of(selectedYear, 1, 1) : startDate;
        LocalDate to = endDate == null ? LocalDate.of(selectedYear, 12, 31) : endDate;
        pageSize = List.of(10, 20, 25).contains(pageSize) ? pageSize : 10;
        attendancePage = Math.max(0, attendancePage);
        claimsPage = Math.max(0, claimsPage);
        budgetPage = Math.max(0, budgetPage);
        try {
            Page<Application> attendance = reports.attendancePage(from, to, category, employeeId,
                    PageRequest.of(attendancePage, pageSize));
            Page<FeeClaim> claims = reports.claimsPage(selectedYear, employeeId,
                    PageRequest.of(claimsPage, pageSize));
            Page<TrainingReportService.BudgetRow> budgets = reports.utilisationPage(
                    selectedYear, employeeId,
                    PageRequest.of(budgetPage, pageSize, Sort.by("name")));
            model.addAttribute("attendees", attendance.getContent());
            model.addAttribute("claims", claims.getContent());
            model.addAttribute("budgets", budgets.getContent());
            model.addAttribute("attendanceTotal", attendance.getTotalPages());
            model.addAttribute("claimsTotal", claims.getTotalPages());
            model.addAttribute("budgetTotal", budgets.getTotalPages());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("attendees", List.of());
            model.addAttribute("claims", List.of());
            model.addAttribute("budgets", List.of());
            model.addAttribute("attendanceTotal", 0);
            model.addAttribute("claimsTotal", 0);
            model.addAttribute("budgetTotal", 0);
        }
        model.addAttribute("attendancePage", attendancePage);
        model.addAttribute("claimsPage", claimsPage);
        model.addAttribute("budgetPage", budgetPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("from", from); model.addAttribute("to", to);
        model.addAttribute("category", category); model.addAttribute("employeeId", employeeId);
        model.addAttribute("year", selectedYear);
        return "reports";
    }

    /** Print the complete selected report, bounded to avoid unbounded memory use. */
    private <T> List<T> allForPrint(IntFunction<Page<T>> loadPage) {
        List<T> rows = new ArrayList<>();
        int page = 0;
        while (true) {
            Page<T> slice = loadPage.apply(page++);
            if (rows.size() + slice.getNumberOfElements() > 1000)
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Printout exceeds 1,000 rows; narrow the reporting filters");
            rows.addAll(slice.getContent());
            if (!slice.hasNext()) return rows;
        }
    }

    @GetMapping("/print")
    public String print(@AuthenticationPrincipal User manager,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) CourseType category,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer year, Model model) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        int selectedYear = year == null ? Year.now(java.time.ZoneId.of("Asia/Singapore")).getValue() : year;
        if (selectedYear < 2000 || selectedYear > 9999)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reporting year");
        LocalDate from = startDate == null ? LocalDate.of(selectedYear, 1, 1) : startDate;
        LocalDate to = endDate == null ? LocalDate.of(selectedYear, 12, 31) : endDate;
        if (selectedYear < 2000 || selectedYear > 9999 || to.isBefore(from))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reporting period");
        model.addAttribute("attendees", allForPrint(i -> reports.attendancePage(
                from, to, category, employeeId, PageRequest.of(i, 100))));
        model.addAttribute("claims", allForPrint(i -> reports.claimsPage(
                selectedYear, employeeId, PageRequest.of(i, 100))));
        model.addAttribute("budgets", allForPrint(i -> reports.utilisationPage(
                selectedYear, employeeId, PageRequest.of(i, 100, Sort.by("name")))));
        model.addAttribute("from", from); model.addAttribute("to", to);
        model.addAttribute("year", selectedYear);
        return "printreports";
    }

    private ResponseEntity<byte[]> download(String filename, byte[] csv) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(csv);
    }

    @GetMapping("/attendance.csv")
    public ResponseEntity<byte[]> attendanceCsv(@AuthenticationPrincipal User manager,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) CourseType category,
            @RequestParam(required = false) Integer employeeId) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (endDate.isBefore(startDate)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        return download("training-attendance.csv", exports.attendance(startDate, endDate, category, employeeId));
    }

    @GetMapping("/budget.csv")
    public ResponseEntity<byte[]> budgetCsv(@AuthenticationPrincipal User manager,
            @RequestParam int year, @RequestParam(required = false) Integer employeeId) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (year < 2000 || year > 9999) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return download("training-budget.csv", exports.budgets(year, employeeId));
    }

    @GetMapping("/claims.csv")
    public ResponseEntity<byte[]> claimsCsv(@AuthenticationPrincipal User manager,
            @RequestParam int year, @RequestParam(required = false) Integer employeeId) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (year < 2000 || year > 9999) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return download("training-claims.csv", exports.claims(year, employeeId));
    }

}
