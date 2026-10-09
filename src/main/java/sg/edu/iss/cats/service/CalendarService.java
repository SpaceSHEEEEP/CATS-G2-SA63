package sg.edu.iss.cats.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.Status;
import sg.edu.iss.cats.repository.AppRepo;
import sg.edu.iss.cats.repository.HolidayRepository;
import sg.edu.iss.cats.dto.CalendarEventDTO;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.Holiday;

@Service
public class CalendarService {

	private final AppRepo appRepo;
	private final HolidayRepository holidayRepository;

	public CalendarService(
	        AppRepo appRepo,
	        HolidayRepository holidayRepository) {
	    this.appRepo = appRepo;
	    this.holidayRepository = holidayRepository;
	}

    @Transactional(readOnly = true)
    public List<Application> findApplications(
            Integer loggedInUserId,
            boolean allStaff,
            LocalDate startDate,
            LocalDate endDate,
            CourseType category) {

        if (loggedInUserId == null) {
            throw new IllegalArgumentException("A logged-in user is required.");
        }

        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid calendar date range.");
        }

        // Limit requests while allowing the adjacent dates in a month grid.
        if (ChronoUnit.DAYS.between(startDate, endDate) > 61) {
            throw new IllegalArgumentException(
                    "Calendar requests cannot exceed 62 days.");
        }

        List<Application> applications;

        if (allStaff) {
            applications = appRepo.findApprovedCalendarApplications(
                    startDate, endDate);
        } else {
            applications = appRepo.findPersonalCalendarApplications(
                    loggedInUserId,
                    List.of(Status.APPLIED, Status.UPDATED, Status.APPROVED),
                    startDate, endDate);
        }

        // A null category means all categories.
        if (category == null) {
            return applications;
        }

        List<Application> filtered = new ArrayList<>();

        for (Application application : applications) {
            if (application.getCourse().getCourseType() == category) {
                filtered.add(application);
            }
        }

        return filtered;
    }
    
    @Transactional(readOnly = true)
    public List<CalendarEventDTO> findEvents(
            Integer loggedInUserId,
            boolean allStaff,
            LocalDate startDate,
            LocalDate endDate,
            CourseType category) {

        List<Application> applications = findApplications(
                loggedInUserId, allStaff, startDate, endDate, category);

        if (applications.isEmpty()) {
            return List.of();
        }

        Set<LocalDate> holidayDates = new HashSet<>();

        for (Holiday holiday :
                holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(
                        startDate, endDate)) {
            holidayDates.add(holiday.getHolidayDate());
        }

        // Other employees' approved applications are used only for counts.
        List<Application> approvedApplications = allStaff
                ? applications
                : appRepo.findApprovedCalendarApplications(startDate, endDate);

        Map<Integer, Set<Integer>> participantsByCourse = new HashMap<>();

        for (Application application : approvedApplications) {
            Integer courseId = application.getCourse().getCourseId();

            participantsByCourse
                    .computeIfAbsent(courseId, key -> new HashSet<>())
                    .add(application.getUser().getUserId());
        }

        List<CalendarEventDTO> events = new ArrayList<>();

        for (Application application : applications) {
            Course course = application.getCourse();

            LocalDate firstDate = course.getStartDate().isBefore(startDate)
                    ? startDate : course.getStartDate();

            LocalDate lastDate = course.getEndDate().isAfter(endDate)
                    ? endDate : course.getEndDate();

            long approvedParticipants = participantsByCourse
                    .getOrDefault(course.getCourseId(), Set.of())
                    .size();

            String employeeName = application.getUser().getName();

            String text = allStaff
                    ? employeeName + " — " + course.getCourseName()
                    : course.getCourseName();

            for (LocalDate date = firstDate;
                    !date.isAfter(lastDate);
                    date = date.plusDays(1)) {

                if (date.getDayOfWeek() == DayOfWeek.SATURDAY
                        || date.getDayOfWeek() == DayOfWeek.SUNDAY
                        || holidayDates.contains(date)) {
                    continue;
                }

                events.add(new CalendarEventDTO(
                        application.getId() + "-" + date,
                        text,
                        date,
                        date.plusDays(1), // DayPilot's end date is exclusive.
                        course.getStartDate(),
                        course.getEndDate(),
                        application.getId(),
                        course.getCourseId(),
                        employeeName,
                        course.getCourseName(),
                        course.getCourseType(),
                        application.getStatus(),
                        course.getDuration(),
                        course.getLocation(),
                        course.getTrainingProvider(),
                        approvedParticipants));
            }
        }

        return events;
    }
}