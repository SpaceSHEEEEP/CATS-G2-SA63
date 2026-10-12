package sg.edu.iss.cats.mobile;

import sg.edu.iss.cats.security.ActionConfirmation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.iss.cats.model.User;

@Controller
public class MobileWebController {
    private final MobileAccessService access;
    private final String apkPath;
    public MobileWebController(MobileAccessService access,
            @Value("${cats.mobile.apk-path:}") String apkPath) {
        this.access = access; this.apkPath = apkPath;
    }

    @GetMapping("/staff/mobile")
    public String mobile(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("devices", access.devices(user.getUserId()));
        model.addAttribute("apkReady", !apkPath.isBlank() && Files.isRegularFile(Path.of(apkPath)));
        return "mobile-devices";
    }

    @PostMapping("/staff/mobile/pair")
    public String pair(@AuthenticationPrincipal User user, RedirectAttributes ra) {
        ra.addFlashAttribute("pairCode", access.issueCode(user.getUserId()));
        return "redirect:/staff/mobile";
    }

    @PostMapping("/staff/mobile/revoke")
    public String revoke(@AuthenticationPrincipal User user, @RequestParam Long id,
            @RequestParam String confirmation, RedirectAttributes ra) {
        try { ActionConfirmation.require(confirmation, "REVOKE");
            access.revoke(user.getUserId(), id); ra.addFlashAttribute("notice", "Device access revoked"); }
        catch (RuntimeException ex) { ra.addFlashAttribute("notice", "Could not revoke that device"); }
        return "redirect:/staff/mobile";
    }

    @GetMapping({"/staff/mobile/download", "/mobile/download"})
    @ResponseBody
    public ResponseEntity<FileSystemResource> download() throws IOException {
        if (apkPath.isBlank()) return ResponseEntity.notFound().build();
        Path apk = Path.of(apkPath);
        if (!Files.isRegularFile(apk) || !apk.getFileName().toString().endsWith(".apk"))
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"CATS-Mobile.apk\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(Files.size(apk))
                .body(new FileSystemResource(apk));
    }
}
