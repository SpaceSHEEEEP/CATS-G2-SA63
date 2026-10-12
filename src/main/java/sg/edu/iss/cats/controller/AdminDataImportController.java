package sg.edu.iss.cats.controller;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.service.DataImportService;

@Controller
@RequestMapping("/admin/import")
public class AdminDataImportController {
    private final DataImportService service;
    public AdminDataImportController(DataImportService service) { this.service = service; }

    @GetMapping
    public String show(@AuthenticationPrincipal User user, Model model) {
        requireAdmin(user);
        return "data-import";
    }

    @PostMapping
    public String upload(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
            RedirectAttributes feedback) throws IOException {
        requireAdmin(user);
        if (!confirm) {
            feedback.addFlashAttribute("errormsg", "Confirm the destination database before importing.");
            return "redirect:/admin/import";
        }
        if (file.isEmpty() || file.getSize() > DataImportService.MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File must be 1-500 KB");
        try {
            DataImportService.Summary result = service.importFile(file.getBytes());
            feedback.addFlashAttribute("successmsg", "Imported " + result.users() + " users, " +
                    result.providers() + " providers, " + result.courses() + " courses, " +
                    result.holidays() + " holidays, " + result.applications() + " applications.");
        } catch (IllegalArgumentException ex) {
            feedback.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/admin/import";
    }

    private static void requireAdmin(User user) {
        if (user == null || user.getRole() != Role.ROLE_ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator required");
    }
}
