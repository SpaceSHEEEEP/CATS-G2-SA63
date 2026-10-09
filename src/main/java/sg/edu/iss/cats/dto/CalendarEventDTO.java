package sg.edu.iss.cats.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sg.edu.iss.cats.model.CourseDuration;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.Status;

@Getter
@AllArgsConstructor
public class CalendarEventDTO {

    // One application can produce several daily calendar entries.
    private final String id;

    private final String text;
    private final LocalDate start;
    private final LocalDate end;
    // Calendar entry dates describe one day. these describe the full course.
    private final LocalDate courseStartDate;
    private final LocalDate courseEndDate;

    private final Integer applicationId;
    private final Integer courseId;
    private final String employeeName;
    private final String courseName;
    private final CourseType category;
    private final Status status;
    private final CourseDuration duration;
    private final String location;
    private final String trainingProvider;
    private final long approvedParticipants;
}