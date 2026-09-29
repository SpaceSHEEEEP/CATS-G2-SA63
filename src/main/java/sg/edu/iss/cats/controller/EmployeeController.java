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
	

}
