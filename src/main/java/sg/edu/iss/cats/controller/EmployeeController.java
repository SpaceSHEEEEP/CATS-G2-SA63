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
import sg.edu.iss.cats.service.TrainingAccountService;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/staff") // for employee's stuff
public class EmployeeController {

    private final CourseRepository courseRepository;
    private final AppRepo appRepo;
    private final UserRepository userRepository;
    private final TrainingAccountService accounts;

    public EmployeeController(CourseRepository courseRepository, AppRepo appRepo,
            UserRepository userRepository, TrainingAccountService accounts) {
        this.courseRepository = courseRepository;
        this.appRepo = appRepo;
        this.userRepository = userRepository;
        this.accounts = accounts;
    }

    private int safeSize(int requested) { return List.of(10, 20, 25).contains(requested) ? requested : 10; }

    @GetMapping("/index")
    public String showIndex(@AuthenticationPrincipal User user,
            @RequestParam(name = "pageNum", defaultValue = "0") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            Model model) {
        if (user == null) return "redirect:/staff/login";
        int year = LocalDate.now(ZoneId.of("Asia/Singapore")).getYear();
        pageSize = safeSize(pageSize);
        pageNum = Math.max(0, pageNum);
        Page<Application> history = appRepo.findCurrentYearPersonal(user.getUserId(),
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
                PageRequest.of(pageNum, pageSize));
        User persisted = userRepository.findById(user.getUserId()).orElseThrow();
        model.addAttribute("applications", history.getContent());
        model.addAttribute("trainingSummary", accounts.summary(persisted, year));
        model.addAttribute("pendingCount", appRepo.findByUser_UserIdAndStatusIn(user.getUserId(),
                List.of(sg.edu.iss.cats.model.Status.APPLIED, sg.edu.iss.cats.model.Status.UPDATED)).size());
        model.addAttribute("isManager", persisted.getRole() == sg.edu.iss.cats.model.Role.ROLE_MANAGER);
        model.addAttribute("user", persisted);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageSizes", List.of(10, 20, 25));
        model.addAttribute("totalPages", history.getTotalPages());
        model.addAttribute("year", year);
        return "index";
    }

    @GetMapping("/courselist")
    public String showInternalCoursesPages(
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "pageNum", defaultValue = "0") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            Model model) {
        pageSize = safeSize(pageSize);
        pageNum = Math.max(0, pageNum);
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

        Page<Course> coursePage = courseRepository.findActiveCoursesByType(courseTypes, pageable);
        model.addAttribute("courses", coursePage);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("selectedType", type);
        model.addAttribute("pageSizes", List.of(10, 20, 25));
        model.addAttribute("pageNumLast", coursePage.getTotalPages());
        return "courselist";
    }

}
