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
import sg.edu.iss.cats.service.ApplicationService;

@Controller
@RequestMapping("/staff")
public class ApplicationController {

    private final AppRepo appRepo;
    private final CourseRepository courseRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;

    public ApplicationController(
            AppRepo appRepo,
            CourseRepository courseRepository, 
            CommentRepository commentRepository, 
            ApplicationService applicationService,
            UserRepository userRepository) {
        this.appRepo = appRepo;
        this.courseRepository = courseRepository;
        this.commentRepository = commentRepository;
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

	@GetMapping("/apply")
	public String showForm(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "courseId", required = false) Integer courseId, 
            Model model) {

        if (user == null) return "redirect:/staff/login";

        Application app = new Application();
        app.setUser(userRepository.findById(user.getUserId()).orElse(null));
        if (courseId != null) app.setCourse(courseRepository.findById(courseId).orElse(null));
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
        if (result.hasErrors()) return "applyform";
        // Check for valid annotations in the Application, Course Model.

        try {
            applicationService.saveApplication(form, user.getUserId());
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "applyform";
        }

        ra.addFlashAttribute("successmsg", "Application '" + form.getId() + "' submitted successfully!");
        return "redirect:/staff/index";
    }

    @GetMapping("/edit")
    public String editForm(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "id", required = true) Integer id,
            Model model) {

        // check if user is logged in
        if (user == null) return "redirect:/staff/login";
        List<Application> applications = appRepo.findByUser_UserIdAndId(user.getUserId(), id);
        if (applications.isEmpty()) return "redirect:/staff/index";

        // else, application exists
        model.addAttribute("applicationForm", applications.get(0));
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
            model.addAttribute("comment", new Comment(applications.get(0).getCourse(), user));
            return "applyresult";
        }

        // if the application isnt mine but its my subordinates, and im the manager,
        Application app = appRepo.findById(id).orElse(null);
        if (app == null) return "redirect:/staff/index"; // cant find app
        List<User> subordinates = user.getSubordinates();
        for (User sub : subordinates) {
            if (app.getUser().getUserId().equals(sub.getUserId())) {
                model.addAttribute("applicationResult", app);
                model.addAttribute("viewer", "manager");
                return "applyresult";
            }
        }
		
        // its not mine nor my subordinates'
        return "redirect:/staff/index";
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
    public String completedApplication(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "id", required = true) Integer id,
            @RequestParam(name = "commentText") String commentText,
            Model model,
            RedirectAttributes ra) {
		    
    	if (user == null) return "redirect:/staff/login";
		
		List<Application> applications = appRepo.findByUser_UserIdAndId(user.getUserId(), id);
		if (applications.isEmpty()) return "redirect:/staff/index";

	    Application completedApplication = applications.get(0);
	    
        // included try-catch for the controller for exception handling from the service
        // checks that the application status is APPROVED first
        if (completedApplication.getStatus() == Status.APPROVED){
            try {
                applicationService.completeApplication(completedApplication, user.getUserId());

                Comment comment = new Comment(completedApplication.getCourse(), user);
                comment.setCommentText(commentText);
                System.out.println("DEBUG: " + comment);
                commentRepository.save(comment);
            } catch (RuntimeException e) {
                ra.addAttribute("errormsg", e.getMessage());
                return "redirect:/staff/index";
            }
        }

        // Flash Attribute for success message
        // TODO: this stopped working?
        ra.addFlashAttribute("successmsg", "Application '" + id + "' is marked as COMPLETED successfully!");
        
        return "redirect:/staff/index";
    }
    
}
