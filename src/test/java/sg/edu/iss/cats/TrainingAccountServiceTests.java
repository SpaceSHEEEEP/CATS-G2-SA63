package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.model.*;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.*;

class TrainingAccountServiceTests {
    private final TrainingYearRepository years = mock(TrainingYearRepository.class);
    private final AppRepo apps = mock(AppRepo.class);
    private final HolidayRepository holidays = mock(HolidayRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TrainingAccountService service =
        new TrainingAccountService(years, apps, holidays, users);

    @Test void fullDayExcludesWeekendsAndPublicHolidaysAcrossYears() {
        Course course = new Course("Training", CourseType.INTERNAL,
            LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 4), "ISS", "ISS",
            BigDecimal.ZERO, CourseDuration.FULLDAY);
        when(holidays.existsByHolidayDate(LocalDate.of(2027, 1, 1))).thenReturn(true);
        assertThat(service.workingDaysByYear(course)).containsEntry(2026, 2.0).containsEntry(2027, 1.0);
    }

    @Test void singleInternalHalfDayCostsOnlyHalfTrainingDay() {
        Course course = new Course("Training", CourseType.INTERNAL,
            LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 30), "ISS", "ISS",
            BigDecimal.ZERO, CourseDuration.HALFDAYAM);
        assertThat(service.workingDays(course)).isEqualTo(0.5);
    }
}
