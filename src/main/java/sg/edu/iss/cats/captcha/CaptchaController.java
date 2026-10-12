package sg.edu.iss.cats.captcha;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {
    private final CaptchaService service;
    public CaptchaController(CaptchaService service) { this.service = service; }

    @GetMapping("/new")
    public ResponseEntity<CaptchaService.ChallengeView> newChallenge(
            @RequestParam String scope, HttpServletRequest request) {
        if (!service.isSupportedScope(scope)) return ResponseEntity.badRequest().build();
        HttpSession session = request.getSession(true);
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(service.issue(session, scope));
        } catch (CaptchaService.TooManyChallengesException ex) {
            return ResponseEntity.status(429).cacheControl(CacheControl.noStore()).build();
        }
    }

    @GetMapping(value = "/media", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> media(@RequestParam String id, @RequestParam String part,
            HttpServletRequest request) {
        byte[] bytes = service.media(request.getSession(false), id, part);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG).body(bytes);
    }
}
