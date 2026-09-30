package sg.edu.iss.cats.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

  @Query("SELECT a from Application a LEFT JOIN FETCH a.course")
  List<Application> findAllApplications();

}
