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
    public enum ClaimStatus { SUBMITTED, APPROVED, REJECTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Application application;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User employee;
    @Column(nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;
    @Column(nullable = false)
    private LocalDate submittedOn;
    private String managerReason;
    private String receiptReference;
    private String certificateReference;
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] receiptData;
    @Lob @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] certificateData;
}
