package sg.edu.iss.cats.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.ApplicationRepository;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.service.ApplicationService;

@Controller
@RequestMapping("/staff")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;

    public ApplicationController(
            ApplicationRepository applicationRepository,
            CourseRepository courseRepository, 
            ApplicationService applicationService,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.courseRepository = courseRepository;
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

	@GetMapping("/apply")
	// public String showApplicationForm(@PathVariable int courseId, Model model) {
	public String showForm(
            @RequestParam(name = "courseId", required = false) Integer courseId, 
            HttpSession session,
            Model model) {

        Application app = new Application();

        // Can only apply if logged in
        if (session.getAttribute("user") == null) return "redirect:/staff/login";
        else {
            User sessionUser = (User) session.getAttribute("user");
            app.setUser(userRepository.findById(sessionUser.getUserId()).orElse(null));
        }

        // If got here via "apply" in internal courses list, do this to prefill form
        if (courseId != null) app.setCourse(courseRepository.findById(courseId).orElse(null));

		model.addAttribute("applicationForm", app);
	    return "applicationform";
    }
    
    @PostMapping("/submitapplication")
    public String submitForm(
            @Valid @ModelAttribute("applicationForm") Application form, 
            BindingResult result, 
            Model model, 
            RedirectAttributes ra,
            HttpSession session) {

        // Can only apply if logged in
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        form.setUser(userRepository.findById(sessionUser.getUserId()).orElse(null));
        // need the above because spring mvc remakes a new object after every state change

        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) return "applicationform";

        try {
            applicationService.saveApplication(form, sessionUser.getUserId());
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "applicationform";
        }

        // do this to trigger countPendingApplications() again, so get most updated info
        sessionUser = userRepository.findById(sessionUser.getUserId()).orElse(null);
        session.setAttribute("user", sessionUser); 

        return "redirect:/staff/index";
    }

    @GetMapping("/edit")
    public String editForm(
            @RequestParam(name = "applicationId", required = true) Integer applicationId,
            Model model,
            HttpSession session) {

        // check if user is logged in
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/staff/login";

        // check if the application exists and that this user made the application
        List<Application> applications = applicationRepository.findByUser_UserIdAndApplicationId(user.getUserId(), applicationId);
        if (applications.isEmpty()) return "redirect:/staff/index";

        // else, application exists
        model.addAttribute("applicationForm", applications.get(0));
        return "applicationform";
    }
}
