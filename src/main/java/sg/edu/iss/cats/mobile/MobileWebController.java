package sg.edu.iss.cats.mobile;

import sg.edu.iss.cats.security.ActionConfirmation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sg.edu.iss.cats.model.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

@Controller
public class MobileWebController {
    private static final String PAIR_QR_SESSION = "cats.mobile.pairingQr";
    private record PairingQr(Integer userId, String code, long expiresAt) {}
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
    public String pair(@AuthenticationPrincipal User user, HttpSession session, RedirectAttributes ra) {
        String code = access.issueCode(user.getUserId());
        session.setAttribute(PAIR_QR_SESSION,
                new PairingQr(user.getUserId(), code, System.currentTimeMillis() + 300_000));
        ra.addFlashAttribute("pairCode", code);
        return "redirect:/staff/mobile";
    }

    @GetMapping(value = "/staff/mobile/pair-qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> pairingQr(@AuthenticationPrincipal User user,
            HttpSession session, HttpServletRequest request) throws Exception {
        Object value = session.getAttribute(PAIR_QR_SESSION);
        if (user == null || !(value instanceof PairingQr pairing)
                || !pairing.userId().equals(user.getUserId())
                || System.currentTimeMillis() >= pairing.expiresAt())
            return ResponseEntity.notFound().build();
        String origin = ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null).replaceQuery(null).build().toUriString();
        if (!origin.startsWith("https://")) return ResponseEntity.notFound().build();
        String payload = "catsmobile://pair?server="
                + java.net.URLEncoder.encode(origin, StandardCharsets.UTF_8)
                + "&code=" + pairing.code();
        var matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 256, 256);
        var buffer = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", buffer);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG).body(buffer.toByteArray());
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
