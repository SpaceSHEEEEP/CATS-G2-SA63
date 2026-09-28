package sg.edu.iss.cats.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.repository.CourseRepository;

@Controller
@RequestMapping("/staff") // for employee's stuff
public class EmployeeController {
	
	@Autowired
	private CourseRepository courseRepository;

	@GetMapping("/courselist")
	public String showInternalCourses(Model model) {
		model.addAttribute("courselist", 
				courseRepository.findByCourseTypeOrderByStartDateAsc(CourseType.INTERNAL));
		
		return "courselist";
	}
	
	@GetMapping("/employee/apply/{courseId}")
	public String showApplicationForm(@PathVariable int courseId, Model model) {
	    
		Course course = courseRepository.findById(courseId).orElse(null);

	    model.addAttribute("course", course);

	    return "applicationform";
	    
	    //need to add invalid id handling too
	    
	}
    
}
