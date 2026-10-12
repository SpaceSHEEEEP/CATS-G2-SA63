package sg.edu.iss.cats.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
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

    private record Evidence(String mimeType, byte[] bytes) {}

    private Evidence validatedEvidence(MultipartFile upload) throws IOException {
        if (upload == null || upload.isEmpty() || upload.getSize() > 5_000_000)
            throw new IllegalArgumentException("Attach a PDF, PNG or JPEG file under 5 MB");
        byte[] bytes = upload.getBytes();
        if (bytes.length == 0 || bytes.length > 5_000_000)
            throw new IllegalArgumentException("Evidence file must be under 5 MB");
        final byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        final String detected;
        if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P'
                && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') {
            detected = "application/pdf";
        } else if (bytes.length >= png.length
                && Arrays.equals(Arrays.copyOf(bytes, png.length), png)) {
            detected = "image/png";
        } else if (bytes.length >= 4 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff
                && (bytes[bytes.length - 2] & 0xff) == 0xff
                && (bytes[bytes.length - 1] & 0xff) == 0xd9) {
            detected = "image/jpeg";
        } else {
            throw new IllegalArgumentException("File signature is not a valid PDF, PNG or JPEG");
        }
        String declared = upload.getContentType();
        if (declared != null && !declared.isBlank()
                && !declared.equals("application/octet-stream") && !declared.equals(detected))
            throw new IllegalArgumentException("File content does not match its declared MIME type");
        // Signature checks are not an antivirus scan; deployment should add malware scanning.
        return new Evidence(detected, bytes);
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
        Evidence receiptEvidence = validatedEvidence(receipt);
        Evidence certificateEvidence = validatedEvidence(certificate);
        FeeClaim claim = new FeeClaim();
        claim.setEmployee(owner);
        claim.setApplication(app);
        claim.setAmount(app.getCourse().getFee());
        claim.setSubmittedOn(LocalDate.now(ZoneId.of("Asia/Singapore")));
        claim.setReceiptReference(receiptEvidence.mimeType());
        claim.setCertificateReference(certificateEvidence.mimeType());
        claim.setReceiptData(receiptEvidence.bytes());
        claim.setCertificateData(certificateEvidence.bytes());
        // Proof of the employee's personal payment is in the attached receipt.
        // Reimbursement is recorded only by ClaimStatus.PAID in recordPayment().
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
                || (decision != FeeClaim.ClaimStatus.APPROVED
                    && decision != FeeClaim.ClaimStatus.REJECTED))
            throw new IllegalArgumentException("Only pending claims can be decided");
        if (reason == null || reason.isBlank() || reason.length() > 1000)
            throw new IllegalArgumentException("Manager decision reason is required (max 1,000 characters)");
        claim.setManagerReason(reason.trim());
        claim.setStatus(decision);
        claims.save(claim);
    }

    /** Administrative confirmation that a previously approved reimbursement was paid. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void recordPayment(Integer adminId, Integer claimId, String reference) {
        User admin = users.findLockedById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Administrator not found"));
        if (admin.getRole() != Role.ROLE_ADMIN)
            throw new SecurityException("Administrator role required to record payments");
        if (reference == null || reference.isBlank() || reference.trim().length() > 120)
            throw new IllegalArgumentException("A payment reference of at most 120 characters is required");
        FeeClaim claim = claims.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        if (claim.getStatus() != FeeClaim.ClaimStatus.APPROVED)
            throw new IllegalArgumentException("Only approved and unpaid claims may be recorded as paid");
        claim.setStatus(FeeClaim.ClaimStatus.PAID);
        claim.setPaidOn(LocalDate.now(ZoneId.of("Asia/Singapore")));
        claim.setPaymentReference(reference.trim());
        claims.save(claim);
    }

    @Transactional(readOnly = true)
    public FeeClaim findAccessible(Integer claimId, User user) {
        FeeClaim claim = claims.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found"));
        if (user == null) throw new SecurityException("Not authenticated");
        if (user.getRole() == Role.ROLE_ADMIN) return claim;
        if (user.getUserId().equals(claim.getEmployee().getUserId())) return claim;
        if (user.getRole() == Role.ROLE_MANAGER
                && claim.getEmployee().getManager() != null
                && user.getUserId().equals(claim.getEmployee().getManager().getUserId())) return claim;
        throw new SecurityException("Not authorised to access the claim evidence");
    }
}
