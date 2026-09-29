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
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.service.ApplicationService;

@Controller
@RequestMapping("/staff")
public class ApplicationController {

    private final CourseRepository courseRepository;
    private final ApplicationService applicationService;

    public ApplicationController(CourseRepository courseRepository, ApplicationService applicationService) {
        this.courseRepository = courseRepository;
        this.applicationService = applicationService;
    }

	@GetMapping("/apply")
	// public String showApplicationForm(@PathVariable int courseId, Model model) {
	public String showApplicationForm(
            @RequestParam(name = "courseId", required = false) Integer courseId, 
            HttpSession session,
            Model model) 
    {
        Application app = new Application();

        if (session.getAttribute("user") != null) {
            // TODO: display user's details
            User user = (User) session.getAttribute("user");
            app.setUser(user);
            model.addAttribute("applicationForm", app);
        }

        if (courseId != null) {
            // TODO: prefill course's details, applicable only for INTERNAL courses
            Course course = courseRepository.findById(courseId).orElse(null);
            app.setCourse(course);
            model.addAttribute("applicationForm", app);
            model.addAttribute("course", course);
        } 

		model.addAttribute("applicationForm", app);
	    return "applicationform";
    }
    
    @PostMapping("/submitapplication")
    public String submitApplication(
            @Valid @ModelAttribute("applicationForm") Application applicationForm, 
            BindingResult result, 
            Model model, 
            HttpSession session) {

        User user = (User)session.getAttribute("user");
        if(user == null) {
            return "redirect:/login";
        }
        applicationForm.setUser(user);

        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) {
            // model.addAttribute("applicationForm", new Application());
            return "applicationform";
        }
        // Apply business validation rules in the service layer.
	    // TODO: add invalid id handling too, do it in service class

        // If it is an external course with no database ID
        if (applicationForm.getCourse() != null && applicationForm.getCourse().getCourseId() == null) {
            // Set course details to be shown in applied table
            applicationForm.setExternalCourseName(applicationForm.getCourse().getCourseName());
            applicationForm.setExternalCourseType(applicationForm.getCourse().getCourseType());
            applicationForm.setExternalDuration(applicationForm.getCourse().getDuration());
            applicationForm.setExternalStartDate(applicationForm.getCourse().getStartDate());
            applicationForm.setExternalEndDate(applicationForm.getCourse().getEndDate());
            applicationForm.setExternalLocation(applicationForm.getCourse().getLocation());
            applicationForm.setExternalTrainingProvider(applicationForm.getCourse().getTrainingProvider());
            applicationForm.setExternalFee(applicationForm.getCourse().getFee());
            applicationForm.setExternalDuration(applicationForm.getCourse().getDuration());
            // set it as null so it won't be saved into the course table
            applicationForm.setCourse(null);
        }
        applicationService.saveApplication(applicationForm);
        return "redirect:/staff/index";
    }
}
