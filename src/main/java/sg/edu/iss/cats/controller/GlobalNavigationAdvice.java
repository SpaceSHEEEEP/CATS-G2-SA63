package sg.edu.iss.cats.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import sg.edu.iss.cats.model.*;

@ControllerAdvice
public class GlobalNavigationAdvice {
    @ModelAttribute("isManager")
    public boolean isManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) return false;
        return user.getRole() == Role.ROLE_MANAGER;
    }
}
