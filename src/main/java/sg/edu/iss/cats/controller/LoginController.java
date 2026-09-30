package sg.edu.iss.cats.controller;

import sg.edu.iss.cats.model.LoginForm;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/staff")
public class LoginController {

    private final UserRepository userRepository;

    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String displayLoginForm(Model model) {
        model.addAttribute("login", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@ModelAttribute LoginForm loginForm, Model model, RedirectAttributes ra, HttpSession session) {
        // get username and password from login form
        // .trim() to remove whitespaces from strings
        String username = loginForm.getUsername().trim();
        String password = loginForm.getPassword().trim();

        if (!userRepository.existsByUsername(username)) {
            model.addAttribute("msg", "The user with username you just entered does not exist");
            model.addAttribute("login", loginForm);
            return "login";
        }

        if (!userRepository.existsByUsernameAndPassword(username, password)) {
            model.addAttribute("msg", "The password you entered is incorrect");
            model.addAttribute("login", loginForm);
            return "login";
        }
        
        // Login successful
        session.setAttribute("user", userRepository.findByUsername(username));

        return "redirect:/staff/index";

    }

    @GetMapping("/clear_session")
    public String clearSession(HttpSession session) {
        session.invalidate();
        return "redirect:/staff/login";
    }
    

    
}

