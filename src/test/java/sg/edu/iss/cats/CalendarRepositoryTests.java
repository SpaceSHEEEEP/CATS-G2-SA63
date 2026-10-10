package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseDuration;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.CourseRepository;
import sg.edu.iss.cats.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CalendarRepositoryTests {

    private final AppRepo appRepo;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    CalendarRepositoryTests(
            AppRepo appRepo,
            CourseRepository courseRepository,
            UserRepository userRepository) {
        this.appRepo = appRepo;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Test
    @DisplayName("Personal calendar stays private; shared calendar shows approved only")
    void calendarQueriesRespectVisibilityAndDateOverlap() {
        User ben = userRepository.findByUsername("benny");
        User dan = userRepository.findByUsername("danny");

        assertThat(ben).isNotNull();
        assertThat(dan).isNotNull();

        Course course = courseRepository.saveAndFlush(new Course(
                "Calendar test course",
                CourseType.INTERNAL,
                LocalDate.of(2035, 3, 30),
                LocalDate.of(2035, 4, 5),
                "Test room",
                "Test provider",
                BigDecimal.ZERO,
                CourseDuration.FULLDAY));

        Application benApplied = saveApplication(ben, course, Status.APPLIED);
        Application benApproved = saveApplication(ben, course, Status.APPROVED);
        Application benUpdated = saveApplication(ben, course, Status.UPDATED);
        saveApplication(ben, course, Status.REJECTED);
        saveApplication(ben, course, Status.CANCELLED);
        saveApplication(ben, course, Status.DELETED);
        saveApplication(ben, course, Status.COMPLETED);

        saveApplication(dan, course, Status.APPLIED);
        saveApplication(dan, course, Status.UPDATED);
        Application danApproved = saveApplication(dan, course, Status.APPROVED);

        LocalDate start = LocalDate.of(2035, 4, 1);
        LocalDate end = LocalDate.of(2035, 4, 30);

        List<Application> personal = appRepo.findPersonalCalendarApplications(
                ben.getUserId(),
                List.of(Status.APPLIED, Status.UPDATED, Status.APPROVED),
                start, end);

        assertThat(personal)
                .extracting(Application::getId)
                .containsExactlyInAnyOrder(
                        benApplied.getId(),
                        benApproved.getId(),
                        benUpdated.getId());

        List<Application> shared =
                appRepo.findApprovedCalendarApplications(start, end);

        assertThat(shared)
                .extracting(Application::getId)
                .containsExactlyInAnyOrder(
                        benApproved.getId(),
                        danApproved.getId());

        assertThat(appRepo.findApprovedCalendarApplications(
                LocalDate.of(2035, 5, 1),
                LocalDate.of(2035, 5, 31)))
                .isEmpty();
    }

    private Application saveApplication(
            User user, Course course, Status status) {
        Application application = new Application();
        application.setUser(user);
        application.setCourse(course);
        application.setStatus(status);
        application.setUserReason("Calendar query test");
        return appRepo.saveAndFlush(application);
    }
}