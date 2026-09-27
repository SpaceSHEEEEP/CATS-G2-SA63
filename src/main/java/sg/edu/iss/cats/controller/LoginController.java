package sg.edu.iss.cats.controller;

import sg.edu.iss.cats.model.LoginForm;
import sg.edu.iss.cats.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String displayLoginForm(Model model) {
        model.addAttribute("login", new LoginForm());
        return "login";
    }

    @PostMapping("/")
    public String processLogin(@ModelAttribute LoginForm loginForm, RedirectAttributes ra) {
        // get username and password from login form
        // .trim() to remove whitespaces from strings
        String username = loginForm.getUsername().trim();
        String password = loginForm.getPassword().trim();

        if (!userRepository.existsByUsername(username)) {
            ra.addFlashAttribute("msg", "The user with username you just entered does not exist");
            return "redirect:/";
        }
        else if (!userRepository.existsByUsernameAndPassword(username, password)) {
            ra.addFlashAttribute("msg", "The password you entered is incorrect");
            return "redirect:/";
        }
        return "index";

    }
    
}

