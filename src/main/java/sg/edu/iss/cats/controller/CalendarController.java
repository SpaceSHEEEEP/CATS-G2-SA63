package sg.edu.iss.cats.controller;
import java.util.ArrayList;
import java.util.List;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;
import java.time.ZoneId;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.service.CalendarService;


@Controller
@RequestMapping("/staff")
public class CalendarController {
	
	private final CalendarService calendarService;

	public CalendarController(CalendarService calendarService) {
	    this.calendarService = calendarService;
	}
  
	@GetMapping("/calendar")
	public String showCalendar(
            @AuthenticationPrincipal User user,
            @RequestParam(name="month", required=false) String month,
            @RequestParam(name="year", required=false) Integer year,
            Model model) {
        
		if (user == null || user.getUserId() == null) {
		    return "redirect:/staff/login";
		}

		model.addAttribute( "isManager", calendarService.canViewTeam(user.getUserId()));
		// Open the current month when no complete selection is provided. SG TIME BASED for now
		LocalDate singaporeToday = LocalDate.now(ZoneId.of("Asia/Singapore"));
		model.addAttribute("singaporeToday", singaporeToday);

		YearMonth selectedYearMonth = YearMonth.from(singaporeToday);

		if (month != null && !month.isBlank() && year != null) {

		    // Keep calendar years positive and within four digits.
		    if (year < 1 || year > 9999) {
		        return "redirect:/staff/calendar";
		    }

		    try {
		        Month selectedMonth = Month.valueOf(
		                month.trim().toUpperCase(Locale.ROOT));

		        selectedYearMonth = YearMonth.of(year, selectedMonth);

		    } catch (IllegalArgumentException exception) {
		        // Invalid month links return to the current calendar.
		        return "redirect:/staff/calendar";
		    }
		}

        String nameOfMonth = selectedYearMonth.getMonth().name();
        int numOfDaysInMonth = selectedYearMonth.lengthOfMonth();
        int currentYear = selectedYearMonth.getYear();

        // Find the 1st day of the month
        LocalDate firstDayOfMonth = selectedYearMonth.atDay(1);
        model.addAttribute("calendarStart", firstDayOfMonth);
        // and its corresponding day of the week
        int dayOfWeek = firstDayOfMonth.getDayOfWeek().getValue();

        // Default is 1 - Monday, 2 - Tuesday to 7 - Sunday
        // Adjust to ensure that Sunday is 1, Monday is 2 and so on
        int reassignedDayOfWeek = (dayOfWeek % 7) + 1;

        // Create list for blank days
        List<Integer> blankDays = new ArrayList<>();
        for (int i=1; i<reassignedDayOfWeek; i++){
          blankDays.add(0);
        }

        // Create list for days
        List<Integer> days = new ArrayList<>();
        for (int i=1; i<=numOfDaysInMonth; i++){
          days.add(i);
        }

        // Add attributes to the model to be used in the view
        model.addAttribute("currentMonth", nameOfMonth);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("blanks", blankDays);
        model.addAttribute("days", days);
     
        // Create list for months
        List<Integer> years = new ArrayList<>(List.of(2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029));
        model.addAttribute("years", years);

        List<String> months = new ArrayList<>(List.of("January", "February", "March", "April", 
                                                      "May", "June", "July", "August", 
                                                      "September", "October", "November", "December"));
        model.addAttribute("months", months);

		return "calendar";
	}
	
}
