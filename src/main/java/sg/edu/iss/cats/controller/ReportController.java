package sg.edu.iss.cats.controller;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
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
    public ReportController(TrainingReportService reports) { this.reports = reports; }

    @GetMapping
    public String reports(@AuthenticationPrincipal User manager,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) CourseType category,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer year,
            Model model) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            return "redirect:/staff/index";
        int selectedYear = year == null ? Year.now(java.time.ZoneId.of("Asia/Singapore")).getValue() : year;
        LocalDate from = startDate == null ? LocalDate.of(selectedYear, 1, 1) : startDate;
        LocalDate to = endDate == null ? LocalDate.of(selectedYear, 12, 31) : endDate;
        try {
            model.addAttribute("attendees", reports.attendance(manager.getUserId(), from, to, category, employeeId));
            model.addAttribute("claims", reports.claims(manager.getUserId(), selectedYear, employeeId));
            model.addAttribute("budgets", reports.utilisation(manager.getUserId(), selectedYear, employeeId));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("attendees", List.of());
            model.addAttribute("claims", List.of());
            model.addAttribute("budgets", List.of());
        }
        model.addAttribute("from", from); model.addAttribute("to", to);
        model.addAttribute("category", category); model.addAttribute("employeeId", employeeId);
        model.addAttribute("year", selectedYear);
        return "reports";
    }
}
