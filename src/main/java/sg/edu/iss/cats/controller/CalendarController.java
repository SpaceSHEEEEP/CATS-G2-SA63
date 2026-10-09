package sg.edu.iss.cats.controller;
import java.util.ArrayList;
import java.util.List;
import java.time.YearMonth;
import java.time.LocalDate;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/staff")
public class CalendarController {
  
	@GetMapping("/calendar")
	public String showCalendar(@RequestParam(name="month", required=false) String month,
    @RequestParam(name="year", required=false) Integer year, Model model) {
        // If no month and year are provided, display current month and year
        YearMonth selectedYearMonth;
        if (month == null || month.trim().isEmpty() || year == null) {
            LocalDate currentDate = LocalDate.now();
            selectedYearMonth = YearMonth.of(currentDate.getYear(), currentDate.getMonth());
            year = currentDate.getYear();
        }

        else {
            // display what was selected
            // java.time.Month is a built-in enum containing all months in caps
            // .valueOf searches that enum for matches
            // converts to upper case to match
            // YearMonth.of combines the year and month and stores them together, to be used for
            // subsequent code
            selectedYearMonth = YearMonth.of(year, java.time.Month.valueOf(month.toUpperCase()));
        }

        String nameOfMonth = selectedYearMonth.getMonth().name();
        int numOfDaysInMonth = selectedYearMonth.lengthOfMonth();
        int currentYear = selectedYearMonth.getYear();

        // Find the 1st day of the month
        LocalDate firstDayOfMonth = selectedYearMonth.atDay(1);
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
