package sg.edu.iss.cats;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/** Verifies the unidirectional one-to-one rule in Flyway V1, not only in Java. */
@DataJpaTest
@ActiveProfiles("test")
@Sql("/repository-fixtures.sql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FeeClaimCardinalityTests {
    @Autowired private JdbcTemplate jdbc;

    @Test void rejectedClaimAlsoOccupiesUniqueApplicationSlot() {
        jdbc.update("""
                INSERT INTO application (id, status, user_reason, course_course_id, user_id)
                VALUES (98, 'COMPLETED', 'Fixture completed training', 1, 3)
                """);
        jdbc.update("""
                INSERT INTO fee_claim (application_id, employee_user_id, amount, status, submitted_on)
                VALUES (98, 3, 450.00, 'REJECTED', '2026-10-08')
                """);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO fee_claim (application_id, employee_user_id, amount, status, submitted_on)
                VALUES (98, 3, 450.00, 'SUBMITTED', '2026-10-09')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
