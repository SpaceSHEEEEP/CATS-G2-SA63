package sg.edu.iss.cats.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import sg.edu.iss.cats.dto.CalendarEventDTO;
import sg.edu.iss.cats.model.CourseType;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.service.CalendarService;

@RestController
@RequestMapping("/staff")
public class CalendarRestController {

    private final CalendarService calendarService;

    public CalendarRestController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/calendar/events")
    public List<CalendarEventDTO> getCalendarEvents(
            @AuthenticationPrincipal User user,

            @RequestParam(name = "start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,

            @RequestParam(name = "end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end,

            @RequestParam(name = "allStaff", defaultValue = "false")
            boolean allStaff,

            @RequestParam(name = "teamTraining", defaultValue = "false")
            boolean teamTraining,

            @RequestParam(name = "category", required = false)
            CourseType category) {

        if (user == null || user.getUserId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in.");
        }

        try {
            return calendarService.findEvents(
                    user.getUserId(), allStaff, teamTraining,
                    start, end, category);
        } catch (SecurityException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    exception.getMessage(),
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception);
        }
    }
}
