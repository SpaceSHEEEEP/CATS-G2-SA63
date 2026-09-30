package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import sg.edu.iss.cats.model.Application;
import sg.edu.iss.cats.model.ApplicationStatus;
import sg.edu.iss.cats.model.Course;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.service.BudgetService;

class BudgetServiceTests {

    @Test
    void remainingBudgetCountsCurrentYearActiveFeesOnly() {
        LocalDate thisYear = LocalDate.now().withDayOfYear(1);
        User user = new User();
        user.setBudget(new BigDecimal("2000.00"));

        Application applied = new Application();
        applied.setApplicationStatus(ApplicationStatus.APPLIED);
        applied.setExternalStartDate(thisYear);
        applied.setExternalFee(new BigDecimal("400.00"));

        Course course = new Course();
        course.setStartDate(thisYear);
        course.setFee(new BigDecimal("600.00"));
        Application approved = new Application();
        approved.setApplicationStatus(ApplicationStatus.APPROVED);
        approved.setCourse(course);

        Application cancelled = new Application();
        cancelled.setApplicationStatus(ApplicationStatus.CANCELLED);
        cancelled.setExternalStartDate(thisYear);
        cancelled.setExternalFee(new BigDecimal("300.00"));

        Application lastYear = new Application();
        lastYear.setApplicationStatus(ApplicationStatus.COMPLETED);
        lastYear.setExternalStartDate(thisYear.minusYears(1));
        lastYear.setExternalFee(new BigDecimal("200.00"));

        BigDecimal remaining = new BudgetService().remainingBudget(user,
                List.of(applied, approved, cancelled, lastYear));

        assertThat(remaining).isEqualByComparingTo("1000.00");
    }
}
