package sg.edu.iss.cats.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** The post-login landing page respects role while leaving MFA enforcement to its filter. */
@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/staff/login";
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()));
        return admin ? "redirect:/admin/index" : "redirect:/staff/index";
    }
}
