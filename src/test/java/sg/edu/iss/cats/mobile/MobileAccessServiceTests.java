package sg.edu.iss.cats.mobile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

class MobileAccessServiceTests {
    private final MobilePairingRepository pairings = mock(MobilePairingRepository.class);
    private final MobileDeviceTokenRepository devices = mock(MobileDeviceTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MobileAccessService service = new MobileAccessService(pairings, devices, users, 30);

    @Test void cannotIssuePairCodeUntilWebMfaIsEnrolled() {
        User user = new User(); user.setUserId(4);
        when(users.findById(4)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.issueCode(4))
                .isInstanceOf(SecurityException.class);
        verify(pairings, never()).save(any());
    }

    @Test void unauthorizedBearerCannotObtainMobileRecords() {
        assertThatThrownBy(() -> service.verifyBearer("Bearer definitely-not-a-valid-token"))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.verifyBearer(null))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(devices);
    }
}
