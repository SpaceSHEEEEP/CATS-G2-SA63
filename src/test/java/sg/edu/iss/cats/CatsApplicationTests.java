package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.ApplicationRepository;

//Uses sample users from data.sql and the test-profile MySQL database.
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CatsApplicationTests {
	@Autowired
	private ApplicationRepository applicationRepository;
	
	@Test
	@DisplayName("Returns a list of applications with user id and application status")
	void testFindApplicationByUserIdAndStatus() {
		// Formulate a sample application for testing
		Application a = new Application();
		a.setApplicationStatus(ApplicationStatus.APPLIED);
		a.setUserReason("Learn more about Java Spring Boot");

		User user = new User();
		user.setUserId(1);
		a.setUser(user);

		Course course = new Course();
		course.setCourseId(1);
		a.setCourse(course);

		applicationRepository.save(a);

		List<Application> result = applicationRepository.findByUser_UserIdAndApplicationStatus(1, ApplicationStatus.APPLIED);
		result.forEach(app -> System.out.println("***** " + app));
		assertThat(result).isNotEmpty();
	}

	@Test
	@DisplayName("Returns an empty list when no applications found with user id and application status")
	void testFindApplicationByUserIdAndStatusWithNoResults() {
		List<Application> result = applicationRepository.findByUser_UserIdAndApplicationStatus(999, ApplicationStatus.APPLIED);
		result.forEach(app -> System.out.println("***** " + app));
		assertThat(result).isEmpty();
	}
}
