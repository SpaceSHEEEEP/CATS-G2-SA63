package sg.edu.iss.cats.mfa;

import java.io.ByteArrayOutputStream;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

@Controller
@RequestMapping("/mfa")
public class MfaController {
    private static final String PENDING_SEED = "cats.pendingTotpSeed";
    private static final String FAILURES = "cats.mfaFailures";
    private final UserRepository users;
    private final TotpService totp;
    public MfaController(UserRepository users, TotpService totp) { this.users = users; this.totp = totp; }

    @GetMapping("/continue")
    public String begin(@AuthenticationPrincipal User principal,
            @RequestParam(required = false) String target, HttpSession session) {
        if (principal == null) return "redirect:/staff/login";
        if ("admin".equals(target) && principal.getRole() == Role.ROLE_ADMIN) session.setAttribute("cats.mfaTarget", "admin");
        else if ("staff".equals(target)) session.setAttribute("cats.mfaTarget", "staff");
        User user = users.findById(principal.getUserId()).orElseThrow();
        return user.getMfaSecretEncrypted() == null ? "redirect:/mfa/enroll" : "redirect:/mfa/challenge";
    }

    @GetMapping("/enroll")
    public String enroll(@AuthenticationPrincipal User principal, HttpSession session, Model model) {
        if (principal == null) return "redirect:/staff/login";
        User user = users.findById(principal.getUserId()).orElseThrow();
        if (user.getMfaSecretEncrypted() != null) return "redirect:/mfa/challenge";
        String seed = (String) session.getAttribute(PENDING_SEED);
        if (seed == null) { seed = totp.newSecret(); session.setAttribute(PENDING_SEED, seed); }
        model.addAttribute("secret", seed);
        return "mfa-enroll";
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qr(@AuthenticationPrincipal User principal, HttpSession session) throws Exception {
        if (principal == null) return ResponseEntity.status(401).build();
        String seed = (String) session.getAttribute(PENDING_SEED);
        if (seed == null) return ResponseEntity.notFound().build();
        String uri = totp.provisionUri(principal.getUsername(), seed);
        var matrix = new MultiFormatWriter().encode(uri, BarcodeFormat.QR_CODE, 240, 240);
        var buffer = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", buffer);
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(buffer.toByteArray());
    }

    @GetMapping("/challenge")
    public String challenge(@AuthenticationPrincipal User principal, Model model) {
        if (principal == null) return "redirect:/staff/login";
        if (users.findById(principal.getUserId()).orElseThrow().getMfaSecretEncrypted() == null)
            return "redirect:/mfa/enroll";
        return "mfa-challenge";
    }

    @PostMapping("/enroll")
    public String completeEnroll(@AuthenticationPrincipal User user, HttpSession session,
            @RequestParam String code, RedirectAttributes feedback) {
        if (user == null) return "redirect:/staff/login";
        String seed = (String) session.getAttribute(PENDING_SEED);
        if (seed == null) return "redirect:/mfa/enroll";
        if (!totp.activate(user.getUserId(), seed, code)) return failure(session, feedback, "/mfa/enroll");
        session.removeAttribute(PENDING_SEED);
        return verified(user, session);
    }

    @PostMapping("/challenge")
    public String verify(@AuthenticationPrincipal User user, HttpSession session,
            @RequestParam String code, RedirectAttributes feedback) {
        if (user == null) return "redirect:/staff/login";
        if (!totp.verify(user.getUserId(), code)) return failure(session, feedback, "/mfa/challenge");
        return verified(user, session);
    }

    private String verified(User user, HttpSession session) {
        session.removeAttribute(FAILURES);
        session.setAttribute(MfaGateFilter.VERIFIED_USER_ID, user.getUserId());
        String target = (String) session.getAttribute("cats.mfaTarget");
        session.removeAttribute("cats.mfaTarget");
        return "admin".equals(target) && user.getRole() == Role.ROLE_ADMIN
                ? "redirect:/admin/index" : "redirect:/staff/index";
    }

    private String failure(HttpSession session, RedirectAttributes feedback, String destination) {
        Integer previous = (Integer) session.getAttribute(FAILURES);
        int failures = (previous == null ? 0 : previous) + 1;
        if (failures >= 5) { session.invalidate(); return "redirect:/staff/login?error"; }
        session.setAttribute(FAILURES, failures);
        feedback.addFlashAttribute("mfaError", "Code incorrect, expired or already used. Try the next code.");
        return "redirect:" + destination;
    }
}
