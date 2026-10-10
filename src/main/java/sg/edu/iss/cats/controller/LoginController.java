package sg.edu.iss.cats.controller;

import sg.edu.iss.cats.model.LoginForm;
import sg.edu.iss.cats.repository.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    
}

