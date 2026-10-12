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
    private final CourseReferenceService references;

    public AdminManagementController(AdminStaffService staff,
            CourseCatalogueService catalogue, CourseRepository courses, CourseReferenceService references) {
        this.staff = staff; this.catalogue = catalogue; this.courses = courses; this.references = references;
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

    private void setupCourseForm(Model model) {
        model.addAttribute("categoryOptions", references.enabledCategories());
        model.addAttribute("providerOptions", references.enabledProviders());
    }

    @PostMapping("/purgeuser")
    public String permanentlyDeleteUser(@AuthenticationPrincipal User admin,
            @RequestParam Integer userId, RedirectAttributes ra) {
        try { staff.deleteUnused(userId, admin.getUserId());
            ra.addFlashAttribute("successmsg", "Unused employee permanently deleted"); }
        catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/admin/index";
    }

    @GetMapping("/course/new")
    public String newCourse(Model model) {
        Course course = new Course(); course.setDuration(CourseDuration.FULLDAY);
        course.setFee(BigDecimal.ZERO);
        model.addAttribute("courseForm", course);
        setupCourseForm(model);
        return "courseform";
    }

    @GetMapping("/course/edit")
    public String editCourse(@RequestParam Integer courseId, Model model) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        course.setProviderId(course.getProvider() == null ? null : course.getProvider().getId());
        model.addAttribute("courseForm", course);
        setupCourseForm(model);
        return "courseform";
    }

    @PostMapping("/course/save")
    public String saveCourse(@Valid @ModelAttribute("courseForm") Course course,
                             BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) { setupCourseForm(model); return "courseform"; }
        try {
            catalogue.save(course);
            ra.addFlashAttribute("successmsg", "Course catalogue updated");
            return "redirect:/admin/index";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            setupCourseForm(model);
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
