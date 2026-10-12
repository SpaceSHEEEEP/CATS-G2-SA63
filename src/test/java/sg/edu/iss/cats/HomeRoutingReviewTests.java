package sg.edu.iss.cats;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import sg.edu.iss.cats.controller.HomeController;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeRoutingReviewTests {
    private final HomeController controller = new HomeController();

    @Test void anonymousUsersGoToLogin() {
        assertEquals("redirect:/staff/login", controller.home(null));
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertEquals("redirect:/staff/login", controller.home(anonymous));
        assertEquals("redirect:/staff/login",
                controller.home(new UsernamePasswordAuthenticationToken("user", "password")));
    }

    @Test void authenticatedStaffAndAdminsAreRoutedSeparately() {
        var staff = new UsernamePasswordAuthenticationToken("staff", "password",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
        var admin = new UsernamePasswordAuthenticationToken("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertEquals("redirect:/staff/index", controller.home(staff));
        assertEquals("redirect:/admin/index", controller.home(admin));
    }
}
