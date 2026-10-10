package sg.edu.iss.cats.controller;

import java.io.IOException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.service.FeeClaimService;
import sg.edu.iss.cats.repository.AppRepo;

@Controller
public class FeeClaimController {
    private final FeeClaimService service;
    private final AppRepo apps;
    public FeeClaimController(FeeClaimService service, AppRepo apps) {
        this.service = service; this.apps = apps;
    }

    @GetMapping("/staff/claim")
    public String claimForm(@AuthenticationPrincipal User user, @RequestParam Integer applicationId, Model model) {
        Application app = apps.findById(applicationId).orElseThrow();
        if (!app.getUser().getUserId().equals(user.getUserId())) throw new SecurityException("Not yours");
        if (app.getStatus() != Status.COMPLETED || app.getCourse().getCourseType() == CourseType.INTERNAL)
            return "redirect:/staff/index";
        model.addAttribute("application", app);
        return "claimform";
    }

    @PostMapping("/staff/claim")
    public String submit(@AuthenticationPrincipal User user,
            @RequestParam Integer applicationId,
            @RequestParam MultipartFile receipt,
            @RequestParam MultipartFile certificate,
            RedirectAttributes ra) {
        try {
            service.submit(user.getUserId(), applicationId, receipt, certificate);
            ra.addFlashAttribute("successmsg", "Claim submitted for manager review");
        } catch (IOException | RuntimeException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/staff/index";
    }

    @PostMapping("/manager/claims/decision")
    public String decide(@AuthenticationPrincipal User manager, @RequestParam Integer claimId,
            @RequestParam String status, @RequestParam String reason, RedirectAttributes ra) {
        try {
            service.decide(manager.getUserId(), claimId, FeeClaim.ClaimStatus.valueOf(status), reason);
            ra.addFlashAttribute("successmsg", "Claim decision saved");
        } catch (RuntimeException ex) { ra.addFlashAttribute("errormsg", ex.getMessage()); }
        return "redirect:/manager/reports";
    }

    @GetMapping("/staff/claims/{id}/{evidence}")
    @ResponseBody
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal User user,
            @PathVariable Integer id, @PathVariable String evidence) {
        FeeClaim claim = service.findAccessible(id, user);
        boolean receipt = evidence.equals("receipt");
        if (!receipt && !evidence.equals("certificate")) return ResponseEntity.notFound().build();
        byte[] bytes = receipt ? claim.getReceiptData() : claim.getCertificateData();
        String mime = receipt ? claim.getReceiptReference() : claim.getCertificateReference();
        if (bytes == null || mime == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=claim-evidence")
                .cacheControl(CacheControl.noStore())
                .body(bytes);
    }
}
