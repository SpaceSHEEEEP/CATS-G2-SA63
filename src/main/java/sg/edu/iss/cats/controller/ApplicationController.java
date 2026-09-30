package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    private final CourseRepository courseRepository;
    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    public ApplicationController(
            CourseRepository courseRepository, 
            ApplicationService applicationService,
            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

	@GetMapping("/apply")
	// public String showApplicationForm(@PathVariable int courseId, Model model) {
	public String showApplicationForm(
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
    public String submitApplication(
            @Valid @ModelAttribute("applicationForm") Application form, 
            BindingResult result, 
            Model model, 
            HttpSession session) {

        // Can only apply if logged in
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/login";
        form.setUser(userRepository.findById(sessionUser.getUserId()).orElse(null));
        // need the above because spring mvc remakes a new object after every state change

        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) return "applicationform";

        applicationService.saveApplication(form);
        return "redirect:/staff/index";
    }
}
