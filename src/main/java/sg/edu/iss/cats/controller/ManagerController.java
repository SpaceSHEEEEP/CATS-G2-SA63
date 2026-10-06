package sg.edu.iss.cats.controller;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.UserRepository;

@Controller 
@RequestMapping("/manager") // for manager stuff
public class ManagerController {

	private final UserRepository userRepository;
	private final AppRepo appRepo;

	public ManagerController(UserRepository userRepository, AppRepo appRepo) {
		this.userRepository = userRepository;
		this.appRepo = appRepo;
	}

	@GetMapping("/applications")
	public String showApplications(Model model, HttpSession session) {
		User user = (User) session.getAttribute("user");

        // needs to be logged in
		if (user == null) return "redirect:/staff/login";

        // needs to be manager
		if (!userRepository.existsByManager_UserId(user.getUserId())) return "redirect:/staff/index";

		List<Application> subordinateApplications = appRepo
				.findByUser_Manager_UserIdOrderByUser_NameAscIdAsc(user.getUserId());

		// Pending applications are the subset awaiting a manager's decision.
		List<Application> pendingApplications = new ArrayList<>();
		for (Application application : subordinateApplications) {
			if (application.getStatus() == Status.APPLIED ||
				application.getStatus() == Status.UPDATED) {
				pendingApplications.add(application);
			}
		}

		model.addAttribute("pendingApplications", pendingApplications);
        // TODO: change findByUser_Manager_UserIdOrderByUser_NameAscIdAsc so that it limits to 4, 
        // instead of me getting all applications, then limit to 4 like this. more performant
		model.addAttribute("subordinateApplications", subordinateApplications.subList(0, Math.min(subordinateApplications.size(), 4)));

		return "pendingapplications";
	}
	
    @GetMapping("/applicationhistory")
    public String showSubordinateHistory(Model model, HttpSession session) {

        // Need to be logged in
		User user = (User) session.getAttribute("user");
		if (user == null) return "redirect:/staff/login";

		// Need to be a manager
		if (!userRepository.existsByManager_UserId(user.getUserId())) return "redirect:/staff/index";

        // Give me a list of subordinates
        List<User> subordinates = userRepository.findAllByManager_UserId(user.getUserId());

        // for each subordinate, gimme a list of his/her applications
        List<List<Application>> subordinatesApplications = new ArrayList<>();
        for (User u : subordinates) {
            if (!appRepo.existsByUser_UserId(u.getUserId())) continue;
            subordinatesApplications.add(appRepo.findByUser_UserId(u.getUserId()));
        }
        model.addAttribute("subordinatesApplications", subordinatesApplications);

        return "applicationhistory";

    }

	@GetMapping("/applications/search")
	public String searchApplications(
	        @RequestParam(name = "employeeName") String employeeName,
					@RequestParam(name = "startDate", required = false) LocalDate startDate,
	        @RequestParam(name = "endDate", required = false) LocalDate endDate,
	        @RequestParam(name = "courseType") String courseType,
	        HttpSession session,
	        Model model) {

	    User manager = (User) session.getAttribute("user");
	    if (manager == null) return "redirect:/staff/login";

	    List<Application> applications = appRepo
            .findByUser_Manager_UserIdOrderByUser_NameAscIdAsc(manager.getUserId());

	    List<Application> searchApplications = new ArrayList<>();

			boolean startDateEmpty = (startDate == null);
			boolean endDateEmpty = (endDate == null);

	    for (Application app : applications) {
	        Course course = app.getCourse();
	        
	        // lower case to make search not case sensitive
	        boolean nameMatch = employeeName.isBlank() || app.getUser().getName().toLowerCase().contains(employeeName.toLowerCase());
	        
					boolean start = startDateEmpty || course.getEndDate().compareTo(startDate)>=0;
					boolean end = endDateEmpty || course.getStartDate().compareTo(endDate)<=0;
					boolean dateMatch = start && end;
	        // boolean dateMatch = !course.getEndDate().isBefore(startDate) && !course.getStartDate().isAfter(endDate);
          // boolean dateMatch = course.getEndDate().compareTo(startDate) >= 0 && 
          //                     course.getStartDate().compareTo(endDate) <= 0;

	        boolean typeMatch = courseType.equals("ALL") || course.getCourseType().name().equals(courseType);
	        
	        if (nameMatch && dateMatch && typeMatch) searchApplications.add(app);
	    }
	    
	    System.out.println("Search results count: " + searchApplications.size());
	    model.addAttribute("searchApplications", searchApplications);

	    // return "applicationresult";
        return "applicationsearch";
	}
	
	@PostMapping("/application/status")
	public String changeStatus(
	        @RequestParam(name = "id") Integer id,
	        @RequestParam(name = "managerReason", required = true) String managerReason,
	        @RequestParam(name = "status") String status,
	        HttpSession session) {

	    User manager = (User) session.getAttribute("user");
	    if (manager == null) return "redirect:/staff/login";

	    Application app = appRepo.findById(id).orElse(null);
	    if (app == null) return "redirect:/manager/applications";

	    if (status.equals("APPROVED")) app.setStatus(Status.APPROVED);
        else if (status.equals("REJECTED")) app.setStatus(Status.REJECTED);

	    app.setManagerReason(managerReason);

	    appRepo.save(app);

	    return "redirect:/manager/applications";
	}
    
}
