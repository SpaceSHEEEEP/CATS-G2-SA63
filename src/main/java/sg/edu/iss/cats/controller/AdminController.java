package sg.edu.iss.cats.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import sg.edu.iss.cats.model.LoginForm;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;
import java.time.LocalDate;
import sg.edu.iss.cats.repository.HolidayRepository;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
	private final UserRepository userRepository;
	
	public AdminController( UserRepository userRepository) {
		this.userRepository = userRepository;

	}

	@GetMapping
	public String ShowAdmin(Model model, HttpSession  session) {
		User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/admin/login";
        }

        if (!user.isAdmin()) {
            return "redirect:/staff/index";
        }

        // Load current user records
        model.addAttribute("users", userRepository.findAll());
        
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
        if (username != null && password != null && userRepository.existsByUsernameAndPassword(username.trim(), password)) {
            user = userRepository.findByUsername(username.trim());
        }

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