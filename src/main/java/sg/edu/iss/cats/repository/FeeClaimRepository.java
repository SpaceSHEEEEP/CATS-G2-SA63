package sg.edu.iss.cats.repository;

import java.util.List;
import java.time.LocalDate;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.iss.cats.model.FeeClaim;

public interface FeeClaimRepository extends JpaRepository<FeeClaim, Integer> {
    boolean existsByApplication_Id(Integer applicationId);
    boolean existsByEmployee_UserId(Integer employeeId);
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    @org.springframework.data.jpa.repository.Query("SELECT claim FROM FeeClaim claim")
    List<FeeClaim> findAllForReporting();
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    List<FeeClaim> findByEmployee_Manager_UserId(Integer managerId);
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    List<FeeClaim> findByEmployee_UserId(Integer employeeId);
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    Page<FeeClaim> findByEmployee_Manager_UserId(Integer managerId, Pageable pageable);
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    Page<FeeClaim> findByEmployee_UserId(Integer employeeId, Pageable pageable);
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    Page<FeeClaim> findByStatusIn(Collection<FeeClaim.ClaimStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    @Query("SELECT claim FROM FeeClaim claim WHERE claim.submittedOn BETWEEN :first AND :last " +
           "AND (:employeeId IS NULL OR claim.employee.userId = :employeeId) " +
           "ORDER BY claim.submittedOn DESC, claim.id DESC")
    Page<FeeClaim> findReportClaims(@Param("first") LocalDate first,
            @Param("last") LocalDate last, @Param("employeeId") Integer employeeId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"employee"})
    List<FeeClaim> findByEmployee_UserIdInAndSubmittedOnBetween(
            Collection<Integer> userIds, LocalDate first, LocalDate last);
    @EntityGraph(attributePaths = {"employee"})
    List<FeeClaim> findByEmployee_UserIdInAndPaidOnBetween(
            Collection<Integer> userIds, LocalDate first, LocalDate last);
}
