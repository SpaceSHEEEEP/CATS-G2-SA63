package sg.edu.iss.cats.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import sg.edu.iss.cats.service.UserCsvService;
import sg.edu.iss.cats.service.UserReportEmailService;

/**
 * /admin/** is restricted to ROLE_ADMIN by SecurityConfig, including both endpoints here.
 * POST remains CSRF-protected and returns to the existing admin page with flash feedback.
 */
@Controller
@RequestMapping("/admin")
public class UserCsvController {
    private static final Logger log = LoggerFactory.getLogger(UserCsvController.class);
    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final UserCsvService csvService;
    private final ObjectProvider<UserReportEmailService> emailService;

    public UserCsvController(UserCsvService csvService, ObjectProvider<UserReportEmailService> emailService) {
        this.csvService = csvService;
        this.emailService = emailService;
    }

    @GetMapping("/users.csv")
    public ResponseEntity<byte[]> downloadUsers() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"AllUsers.csv\"")
                .contentType(CSV)
                .body(csvService.generateCsv());
    }

    @PostMapping("/users/email")
    public String emailUsers(@RequestParam(name = "recipient", defaultValue = "") String recipient,
            RedirectAttributes flash) {
        String email;
        try {
            email = singleMailbox(recipient);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errormsg", "Enter a single valid recipient email address.");
            return "redirect:/admin/index";
        }

        try {
            UserReportEmailService service = emailService.getIfAvailable();
            if (service == null) throw new IllegalStateException("SMTP not configured");
            service.sendReport(email);
            flash.addFlashAttribute("successmsg", "User report emailed successfully.");
        } catch (MessagingException | RuntimeException ex) {
            // Do not disclose SMTP configuration or internal exception messages to the browser.
            log.warn("Failed to email user CSV report: {}", ex.getClass().getSimpleName());
            flash.addFlashAttribute("errormsg", "Could not email the user report. Check mail configuration or try again.");
        }
        return "redirect:/admin/index";
    }

    private static String singleMailbox(String recipient) {
        String email = recipient == null ? "" : recipient.trim();
        if (email.isEmpty() || email.length() > 254 || email.contains("\r") || email.contains("\n")) {
            throw new IllegalArgumentException("Invalid recipient");
        }
        try {
            InternetAddress[] parsed = InternetAddress.parse(email, true);
            if (parsed.length != 1 || !email.equals(parsed[0].getAddress())) {
                throw new IllegalArgumentException("Expected one plain email address");
            }
            parsed[0].validate();
            return email;
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Invalid recipient", ex);
        }
    }
}
