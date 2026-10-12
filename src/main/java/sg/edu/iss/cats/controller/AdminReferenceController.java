package sg.edu.iss.cats.controller;

import sg.edu.iss.cats.security.ActionConfirmation;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.service.CourseReferenceService;
import sg.edu.iss.cats.service.HolidayManagementService;

/** Restricted under /admin/** by Spring Security. All mutations are POST+CSRF protected. */
@Controller
@RequestMapping("/admin/references")
public class AdminReferenceController {
    private final CourseReferenceService references;
    private final HolidayManagementService holidayService;
    private final HolidayRepository holidays;

    public AdminReferenceController(CourseReferenceService references,
            HolidayManagementService holidayService, HolidayRepository holidays) {
        this.references = references; this.holidayService = holidayService; this.holidays = holidays;
    }

    @GetMapping
    public String show(@RequestParam(required = false) Integer year, Model model) {
        int selected = year == null ? LocalDate.now(ZoneId.of("Asia/Singapore")).getYear() : year;
        if (selected < 2000 || selected > 9999) throw new IllegalArgumentException("Invalid calendar year");
        model.addAttribute("categories", references.categories());
        model.addAttribute("providers", references.providers());
        model.addAttribute("holidays", holidays.findByHolidayDateBetweenOrderByHolidayDateAsc(
                LocalDate.of(selected, 1, 1), LocalDate.of(selected, 12, 31)));
        model.addAttribute("year", selected);
        return "references";
    }

    @PostMapping("/category")
    public String updateCategory(@RequestParam CourseType code, @RequestParam String displayName,
            @RequestParam(defaultValue = "false") boolean enabled, RedirectAttributes ra) {
        try { references.updateCategory(code, displayName, enabled);
            ra.addFlashAttribute("successmsg", "Course category updated"); }
        catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/admin/references";
    }

    @PostMapping("/provider")
    public String saveProvider(@RequestParam(required = false) Integer id, @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean enabled, RedirectAttributes ra) {
        try { references.saveProvider(id, name, enabled);
            ra.addFlashAttribute("successmsg", "Provider saved"); }
        catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/admin/references";
    }

    @PostMapping("/provider/delete")
    public String deleteProvider(@RequestParam Integer id, @RequestParam String confirmation, RedirectAttributes ra) {
        try { ActionConfirmation.require(confirmation, "DELETE");
            references.deleteProvider(id); ra.addFlashAttribute("successmsg", "Provider deleted"); }
        catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/admin/references";
    }

    @PostMapping("/holiday")
    public String saveHoliday(@RequestParam(required = false) Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate holidayDate,
            @RequestParam String holidayName, RedirectAttributes ra) {
        try { holidayService.save(id, holidayDate, holidayName);
            ra.addFlashAttribute("successmsg", "Holiday saved"); }
        catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/admin/references?year=" + holidayDate.getYear();
    }

    @PostMapping("/holiday/delete")
    public String deleteHoliday(@RequestParam Integer id, @RequestParam int year,
            @RequestParam String confirmation, RedirectAttributes ra) {
        try { ActionConfirmation.require(confirmation, "DELETE");
            holidayService.delete(id); ra.addFlashAttribute("successmsg", "Holiday deleted"); }
        catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/admin/references?year=" + year;
    }
}
