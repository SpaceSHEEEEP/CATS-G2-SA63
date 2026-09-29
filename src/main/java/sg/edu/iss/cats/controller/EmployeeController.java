package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.repository.CourseRepository;

@Controller
@RequestMapping("/staff") // for employee's stuff
public class EmployeeController {

	private final CourseRepository courseRepository;

	public EmployeeController(CourseRepository courseRepository) {
		this.courseRepository = courseRepository;
	}

	@GetMapping("/index")
	public String showIndex(){
		return "index";
	}

	@GetMapping("/courselist")
	public String showInternalCourses(Model model) {
		model.addAttribute("courselist", 
				courseRepository.findByCourseTypeOrderByStartDateAsc(CourseType.INTERNAL));
		return "courselist";
	}
	
	@GetMapping("/apply")
	// public String showApplicationForm(@PathVariable int courseId, Model model) {
	public String showApplicationForm(
            @RequestParam(name = "courseId", required = false) Integer courseId, 
            HttpSession session,
            Model model) 
    {
        if (session.getAttribute("user") != null) {
            // TODO: display user's details
        }

        if (courseId != null) {
            // TODO: prefill course's details, applicable only for INTERNAL courses
            Course course = courseRepository.findById(courseId).orElse(null);
            model.addAttribute("course", course);
        } 

		model.addAttribute("applicationForm", new Application());
	    return "applicationform";
	    
	    // TODO: add invalid id handling too
    }
	    
    @Deprecated
    @GetMapping("/form") // depreciated. use @GetMapping("/apply")
    public String showForm(Model model) {
		model.addAttribute("applicationForm", new Application());
        return "applicationform";
    }	
    
    @PostMapping("/submitapplication")
    public String submitApplication(
            @Valid @ModelAttribute("applicationForm") Application applicationForm, 
            BindingResult result, 
            Model model) {
        // Check for valid annotations in the Application, Course Model.
        if (result.hasErrors()) {
            // model.addAttribute("applicationForm", new Application());
            return "applicationform";
        }
        // Apply business validation rules in the service layer.

        return "redirect:/staff/index";
    }

}
