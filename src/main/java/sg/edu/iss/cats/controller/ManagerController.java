package sg.edu.iss.cats.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.ApplicationRepository;
import sg.edu.iss.cats.repository.UserRepository;

@Controller 
@RequestMapping("/manager") // for manager stuff
public class ManagerController {

	private final UserRepository userRepository;
	private final ApplicationRepository applicationRepository;

	public ManagerController(UserRepository userRepository, ApplicationRepository applicationRepository) {
		this.userRepository = userRepository;
		this.applicationRepository = applicationRepository;
	}

	@GetMapping("/applications")
	public String showApplications(Model model, HttpSession session) {
		User user = (User) session.getAttribute("user");

        // needs to be logged in
		if (user == null) return "redirect:/staff/login";

        // needs to be manager
		if (!userRepository.existsByManager_UserId(user.getUserId())) return "redirect:/staff/index";

		List<Application> subordinateApplications = applicationRepository
				.findByUser_Manager_UserIdOrderByUser_NameAscApplicationIdAsc(user.getUserId());

		// Pending applications are the subset awaiting a manager's decision.
		List<Application> pendingApplications = new ArrayList<>();
		for (Application application : subordinateApplications) {
			if (application.getApplicationStatus() == ApplicationStatus.APPLIED ||
				application.getApplicationStatus() == ApplicationStatus.UPDATED) {
				pendingApplications.add(application);
			}
		}

		model.addAttribute("pendingApplications", pendingApplications);
        // TODO: change findByUser_Manager_UserIdOrderByUser_NameAscApplicationIdAsc so that it limits to 4, 
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
            if (!applicationRepository.existsByUser_UserId(u.getUserId())) continue;
            subordinatesApplications.add(applicationRepository.findByUser_UserId(u.getUserId()));
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

	    List<Application> applications = applicationRepository
            .findByUser_Manager_UserIdOrderByUser_NameAscApplicationIdAsc(manager.getUserId());

	    List<Application> searchApplications = new ArrayList<>();

			List<String> userDetailsList = new ArrayList<>();
			List<Integer> userIdTrackingList = new ArrayList<>(); // prevent duplicated entries for the user personal details
			List<String> userDetailsAndAllowanceRemainingList = new ArrayList<>();
			Map<Integer, BigDecimal> userIdToAllowance = new HashMap<>();
			
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
	        
	        if (nameMatch && dateMatch && typeMatch) 
					{
							searchApplications.add(app);

							// if application is not DELETED, prepare the details
							if (!app.getApplicationStatus().equals(ApplicationStatus.DELETED)) {
									Integer userId = app.getUser().getUserId();
									BigDecimal fee = app.getCourse().getFee();

									// if the hash map does not contain the user ID as key
									if (!userIdToAllowance.containsKey(userId)) {
										// fill in the map with user id and the user's actual allowance
										userIdToAllowance.put(userId, app.getUser().getActualAllowanceRemaining());
									}

									// return the value (allowance) which the specific key (userId) is mapped and subtract fee to obtain remaining budget
									BigDecimal budgetRemaining = userIdToAllowance.get(userId).subtract(fee);

									// update the hash map
									userIdToAllowance.put(userId, budgetRemaining);

									// Construct csv style for user personal details
									// Check first if the user id is new or duplicate
									// If the user id is new, add it into the tracking list and display it
									if (!userIdTrackingList.contains(userId)) {
										userIdTrackingList.add(userId);
									
										String personalDetailsRow = String.format("%d,%s,%s", userId, app.getUser().getName(), app.getUser().getEmail());

										userDetailsList.add(personalDetailsRow);
									}

									// Construct csv style for user application details and allowance remaining
									String row = String.format("%d,%s,%d,%s,%.2f,%.2f", userId, app.getUser().getName(), course.getCourseId(), course.getCourseName(), fee, budgetRemaining);

									userDetailsAndAllowanceRemainingList.add(row);									
							}
					}
	    }
	    
	    System.out.println("Search results count: " + searchApplications.size());
	    model.addAttribute("searchApplications", searchApplications);
			model.addAttribute("personalDetailsRows", userDetailsList);
			model.addAttribute("rows", userDetailsAndAllowanceRemainingList);

	    // return "applicationresult";
        return "applicationsearch";
	}
	
	@PostMapping("/application/status")
	public String changeApplicationStatus(
	        @RequestParam(name = "applicationId") Integer applicationId,
	        @RequestParam(name = "managerReason", required = true) String managerReason,
	        @RequestParam(name = "status") String status,
	        HttpSession session) {

	    User manager = (User) session.getAttribute("user");
	    if (manager == null) return "redirect:/staff/login";

	    Application app = applicationRepository.findById(applicationId).orElse(null);
	    if (app == null) return "redirect:/manager/applications";

	    if (status.equals("APPROVED")) app.setApplicationStatus(ApplicationStatus.APPROVED);
        else if (status.equals("REJECTED")) app.setApplicationStatus(ApplicationStatus.REJECTED);

	    app.setManagerReason(managerReason);

	    applicationRepository.save(app);

	    return "redirect:/manager/applications";
	}
    
}
