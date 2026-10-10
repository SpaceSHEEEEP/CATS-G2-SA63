package sg.edu.iss.cats.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Status;

public interface AppRepo extends JpaRepository<Application, Integer> {

    // This finds a user's applications that has some status
    public List<Application> findByUser_UserIdAndStatusIn(Integer userId, Collection<Status> statuses);

    // We write "SELECT a" insteaf of "SELECT *" becaue this is hibernate automatically giving us a java object a, not sql columns
    // Application instad of application because Application is the name of our java class 
    // JOIN FETCH does the joining btw application and user, but also fetches us the user table, which is FetchType.LAZY,
    // without FETCH, then we can't access the user somewhere down the line
    //needed to add JOIN FETCH for course as well
    @Query("SELECT a FROM Application a " +
    	       "JOIN FETCH a.user " +
    	       "JOIN FETCH a.course " +
    	       "WHERE a.user.userId = ?1 AND a.id = ?2")
    public List<Application> findByUser_UserIdAndId(
            Integer userId, Integer id);
    // can replace the above with 
    // @EntityGraph(attributePaths = {"user", "course"})
    // public List<Application> findByUser_UserIdAndId(Integer userId, Integer id);
    // the EntityGraph will load the applications' user and course data

    @Override
    @EntityGraph(attributePaths = {"user", "course"})
    public Optional<Application> findById(Integer id);

    // Find all applications belonging to one employee.
    @EntityGraph(attributePaths = {"user", "course"})
    List<Application> findByUser_UserId(Integer userId);

    // Load direct subordinates' applications, ordered by employee name.
    @EntityGraph(attributePaths = {"user", "course"})
    List<Application> findByUser_Manager_UserIdOrderByUser_NameAscIdAsc(Integer managerId);

    // has this user made any applications?
    @EntityGraph(attributePaths = {"user", "course"})
    boolean existsByUser_UserId(Integer userId);
    // can use the above findByUser_UserId?
    
 // Personal view: the service supplies the logged-in user's ID and allowed statuses.
    @EntityGraph(attributePaths = {"user", "course"})
    @Query("""
            SELECT a FROM Application a
            WHERE a.user.userId = :userId
              AND a.status IN :statuses
              AND a.course.startDate <= :endDate
              AND a.course.endDate >= :startDate
            ORDER BY a.course.startDate, a.id
            """)
    List<Application> findPersonalCalendarApplications(
            @Param("userId") Integer userId,
            @Param("statuses") Collection<Status> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Shared view: approval is enforced in the query itself.
    @EntityGraph(attributePaths = {"user", "course"})
    @Query("""
            SELECT a FROM Application a
            WHERE a.status = sg.edu.iss.cats.model.Status.APPROVED
              AND a.course.startDate <= :endDate
              AND a.course.endDate >= :startDate
            ORDER BY a.course.startDate, a.user.name, a.id
            """)
    List<Application> findApprovedCalendarApplications(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
    
    // Team view: all statuses, restricted to the manager's direct subordinates.
    @EntityGraph(attributePaths = {"user", "course"})
    @Query("""
            SELECT a FROM Application a
            WHERE a.user.manager.userId = :managerId
              AND a.course.startDate <= :endDate
              AND a.course.endDate >= :startDate
            ORDER BY a.course.startDate, a.user.name, a.id
            """)
    List<Application> findTeamCalendarApplications(
            @Param("managerId") Integer managerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
