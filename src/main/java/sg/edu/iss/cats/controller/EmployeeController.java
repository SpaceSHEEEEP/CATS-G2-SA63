package sg.edu.iss.cats.controller;

import java.util.List;

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
import sg.edu.iss.cats.repository.ApplicationRepository;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.service.BudgetService;

@Controller
@RequestMapping("/staff") // for employee's stuff
public class EmployeeController {

	private final CourseRepository courseRepository;
	private final ApplicationRepository applicationRepository;
	private final BudgetService budgetService;

	public EmployeeController(CourseRepository courseRepository, ApplicationRepository applicationRepository,
			BudgetService budgetService) {
		this.courseRepository = courseRepository;
		this.applicationRepository = applicationRepository;
		this.budgetService = budgetService;
	}

	@GetMapping("/index")
	public String showIndex(Model model, HttpSession session){
		User user = (User) session.getAttribute("user");
		if (user == null) {
			return "redirect:/staff/login";
		}
		List<Application> applications = applicationRepository.findByUserIdWithCourse(user.getUserId());
		model.addAttribute("applications", applications);
		model.addAttribute("remainingBudget", budgetService.remainingBudget(user, applications));
		return "index";
	}

	@GetMapping("/courselist")
	public String showInternalCourses(Model model) {
		model.addAttribute("courselist", 
				courseRepository.findByCourseTypeOrderByStartDateAsc(CourseType.INTERNAL));
		return "courselist";
	}
	

}
