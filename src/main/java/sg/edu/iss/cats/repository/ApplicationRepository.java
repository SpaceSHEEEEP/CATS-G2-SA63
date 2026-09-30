package sg.edu.iss.cats.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

  @Query("SELECT a from Application a LEFT JOIN FETCH a.course")
  List<Application> findAllApplications();

  // Load only this employee's applications and their catalogue courses for the index page.
  @Query("SELECT a FROM Application a LEFT JOIN FETCH a.course WHERE a.user.userId = :userId")
  List<Application> findByUserIdWithCourse(@Param("userId") Integer userId);

}
