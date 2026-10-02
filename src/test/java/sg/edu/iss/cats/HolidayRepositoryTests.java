package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.iss.cats.repository.HolidayRepository;

//Uses sample users from data.sql and the test-profile MySQL database.
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

public class HolidayRepositoryTests {

  @Autowired
  private HolidayRepository holidayRepository;

  @Test
  @DisplayName("Returns TRUE when the holiday date exists in the database")
  void testExistsByHolidayDate() {
    LocalDate date = LocalDate.of(2027, 10, 28); //
    boolean exists = holidayRepository.existsByHolidayDate(date);
    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("Returns False when the date does not exists in the holiday table database")
  void testDoesNotExistsByHolidayDate() {
    LocalDate date = LocalDate.of(2027, 11, 28); //
    boolean doesnotexists = holidayRepository.existsByHolidayDate(date);
    assertThat(doesnotexists).isFalse();
  }
}
