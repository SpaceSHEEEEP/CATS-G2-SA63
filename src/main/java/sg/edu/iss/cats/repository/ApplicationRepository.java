package sg.edu.iss.cats.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    // This finds a user's applications that has some status
    public List<Application> findByUser_UserIdAndApplicationStatus(Integer userId, ApplicationStatus status);

    // We write "SELECT a" insteaf of "SELECT *" becaue this is hibernate automatically giving us a java object a, not sql columns
    // Application instad of application because Application is the name of our java class 
    // JOIN FETCH does the joining btw application and user, but also fetches us the user table, which is FetchType.LAZY,
    // without FETCH, then we can't access the user somewhere down the line
    //needed to add JOIN FETCH for course as well
    @Query("SELECT a FROM Application a " +
    	       "JOIN FETCH a.user " +
    	       "JOIN FETCH a.course " +
    	       "WHERE a.user.userId = ?1 AND a.applicationId = ?2")
    	public List<Application> findByUser_UserIdAndApplicationId(
    	        Integer userId, Integer applicationId);

    // Find all applications belonging to one employee.
    @EntityGraph(attributePaths = {"user", "course"})
    List<Application> findByUser_UserId(Integer userId);

    // Load direct subordinates' applications, ordered by employee name.
    @EntityGraph(attributePaths = {"user", "course"})
    List<Application> findByUser_Manager_UserIdOrderByUser_NameAscApplicationIdAsc(Integer managerId);

    // has this user made any applications?
    @EntityGraph(attributePaths = {"user", "course"})
    boolean existsByUser_UserId(Integer userId);
}
