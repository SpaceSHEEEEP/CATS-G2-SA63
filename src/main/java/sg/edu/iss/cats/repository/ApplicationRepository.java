package sg.edu.iss.cats.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    // TODO: add JUnit tests for this
    // This finds a user's applications that has some status
    List<Application> findByUser_UserIdAndApplicationStatus(Integer userId, ApplicationStatus status);

}
