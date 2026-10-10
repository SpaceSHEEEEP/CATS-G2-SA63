package sg.edu.iss.cats.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import sg.edu.iss.cats.dto.CourseApplicantSummary;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.LoginForm;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import sg.edu.iss.cats.model.Role;
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

	@GetMapping("/index")
	public String showAdmin(
            @AuthenticationPrincipal User user,
            @RequestParam(name = "pageNum", defaultValue = "0") int pageNum,
            Model model) {

        if (user == null)                      return "redirect:/admin/login";
        if (user.getRole() != Role.ROLE_ADMIN) return "redirect:/staff/index";

        // Load current user records
        model.addAttribute("users", userRepository.findAll());
        
        // Show holidays for the current calendar year only.
        int year = LocalDate.now().getYear();
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);

        model.addAttribute("holidayYear", year);
        model.addAttribute("holidays",
                holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(firstDay, lastDay));

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        // TODO: get pages WITH application count!! The one below doesn't have application count
        // Page<Course> courseSummaries = courseRepository.findAllByOrderByApplicationsSizeDesc(pageable);
        // model.addAttribute("courseSummaries", courseSummaries);
        Page<CourseApplicantSummary> courseSummaries = courseRepository.findCoursesWithApplicantCounts(pageable);
        model.addAttribute("courseSummaries", courseSummaries);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageNumLast", courseSummaries.getTotalPages());
        model.addAttribute("user", userRepository.findById(user.getUserId()).orElseThrow());
        
        return "admin";
	}
	
	@GetMapping("/login")
	public String showAdminLogin(Model model) {
		model.addAttribute("login", new LoginForm());
		model.addAttribute("adminLogin", true); // TODO: check what this is for
		return "login";
	}
	
    // User and course maintenance handlers live in AdminManagementController.
    // This controller intentionally provides the read-only admin index and login only.
}
