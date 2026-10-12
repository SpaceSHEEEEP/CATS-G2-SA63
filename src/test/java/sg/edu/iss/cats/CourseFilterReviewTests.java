package sg.edu.iss.cats;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseDuration;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.repository.CourseRepository;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Sql("/repository-fixtures.sql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseFilterReviewTests {
    @Autowired CourseRepository courses;

    @Test void categoryFilterHasCorrectPageCountAndExcludesOtherCategories() {
        for (int i = 1; i <= 3; i++) {
            var course = new Course("External " + i, CourseType.EXTERNAL,
                    LocalDate.of(2035, 4, i), LocalDate.of(2035, 4, i),
                    "Training room", "Fixture Provider", new BigDecimal("450.00"),
                    CourseDuration.FULLDAY);
            courses.save(course);
        }
        courses.flush();
        var page = courses.findCoursesWithApplicantCountsByType(CourseType.EXTERNAL, PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3L);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).allSatisfy(row ->
                assertThat(row.getCourse().getCourseType()).isEqualTo(CourseType.EXTERNAL));
    }
}
