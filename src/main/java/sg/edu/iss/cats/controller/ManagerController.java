package sg.edu.iss.cats.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.UserRepository;
import sg.edu.iss.cats.service.ApplicationWorkflowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.LinkedHashMap;

@Controller 
@RequestMapping("/manager") // for manager stuff
public class ManagerController {

    private final UserRepository userRepository;
    private final AppRepo appRepo;
    private final ApplicationWorkflowService workflows;

    public ManagerController(UserRepository userRepository, AppRepo appRepo,
                             ApplicationWorkflowService workflows) {
        this.userRepository = userRepository;
        this.appRepo = appRepo;
        this.workflows = workflows;
    }

    private int safeSize(int requested) { return List.of(10, 20, 25).contains(requested) ? requested : 10; }

    @GetMapping("/applications")
    public String showApplications(@AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model) {
        if (user == null || user.getRole() != Role.ROLE_MANAGER) return "redirect:/staff/index";
        pageSize = safeSize(pageSize);
        pageNum = Math.max(0, pageNum);
        Page<Application> page = appRepo.findByUser_Manager_UserIdAndStatusIn(user.getUserId(),
                List.of(Status.APPLIED, Status.UPDATED), PageRequest.of(pageNum, pageSize));
        Map<String, List<Application>> grouped = new LinkedHashMap<>();
        page.getContent().stream()
                .sorted(java.util.Comparator.comparing(a -> a.getUser().getName()))
                .forEach(a -> grouped.computeIfAbsent(a.getUser().getName(), key -> new ArrayList<>()).add(a));
        model.addAttribute("pendingGroups", grouped);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageSizes", List.of(10, 20, 25));
        model.addAttribute("totalPages", page.getTotalPages());
        return "pendingapplications";
    }
	
    @GetMapping("/history")
    public String showSubordinateHistory(@AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model) {
        if (user == null || user.getRole() != Role.ROLE_MANAGER) return "redirect:/staff/index";
        int year = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Singapore")).getYear();
        pageSize = safeSize(pageSize);
        pageNum = Math.max(0, pageNum);
        Page<Application> page = appRepo.findCurrentYearTeam(user.getUserId(),
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31),
                PageRequest.of(pageNum, pageSize));
        Map<String, List<Application>> grouped = new LinkedHashMap<>();
        for (Application application : page.getContent()) {
            grouped.computeIfAbsent(application.getUser().getName(), key -> new ArrayList<>()).add(application);
        }
        model.addAttribute("subordinateGroups", grouped);
        model.addAttribute("year", year);
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageSizes", List.of(10, 20, 25));
        model.addAttribute("totalPages", page.getTotalPages());
        return "history";
    }

    @GetMapping("/applications/search")
    public String searchApplications(@AuthenticationPrincipal User manager,
            @RequestParam(defaultValue = "") String employeeName,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "ALL") String courseType,
            @RequestParam(defaultValue = "0") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            return "redirect:/staff/index";
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            model.addAttribute("error", "End date must not precede start date");
        }
        sg.edu.iss.cats.model.CourseType category;
        try {
            category = courseType.equals("ALL") ? null : sg.edu.iss.cats.model.CourseType.valueOf(courseType);
        } catch (IllegalArgumentException ex) {
            category = null;
        }
        pageSize = safeSize(pageSize);
        pageNum = Math.max(0, pageNum);
        Page<Application> page = appRepo.searchTeam(manager.getUserId(), employeeName,
                startDate, endDate, category, PageRequest.of(pageNum, pageSize));
        model.addAttribute("searchApplications", page.getContent());
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageSizes", List.of(10, 20, 25));
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("employeeName", employeeName);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("courseType", courseType);
        return "search";
    }
	
    @PostMapping("/application/status")
    public String changeStatus(@AuthenticationPrincipal User manager,
            @RequestParam("id") Integer id,
            @RequestParam("managerReason") String reason,
            @RequestParam("status") String status,
            RedirectAttributes ra) {
        if (manager == null || manager.getRole() != Role.ROLE_MANAGER)
            return "redirect:/staff/index";
        try {
            workflows.decide(manager.getUserId(), id, Status.valueOf(status), reason);
            ra.addFlashAttribute("successmsg", "Application " + id + " " + status.toLowerCase());
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/manager/applications";
    }
}
