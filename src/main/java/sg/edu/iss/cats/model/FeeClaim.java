package sg.edu.iss.cats.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Claim records can be reported independently of application approvals. */
@Entity
@Getter @Setter
@Table(name = "fee_claim")
public class FeeClaim {
    public enum ClaimStatus { SUBMITTED, APPROVED, REJECTED, PAID }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    /** One reimbursement claim per application, including a rejected claim. */
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User employee;
    @Column(nullable = false)
    private BigDecimal amount;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private ClaimStatus status = ClaimStatus.SUBMITTED;
    @Column(nullable = false)
    private LocalDate submittedOn;
    private String managerReason;
    /** Actual external reimbursement, recorded only after payment is confirmed. */
    private LocalDate paidOn;
    @Column(length = 120)
    private String paymentReference;
    private String receiptReference;
    private String certificateReference;
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] receiptData;
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] certificateData;
}
