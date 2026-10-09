package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.service.CalendarService;
import sg.edu.iss.cats.dto.CalendarEventDTO;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.CourseDuration;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.Holiday;
import sg.edu.iss.cats.model.User;

class CalendarServiceTests {

	private final AppRepo appRepo = mock(AppRepo.class);
	private final HolidayRepository holidayRepository = mock(HolidayRepository.class);

	private final CalendarService calendarService = new CalendarService(appRepo, holidayRepository);

    private final LocalDate start = LocalDate.of(2035, 4, 1);
    private final LocalDate end = LocalDate.of(2035, 4, 30);

    @Test
    void personalViewUsesOnlyTheLoggedInUsersQuery() {
        List<Status> statuses =
                List.of(Status.APPLIED, Status.UPDATED, Status.APPROVED);

        Application application = new Application();

        when(appRepo.findPersonalCalendarApplications(
                7, statuses, start, end))
                .thenReturn(List.of(application));

        assertThat(calendarService.findApplications(
                7, false, start, end, null))
                .containsExactly(application);

        verify(appRepo).findPersonalCalendarApplications(
                7, statuses, start, end);
        verifyNoMoreInteractions(appRepo);
    }

    @Test
    void sharedViewUsesOnlyTheApprovedQuery() {
        Application application = new Application();

        when(appRepo.findApprovedCalendarApplications(start, end))
                .thenReturn(List.of(application));

        assertThat(calendarService.findApplications(
                7, true, start, end, null))
                .containsExactly(application);

        verify(appRepo).findApprovedCalendarApplications(start, end);
        verifyNoMoreInteractions(appRepo);
    }
    
    @Test
    void personalEventsExcludeWeekendsAndHolidaysAndCountApprovedStaff() {
        LocalDate rangeStart = LocalDate.of(2026, 10, 9);
        LocalDate rangeEnd = LocalDate.of(2026, 10, 13);

        Course course = new Course();
        course.setCourseId(42);
        course.setCourseName("Java Training");
        course.setCourseType(CourseType.INTERNAL);
        course.setStartDate(LocalDate.of(2026, 10, 8));
        course.setEndDate(LocalDate.of(2026, 10, 14));
        course.setDuration(CourseDuration.FULLDAY);

        User ben = new User();
        ben.setUserId(7);
        ben.setName("Ben");

        User dan = new User();
        dan.setUserId(8);
        dan.setName("Dan");

        Application ownApplication = new Application();
        ownApplication.setId(100);
        ownApplication.setUser(ben);
        ownApplication.setCourse(course);
        ownApplication.setStatus(Status.APPLIED);

        Application approvedApplication = new Application();
        approvedApplication.setId(101);
        approvedApplication.setUser(dan);
        approvedApplication.setCourse(course);
        approvedApplication.setStatus(Status.APPROVED);

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(LocalDate.of(2026, 10, 12));
        holiday.setHolidayName("Test holiday");

        when(appRepo.findPersonalCalendarApplications(
                7,
                List.of(Status.APPLIED, Status.UPDATED, Status.APPROVED),
                rangeStart, rangeEnd))
                .thenReturn(List.of(ownApplication));

        when(appRepo.findApprovedCalendarApplications(rangeStart, rangeEnd))
                .thenReturn(List.of(approvedApplication));

        when(holidayRepository
                .findByHolidayDateBetweenOrderByHolidayDateAsc(
                        rangeStart, rangeEnd))
                .thenReturn(List.of(holiday));

        List<CalendarEventDTO> events = calendarService.findEvents(
                7, false, rangeStart, rangeEnd, null);

        assertThat(events)
                .extracting(CalendarEventDTO::getStart)
                .containsExactly(
                        LocalDate.of(2026, 10, 9),
                        LocalDate.of(2026, 10, 13));

        assertThat(events)
                .extracting(CalendarEventDTO::getEnd)
                .containsExactly(
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 14));

        assertThat(events).allSatisfy(event -> {
            assertThat(event.getEmployeeName()).isEqualTo("Ben");
            assertThat(event.getStatus()).isEqualTo(Status.APPLIED);
            assertThat(event.getApprovedParticipants()).isEqualTo(1L);
            assertThat(event.getDuration()).isEqualTo(CourseDuration.FULLDAY);
        });

        assertThat(events)
                .extracting(CalendarEventDTO::getId)
                .doesNotHaveDuplicates();
    }
}