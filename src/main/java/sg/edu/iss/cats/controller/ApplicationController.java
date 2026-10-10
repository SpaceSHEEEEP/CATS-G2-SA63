package sg.edu.iss.cats.controller;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Comment;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.CommentRepository;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.service.ApplicationWorkflowService;
import sg.edu.iss.cats.service.TrainingAccountService;
import sg.edu.iss.cats.model.Role;

@Controller
@RequestMapping("/staff")
public class ApplicationController {

    private final AppRepo appRepo;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ApplicationWorkflowService applicationService;
    private final TrainingAccountService accounts;

    public ApplicationController(
            AppRepo appRepo,
            CourseRepository courseRepository,
            ApplicationWorkflowService applicationService,
            UserRepository userRepository,
            TrainingAccountService accounts) {
        this.appRepo = appRepo;
        this.courseRepository = courseRepository;
        this.applicationService = applicationService;
        this.userRepository = userRepository;
        this.accounts = accounts;
    }

	@GetMapping("/apply")
	public String showForm(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "courseId", required = false) Integer courseId, 
            Model model) {

        if (user == null) return "redirect:/staff/login";

        Application app = new Application();
        app.setUser(userRepository.findById(user.getUserId()).orElse(null));
        Course selectedCourse = courseId == null ? null : courseRepository.findById(courseId).orElse(null);
        if (selectedCourse != null && !Boolean.TRUE.equals(selectedCourse.getArchived())) {
            app.setCourse(selectedCourse);
        } else {
            Course custom = new Course();
            custom.setDuration(sg.edu.iss.cats.model.CourseDuration.FULLDAY);
            custom.setFee(java.math.BigDecimal.ZERO);
            app.setCourse(custom);
        }
        model.addAttribute("applicationForm", app);
	    return "applyform";
    }
    
    @PostMapping("/submitapplication")
    public String submitForm(
            @AuthenticationPrincipal User user,
            @Valid @ModelAttribute("applicationForm") Application form, 
            BindingResult result, 
            Model model, 
            RedirectAttributes ra) {

        if (user == null) return "redirect:/login";

        // need below because spring mvc remakes a new object after every state change
        form.setUser(userRepository.findById(user.getUserId()).orElse(null));
        // Bean validation runs before the transactional business checks.
        if (result.hasErrors()) return "applyform";

         // Check if it is a new application or an existing application (editing)
        boolean isNewApp = (form.getId()==null);

        try {
            applicationService.saveApplication(form, user.getUserId());
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "applyform";
        }

        if (isNewApp) {
            ra.addFlashAttribute("successmsg", "Application '" + form.getId() + "' submitted successfully!");
        } else {
            ra.addFlashAttribute("successmsg", "Application '" + form.getId() + "' updated successfully!");
        }
        return "redirect:/staff/index";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal User user,
            @RequestParam("id") Integer id, Model model, RedirectAttributes ra) {
        if (user == null) return "redirect:/staff/login";
        List<Application> applications = appRepo.findByUser_UserIdAndId(user.getUserId(), id);
        if (applications.isEmpty()) return "redirect:/staff/index";
        Application application = applications.get(0);
        if (application.getStatus() != Status.APPLIED && application.getStatus() != Status.UPDATED) {
            ra.addFlashAttribute("errormsg", "Only applied or updated applications may be edited");
            return "redirect:/staff/index";
        }
        model.addAttribute("applicationForm", application);
        return "applyform";
    }

    @GetMapping("/view")
    public String viewApplication(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "id", required = true) Integer id,
            Model model) {
		    
    	if (user == null) return "redirect:/staff/login";
		
        // TODO: leave better comments
        // TODO: maybe rewrite this
        List<Application> applications = appRepo.findByUser_UserIdAndId(user.getUserId(), id);
        if (!applications.isEmpty()) {
            model.addAttribute("applicationResult", applications.get(0));
            model.addAttribute("viewer", "mine");
            model.addAttribute("canComplete", applications.get(0).getStatus() == Status.APPROVED
                    && applications.get(0).getCourse().getEndDate().isBefore(
                       java.time.LocalDate.now(java.time.ZoneId.of("Asia/Singapore"))));
            // TODO: fix this!
            if (applications.get(0).getStatus() != Status.COMPLETED) model.addAttribute("comment", new Comment(applications.get(0)));
            else {
                model.addAttribute("comment", applications.get(0).getComment());
                System.out.println("DEBUG: added the comment " + applications.get(0).getComment());
            }
            return "applyresult";
        }

        // Manager-only subordinate access, checked from the persisted hierarchy.
        Application app = appRepo.findById(id).orElse(null);
        if (app == null || user.getRole() != Role.ROLE_MANAGER
                || app.getUser().getManager() == null
                || !app.getUser().getManager().getUserId().equals(user.getUserId())) {
            return "redirect:/staff/index";
        }
        var budget = accounts.summary(app.getUser(), java.time.LocalDate.now().getYear());
        model.addAttribute("approvalBudget", budget);
        var overlaps = appRepo.findByUser_Manager_UserIdOrderByUser_NameAscIdAsc(user.getUserId()).stream()
                .filter(other -> !other.getId().equals(app.getId()))
                .filter(other -> !other.getUser().getUserId().equals(app.getUser().getUserId()))
                .filter(other -> other.getStatus() == Status.APPROVED)
                .filter(other -> !other.getCourse().getStartDate().isAfter(app.getCourse().getEndDate())
                        && !other.getCourse().getEndDate().isBefore(app.getCourse().getStartDate()))
                .toList();
        model.addAttribute("overlappingApprovedCourses", overlaps);
        model.addAttribute("applicationResult", app);
        model.addAttribute("viewer", "manager");
        return "applyresult";
    }

    @PostMapping("/delete")
    public String deleteApplication(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "id", required = true) Integer id,
            Model model,
            RedirectAttributes ra) {

        if (user == null) return "redirect:/staff/login";

        List<Application> applications = appRepo.findByUser_UserIdAndId(user.getUserId(), id);
        if (applications.isEmpty()) return "redirect:/staff/index";
        // else, application exists, delete it
        // included try-catch for the controller for exception handling from the service
         try {
            applicationService.deleteApplication(applications.get(0), user.getUserId());
        } catch (RuntimeException e) {
            ra.addAttribute("error", e.getMessage());
            return "redirect:/staff/index";
        }
            
        ra.addFlashAttribute("successmsg", "Application '" + id + "' deleted successfully!");
        return "redirect:/staff/index";
    }
    
    @PostMapping("/completed")
    public String completedApplication(@AuthenticationPrincipal User user,
            @RequestParam("id") Integer id,
            @RequestParam("commentText") String commentText,
            RedirectAttributes ra) {
        if (user == null) return "redirect:/staff/login";
        try {
            applicationService.completeApplication(id, user.getUserId(), commentText);
            ra.addFlashAttribute("successmsg", "Application '" + id + "' completed successfully");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/staff/index";
    }

    @PostMapping("/cancel")
    public String cancelApplication(@AuthenticationPrincipal User user,
            @RequestParam("id") Integer id, RedirectAttributes ra) {
        if (user == null) return "redirect:/staff/login";
        try {
            applicationService.cancelApplication(id, user.getUserId());
            ra.addFlashAttribute("successmsg", "Application '" + id + "' cancelled");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/staff/index";
    }
    
}
