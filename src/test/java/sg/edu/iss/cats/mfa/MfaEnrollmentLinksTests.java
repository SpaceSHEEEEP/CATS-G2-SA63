package sg.edu.iss.cats.mfa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

class MfaEnrollmentLinksTests {
    @Test void enrollmentProvidesStandardOtpUriWithoutVerifyingMfaFirst() {
        UserRepository users = mock(UserRepository.class);
        TotpService totp = mock(TotpService.class);
        User principal = new User();
        principal.setUserId(42); principal.setUsername("employee42");
        when(users.findById(42)).thenReturn(Optional.of(principal));
        when(totp.newSecret()).thenReturn("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567");
        when(totp.provisionUri("employee42", "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"))
                .thenReturn("otpauth://totp/CATS%20NUS-ISS:employee42?secret=ABCDEFGHIJKLMNOPQRSTUVWXYZ234567");
        var model = new ExtendedModelMap();
        var session = new MockHttpSession();
        assertThat(new MfaController(users, totp).enroll(principal, session, model))
                .isEqualTo("mfa-enroll");
        assertThat(model.get("provisionUri").toString()).startsWith("otpauth://totp/");
        assertThat(model.get("secret")).isEqualTo("ABCDEFGHIJKLMNOPQRSTUVWXYZ234567");
        verify(totp).provisionUri("employee42", "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567");
    }
}
