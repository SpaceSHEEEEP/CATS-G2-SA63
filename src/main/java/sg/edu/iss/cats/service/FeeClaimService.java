package sg.edu.iss.cats.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;

/** Evidence remains in the database transaction with its claim metadata.
 * Store a bounded amount of file data; downloads require owner/manager checks. */
@Service
public class FeeClaimService {
    private final FeeClaimRepository claims;
    private final AppRepo apps;
    private final UserRepository users;

    public FeeClaimService(FeeClaimRepository claims, AppRepo apps, UserRepository users) {
        this.claims = claims; this.apps = apps; this.users = users;
    }

    private String evidenceType(MultipartFile upload) {
        Set<String> allowed = Set.of("application/pdf", "image/png", "image/jpeg");
        if (upload == null || upload.isEmpty() || upload.getSize() > 5_000_000
                || !allowed.contains(upload.getContentType())) {
            throw new IllegalArgumentException("Attach a PDF, PNG or JPEG file under 5 MB");
        }
        return upload.getContentType();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public FeeClaim submit(Integer employeeId, Integer applicationId,
            MultipartFile receipt, MultipartFile certificate) throws IOException {
        User owner = users.findLockedById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Application app = apps.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        if (!app.getUser().getUserId().equals(employeeId)) throw new SecurityException("Not your application");
        if (app.getStatus() != Status.COMPLETED
                || app.getCourse().getCourseType() == CourseType.INTERNAL
                || app.getCourse().getFee().signum() <= 0)
            throw new IllegalArgumentException("Only completed fee-paying courses are claimable");
        if (claims.existsByApplication_Id(applicationId))
            throw new IllegalArgumentException("This application already has a claim");
        String receiptType = evidenceType(receipt);
        String certificateType = evidenceType(certificate);
        FeeClaim claim = new FeeClaim();
        claim.setEmployee(owner);
        claim.setApplication(app);
        claim.setAmount(app.getCourse().getFee());
        claim.setSubmittedOn(LocalDate.now(ZoneId.of("Asia/Singapore")));
        claim.setReceiptReference(receiptType);
        claim.setCertificateReference(certificateType);
        claim.setReceiptData(receipt.getBytes());
        claim.setCertificateData(certificate.getBytes());
        app.setHasBeenPaid(true);
        apps.save(app);
        return claims.save(claim);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void decide(Integer managerId, Integer claimId, FeeClaim.ClaimStatus decision, String reason) {
        User manager = users.findLockedById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
        if (manager.getRole() != Role.ROLE_MANAGER) throw new SecurityException("Manager role required");
        FeeClaim claim = claims.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        if (claim.getEmployee().getManager() == null
                || !claim.getEmployee().getManager().getUserId().equals(managerId))
            throw new SecurityException("Claim belongs to another manager's subordinate");
        if (claim.getStatus() != FeeClaim.ClaimStatus.SUBMITTED
                || decision == FeeClaim.ClaimStatus.SUBMITTED)
            throw new IllegalArgumentException("Only pending claims can be decided");
        if (reason == null || reason.isBlank() || reason.length() > 1000)
            throw new IllegalArgumentException("Manager decision reason is required (max 1,000 characters)");
        claim.setManagerReason(reason.trim());
        claim.setStatus(decision);
        claims.save(claim);
    }

    @Transactional(readOnly = true)
    public FeeClaim findAccessible(Integer claimId, User user) {
        FeeClaim claim = claims.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        if (user.getUserId().equals(claim.getEmployee().getUserId())) return claim;
        if (user.getRole() == Role.ROLE_MANAGER
                && claim.getEmployee().getManager() != null
                && user.getUserId().equals(claim.getEmployee().getManager().getUserId())) return claim;
        throw new SecurityException("Not authorised to access the claim evidence");
    }
}
