package sg.edu.iss.cats.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.LoginForm;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;
import java.time.LocalDate;

import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.HolidayRepository;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
    private final int pageSize = 10;
	private final UserRepository userRepository;
	private final HolidayRepository holidayRepository;
	private final CourseRepository courseRepository;
	
	public AdminController(UserRepository userRepository, HolidayRepository holidayRepository, CourseRepository courseRepository) {
		this.userRepository = userRepository;
		this.holidayRepository = holidayRepository;
		this.courseRepository = courseRepository;

	}

	@GetMapping
	public String ShowAdmin(@RequestParam(name = "pageNum", defaultValue = "0") int pageNum, Model model, HttpSession session) {
		User user = (User) session.getAttribute("user");

        if (user == null) return "redirect:/admin/login";

        if (!user.isAdmin()) return "redirect:/staff/index";

        // Load current user records
        model.addAttribute("users", userRepository.findAll());
        
        // Show holidays for the current calendar year only.
        int year = LocalDate.now().getYear();
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);

        model.addAttribute("holidayYear", year);
        model.addAttribute("holidays",
                holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(
                        firstDay, lastDay));
        // model.addAttribute("courseSummaries",courseRepository.findCoursesWithApplicantCounts());

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // TODO: get pages WITH application count!! The one below doesn't have application count
        Page<Course> courseSummaries = courseRepository.findAllByOrderByApplicationsSizeDesc(pageable);
        model.addAttribute("courseSummaries", courseSummaries);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageNumLast", courseSummaries.getTotalPages());
        
        return "admin";
	}
	
	@GetMapping("/login")
	public String ShowAdminLogin(Model model) {
		model.addAttribute("login", new LoginForm());
		model.addAttribute("adminLogin", true);
		return "login";
	}
	
	@PostMapping("/login")
	public String processLogin( @ModelAttribute LoginForm loginForm, Model model, HttpSession session) {
		String username = loginForm.getUsername();
        String password = loginForm.getPassword();

        User user = null;

        // Check credentials before checking whether this is an admin account.
        if (username != null && password != null && userRepository.existsByUsernameAndPassword(username.trim(), password)) 
            user = userRepository.findByUsername(username.trim());

        if (user == null || !user.isAdmin()) {
            // Keep the page in admin mode after an unsuccessful attempt.
            model.addAttribute("login", new LoginForm());
            model.addAttribute("adminLogin", true);
            model.addAttribute("msg",
                    "Unable to log in with these administrator details.");
            return "login";
        }
        
        session.setAttribute("user", user);
        return "redirect:/admin";
	}
	
}
