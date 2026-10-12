package sg.edu.iss.cats.mfa;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.iss.cats.mobile.MobileDeviceToken;
import sg.edu.iss.cats.mobile.MobileDeviceTokenRepository;
import sg.edu.iss.cats.model.Role;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

/** Identity verification is an operational prerequisite to invoking this admin-only reset. */
@Controller
public class AdminMfaResetController {
    private final UserRepository users;
    private final MobileDeviceTokenRepository devices;
    public AdminMfaResetController(UserRepository users, MobileDeviceTokenRepository devices) {
        this.users = users; this.devices = devices;
    }
    @Transactional
    @PostMapping("/admin/mfa/reset")
    public String reset(@AuthenticationPrincipal User admin,
            @RequestParam Integer userId, @RequestParam String confirmation, RedirectAttributes ra) {
        if (admin == null || admin.getRole() != Role.ROLE_ADMIN) throw new SecurityException("Admin required");
        if (!"RESET MFA".equals(confirmation) || admin.getUserId().equals(userId)) {
            ra.addFlashAttribute("errormsg", "Type RESET MFA to reset someone else's MFA after verifying their identity");
            return "redirect:/admin/index";
        }
        User user = users.findLockedById(userId).orElseThrow();
        user.setMfaSecretEncrypted(null); user.setMfaLastStep(null);
        users.save(user);
        for (MobileDeviceToken device : devices.findByUser_UserIdOrderByIssuedAtDesc(userId)) {
            device.setRevoked(true); devices.save(device);
        }
        ra.addFlashAttribute("successmsg", "MFA reset and all mobile sync tokens revoked for " + user.getUsername());
        return "redirect:/admin/index";
    }
}
