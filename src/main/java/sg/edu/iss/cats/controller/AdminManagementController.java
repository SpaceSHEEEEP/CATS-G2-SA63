package sg.edu.iss.cats.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import sg.edu.iss.cats.dto.AdminUserForm;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.service.*;

@Controller
@RequestMapping("/admin")
public class AdminManagementController {
    private final AdminStaffService staff;
    private final CourseCatalogueService catalogue;
    private final CourseRepository courses;

    public AdminManagementController(AdminStaffService staff,
            CourseCatalogueService catalogue, CourseRepository courses) {
        this.staff = staff; this.catalogue = catalogue; this.courses = courses;
    }

    private void setupStaffForm(Model model) { model.addAttribute("managers", staff.eligibleManagers()); }

    @GetMapping("/createuser")
    public String newUser(Model model) {
        AdminUserForm form = new AdminUserForm();
        form.setAnnualDays(10.0);
        form.setAnnualAllowance(new BigDecimal("2000.00"));
        form.setYear(LocalDate.now(ZoneId.of("Asia/Singapore")).getYear());
        model.addAttribute("userForm", form);
        setupStaffForm(model);
        return "userform";
    }

    @GetMapping("/edituser")
    public String editUser(@RequestParam Integer userId,
            @RequestParam(required = false) Integer year, Model model) {
        int selectedYear = year == null ? Year.now(ZoneId.of("Asia/Singapore")).getValue() : year;
        model.addAttribute("userForm", staff.formFor(userId, selectedYear));
        setupStaffForm(model);
        return "userform";
    }

    @PostMapping("/createuser")
    public String saveUser(@AuthenticationPrincipal User admin,
            @Valid @ModelAttribute("userForm") AdminUserForm form,
            BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) { setupStaffForm(model); return "userform"; }
        try {
            staff.save(form, admin.getUserId());
            ra.addFlashAttribute("successmsg", "Employee details saved");
            return "redirect:/admin/index";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            setupStaffForm(model);
            return "userform";
        }
    }

    @PostMapping("/deleteuser")
    public String deleteUser(@AuthenticationPrincipal User admin,
            @RequestParam Integer userId, RedirectAttributes ra) {
        try {
            staff.deactivate(userId, admin.getUserId());
            ra.addFlashAttribute("successmsg", "Employee deactivated; application history preserved");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/admin/index";
    }

    @GetMapping("/course/new")
    public String newCourse(Model model) {
        Course course = new Course(); course.setDuration(CourseDuration.FULLDAY);
        course.setFee(BigDecimal.ZERO);
        model.addAttribute("courseForm", course);
        return "courseform";
    }

    @GetMapping("/course/edit")
    public String editCourse(@RequestParam Integer courseId, Model model) {
        model.addAttribute("courseForm", courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found")));
        return "courseform";
    }

    @PostMapping("/course/save")
    public String saveCourse(@Valid @ModelAttribute("courseForm") Course course,
                             BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return "courseform";
        try {
            catalogue.save(course);
            ra.addFlashAttribute("successmsg", "Course catalogue updated");
            return "redirect:/admin/index";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "courseform";
        }
    }

    @PostMapping("/course/archive")
    public String archiveCourse(@RequestParam Integer courseId, RedirectAttributes ra) {
        try {
            catalogue.archive(courseId);
            ra.addFlashAttribute("successmsg", "Course archived and retained for application history");
        } catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/admin/index";
    }
}
