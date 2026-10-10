package sg.edu.iss.cats.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.AppRepo;

@Controller
@RequestMapping("/staff") // for employee's stuff
public class EmployeeController {

    private final int pageSize = 10;
	private final CourseRepository courseRepository;
	private final AppRepo appRepo;
	private final UserRepository userRepository;

	public EmployeeController(
            CourseRepository courseRepository, 
            AppRepo appRepo,
			UserRepository userRepository) {
		this.courseRepository = courseRepository;
		this.appRepo = appRepo;
		this.userRepository = userRepository;
	}

	@GetMapping("/index")
	public String showIndex(
            @AuthenticationPrincipal User user, 
            Model model) {

        if (user == null) return "redirect:/staff/login";
    
        List<Application> applications = appRepo.findByUser_UserId(user.getUserId());
        model.addAttribute("applications", applications);
        model.addAttribute("isManager", userRepository.existsByManager_UserId(user.getUserId()));
        model.addAttribute("user", userRepository.findById(user.getUserId()).orElseThrow());

        return "index";
	}

    @GetMapping ("/courselist")
    public String showInternalCoursesPages(
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "pageNum", defaultValue = "0") int pageNum,
            Model model) {

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        List<CourseType> courseTypes = new ArrayList<>();

        switch (type) {
            case "INTERNAL":
                courseTypes.add(CourseType.INTERNAL);
                break;
            case "EXTERNAL":
                courseTypes.add(CourseType.EXTERNAL);
                break;
            case "PROFESSIONAL":
                courseTypes.add(CourseType.PROFESSIONAL);
                break;
            default:
                courseTypes.addAll(List.of(CourseType.INTERNAL, CourseType.EXTERNAL, CourseType.PROFESSIONAL));
                break;
        }

        Page<Course> coursePage = courseRepository.findByCourseTypeInOrderByStartDateAsc(courseTypes, pageable);
        model.addAttribute("courses", coursePage);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageNumLast", coursePage.getTotalPages());
        return "courselist";
    }

}
