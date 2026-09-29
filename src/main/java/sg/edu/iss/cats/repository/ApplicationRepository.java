package sg.edu.iss.cats.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {


}
