package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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


    @GetMapping("/form")
    public String showForm(Model model) {
		model.addAttribute("applicationForm", new Application());
        return "applicationform";
    }	
	
	@GetMapping("/courselist")
	public String showInternalCourses(Model model) {
		model.addAttribute("courselist", 
				courseRepository.findByCourseTypeOrderByStartDateAsc(CourseType.INTERNAL));
		
		return "courselist";
	}

	@GetMapping("/index")
	public String showIndex(){
		return "index";
	}
    
}
