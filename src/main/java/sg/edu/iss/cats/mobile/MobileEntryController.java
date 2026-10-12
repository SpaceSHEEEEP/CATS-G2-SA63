package sg.edu.iss.cats.mobile;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MobileEntryController {
    @GetMapping({"/mobile/app", "/mobile/app/"})
    public String entry() {
        // Built by Angular (build:web) and copied into the bootJar by Dockerfile.
        return "forward:/mobile/app/index.html";
    }
}
