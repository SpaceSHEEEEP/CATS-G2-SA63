package sg.edu.iss.cats.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.ApplicationRepository;

@Controller
@RequestMapping("/staff") // for employee's stuff
public class EmployeeController {

    private final int pageSize = 10;
	private final CourseRepository courseRepository;
	private final ApplicationRepository applicationRepository;
	private final UserRepository userRepository;

	public EmployeeController(CourseRepository courseRepository, ApplicationRepository applicationRepository,
			UserRepository userRepository) {
		this.courseRepository = courseRepository;
		this.applicationRepository = applicationRepository;
		this.userRepository = userRepository;
	}

	@GetMapping("/index")
	public String showIndex(Model model, HttpSession session){

        // needs to be logged in
		User user = (User) session.getAttribute("user");
		if (user == null) return "redirect:/staff/login";
		
		List<Application> applications = applicationRepository.findByUser_UserId(user.getUserId());
		model.addAttribute("applications", applications);
		
		// Show Team Training only when employees report to this user.
		model.addAttribute("isManager", userRepository.existsByManager_UserId(user.getUserId()));
		
		return "index";
	}

    @GetMapping ("/courselist")
    public String showInternalCoursesPages(
            @RequestParam(name = "pageNum", defaultValue = "0") int pageNum,
            Model model) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<Course> coursePage = courseRepository.findByCourseTypeOrderByStartDateAsc(CourseType.INTERNAL, pageable);
        model.addAttribute("courselist", coursePage);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageNumLast", coursePage.getTotalPages());
        return "courselist";
    }

}
