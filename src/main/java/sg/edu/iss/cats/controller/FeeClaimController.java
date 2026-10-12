package sg.edu.iss.cats.controller;

import sg.edu.iss.cats.security.ActionConfirmation;

import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.service.FeeClaimService;
import sg.edu.iss.cats.repository.AppRepo;

import sg.edu.iss.cats.repository.FeeClaimRepository;

@Controller
public class FeeClaimController {
    private final FeeClaimService service;
    private final AppRepo apps;
    private final FeeClaimRepository claims;
    public FeeClaimController(FeeClaimService service, AppRepo apps, FeeClaimRepository claims) {
        this.service = service; this.apps = apps; this.claims = claims;
    }

    private PageRequest page(int page) {
        return PageRequest.of(Math.max(0, page), 20, Sort.by("submittedOn").descending());
    }

    @GetMapping("/staff/claims")
    public String personalClaims(@AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page, Model model) {
        Page<FeeClaim> result = claims.findByEmployee_UserId(user.getUserId(), page(page));
        model.addAttribute("claims", result.getContent());
        model.addAttribute("page", result);
        return "staffclaims";
    }

    @GetMapping("/manager/claims")
    public String teamClaims(@AuthenticationPrincipal User manager,
            @RequestParam(defaultValue = "0") int page, Model model) {
        if (manager.getRole() != Role.ROLE_MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Page<FeeClaim> result = claims.findByEmployee_Manager_UserId(manager.getUserId(), page(page));
        model.addAttribute("claims", result.getContent());
        model.addAttribute("page", result);
        return "managerclaims";
    }

    @GetMapping("/admin/claims")
    public String payments(@AuthenticationPrincipal User admin,
            @RequestParam(defaultValue = "0") int page, Model model) {
        if (admin.getRole() != Role.ROLE_ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Page<FeeClaim> result = claims.findByStatusIn(
                List.of(FeeClaim.ClaimStatus.APPROVED, FeeClaim.ClaimStatus.PAID), page(page));
        model.addAttribute("claims", result.getContent());
        model.addAttribute("page", result);
        return "adminclaims";
    }

    @PostMapping("/admin/claims/pay")
    public String recordPaid(@AuthenticationPrincipal User admin,
            @RequestParam Integer claimId, @RequestParam String paymentReference,
            @RequestParam String confirmation, RedirectAttributes ra) {
        try {
            ActionConfirmation.require(confirmation, "PAID");
            service.recordPayment(admin.getUserId(), claimId, paymentReference);
            ra.addFlashAttribute("successmsg", "External reimbursement marked paid");
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorised", ex);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/admin/claims";
    }

    @GetMapping("/staff/claim")
    public String claimForm(@AuthenticationPrincipal User user, @RequestParam Integer applicationId, Model model) {
        Application app = apps.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        if (!app.getUser().getUserId().equals(user.getUserId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your application");
        if (app.getStatus() != Status.COMPLETED || app.getCourse().getCourseType() == CourseType.INTERNAL)
            return "redirect:/staff/index";
        if (claims.existsByApplication_Id(applicationId)) return "redirect:/staff/claims";
        model.addAttribute("claimApplication", app);
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
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your application", ex);
        } catch (IOException | IllegalArgumentException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/staff/index";
    }

    @PostMapping("/manager/claims/decision")
    public String decide(@AuthenticationPrincipal User manager, @RequestParam Integer claimId,
            @RequestParam String status, @RequestParam String reason,
            @RequestParam String confirmation, RedirectAttributes ra) {
        try {
            ActionConfirmation.require(confirmation, "REJECTED".equals(status) ? "REJECT" : "APPROVE");
            service.decide(manager.getUserId(), claimId, FeeClaim.ClaimStatus.valueOf(status), reason);
            ra.addFlashAttribute("successmsg", "Claim decision saved");
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorised", ex);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("errormsg", ex.getMessage());
        }
        return "redirect:/manager/claims";
    }

    @GetMapping("/staff/claims/{id}/{evidence}")
    @ResponseBody
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal User user,
            @PathVariable Integer id, @PathVariable String evidence) {
        final FeeClaim claim;
        try {
            claim = service.findAccessible(id, user);
        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorised", ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found", ex);
        }
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
