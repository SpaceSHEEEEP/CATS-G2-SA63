package sg.edu.iss.cats.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.iss.cats.model.FeeClaim;

public interface FeeClaimRepository extends JpaRepository<FeeClaim, Integer> {
    boolean existsByApplication_Id(Integer applicationId);
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    @org.springframework.data.jpa.repository.Query("SELECT claim FROM FeeClaim claim")
    List<FeeClaim> findAllForReporting();
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    List<FeeClaim> findByEmployee_Manager_UserId(Integer managerId);
    @EntityGraph(attributePaths = {"application", "application.course", "employee"})
    List<FeeClaim> findByEmployee_UserId(Integer employeeId);
}
