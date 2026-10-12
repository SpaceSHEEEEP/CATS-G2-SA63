package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.CourseRepository;

//Synthetic fixtures, isolated in an H2/Flyway test database.
@DataJpaTest
@ActiveProfiles("test")
@Sql("/repository-fixtures.sql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CatsApplicationTests {
	@Autowired
	private AppRepo appRepo;
	@Autowired
	private CourseRepository courseRepository;
	
	@Test
	@DisplayName("Returns a list of applications with user id and application status")
	void testFindApplicationByUserIdAndStatus() {
		// Formulate a sample application for testing
		Application a = new Application();
		a.setStatus(Status.APPLIED);
		a.setUserReason("Learn more about Java Spring Boot");

		User user = new User();
		user.setUserId(1);
		a.setUser(user);

		a.setCourse(fixtureCourse());

		appRepo.save(a);

		List<Application> result = appRepo.findByUser_UserIdAndStatusIn(1, List.of(Status.APPLIED));
		result.forEach(app -> System.out.println("***** " + app));
		assertThat(result).isNotEmpty();
	}

	@Test
	@DisplayName("Returns an empty list when no applications found with user id and application status")
	void testFindApplicationByUserIdAndStatusWithNoResults() {
		List<Application> result = appRepo.findByUser_UserIdAndStatusIn(999, List.of(Status.APPLIED));
		result.forEach(app -> System.out.println("***** " + app));
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("Returns a list of applications with user id and application id")
	void testFindApplicationByUserIdAndId() {
		// Formulate a sample application for testing
		Application a = new Application();
		a.setStatus(Status.APPLIED);
		a.setUserReason("Learn more about Java Spring Boot");

		User user = new User();
		user.setUserId(1);
		a.setUser(user);

		a.setCourse(fixtureCourse());

		appRepo.save(a);
		List<Application> result = appRepo.findByUser_UserIdAndId(1, a.getId());
		result.forEach(app -> System.out.println("***** " + app));
		assertThat(result).isNotEmpty();
	}

	private Course fixtureCourse() {
		return courseRepository.findAll().stream()
				.filter(course -> "Fixture Internal Course".equals(course.getCourseName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing fixture course"));
	}

	@Test
	@DisplayName ("Returns an empty list when no applications found with user id and application id")
	void testFindApplicationByUserIdAndIdWithNoResults() {
		List<Application> result = appRepo.findByUser_UserIdAndId(999, 999);
		result.forEach(app -> System.out.println("***** " + app));
		assertThat(result).isEmpty();
	}
}
