package sg.edu.iss.cats.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.User;
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

		if (user == null) {
			return "redirect:/staff/login";
		}

		// Managers are users who have employees reporting to them.
		if (!userRepository.existsByManager_UserId(user.getUserId())) {
			return "redirect:/staff/index";
		}

		List<Application> subordinateApplications = applicationRepository
				.findByUser_Manager_UserIdOrderByUser_NameAscApplicationIdAsc(user.getUserId());

		// Pending applications are the subset awaiting a manager's decision.
		List<Application> pendingApplications = new ArrayList<>();
		for (Application application : subordinateApplications) {
			if (application.getApplicationStatus() == ApplicationStatus.APPLIED
					|| application.getApplicationStatus() == ApplicationStatus.UPDATED) {
				pendingApplications.add(application);
			}
		}

		model.addAttribute("pendingApplications", pendingApplications);
		model.addAttribute("subordinateApplications", subordinateApplications);

		return "pendingapplications";
	}
	
    
}
