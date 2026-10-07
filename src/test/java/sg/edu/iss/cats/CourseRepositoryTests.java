package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.repository.CourseRepository;

//Uses sample users from data.sql and the test-profile MySQL database.
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

public class CourseRepositoryTests {
  
  @Autowired
  private CourseRepository courseRepository;

  @Test
  @DisplayName("Returns an list of courses based on course type with ascending order of start dates")
  void testFindByCourseTypeInOrderByStartDateAsc() {
    List<Course> result = courseRepository.findByCourseTypeInOrderByStartDateAsc(List.of(CourseType.INTERNAL));
    result.forEach(c -> System.out.println("***** " + c));

    assertThat(result).isNotEmpty();

    if (result.size() > 1) {
      for (int i = 0; i < result.size() - 1; i++) {
        assertThat(result.get(i).getStartDate()).isBeforeOrEqualTo(result.get(i + 1).getStartDate());
      }
    }
  }

  
}
