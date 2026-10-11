package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.repository.*;
import sg.edu.iss.cats.service.HolidayManagementService;

class HolidayManagementServiceTests {
    private final HolidayRepository holidays = mock(HolidayRepository.class);
    private final AppRepo apps = mock(AppRepo.class);
    private final HolidayManagementService service = new HolidayManagementService(holidays, apps);

    @Test void newHolidayCannotRetroactivelyChangeBookedCourseDays() {
        LocalDate day = LocalDate.of(2027, 1, 1);
        when(apps.hasActiveApplicationOnDate(day)).thenReturn(true);
        assertThatThrownBy(() -> service.save(null, day, "New holiday"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("balances");
        verify(holidays, never()).save(any());
    }

    @Test void duplicateHolidayDateIsRejected() {
        LocalDate day = LocalDate.of(2027, 1, 1);
        when(holidays.existsByHolidayDateAndHolidayIdNot(day, -1)).thenReturn(true);
        assertThatThrownBy(() -> service.save(null, day, "Duplicate"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already");
    }
}
