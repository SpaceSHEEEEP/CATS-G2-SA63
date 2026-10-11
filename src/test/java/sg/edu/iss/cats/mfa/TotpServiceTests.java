package sg.edu.iss.cats.mfa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

class TotpServiceTests {
    private static String referenceOtp(byte[] seed, long step) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(seed, "HmacSHA1"));
        byte[] bytes = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
        int offset = bytes[19] & 15;
        int value = ((bytes[offset] & 127) << 24) | ((bytes[offset + 1] & 255) << 16)
                | ((bytes[offset + 2] & 255) << 8) | (bytes[offset + 3] & 255);
        return String.format("%06d", value % 1_000_000);
    }

    @Test void enrollmentEncryptsAndRejectsReusedTimeStep() throws Exception {
        User user = new User(); user.setUserId(14);
        UserRepository repository = mock(UserRepository.class);
        when(repository.findLockedById(14)).thenReturn(Optional.of(user));
        String testKey = Base64.getEncoder().encodeToString(new byte[32]);
        TotpService totp = new TotpService(testKey, repository);
        String seed = totp.newSecret();
        assertThat(seed).matches("[A-Z2-7]{32}");
        String code = referenceOtp(TotpService.decode32(seed), Instant.now().getEpochSecond() / 30);
        assertThat(totp.activate(14, seed, code)).isTrue();
        assertThat(user.getMfaSecretEncrypted()).isNotNull().doesNotContain(seed);
        assertThat(totp.verify(14, code)).isFalse();
        verify(repository, atLeastOnce()).save(user);
    }

    @Test void setupKeyAndUriContainNoPassword() {
        UserRepository repository = mock(UserRepository.class);
        TotpService totp = new TotpService(Base64.getEncoder().encodeToString(new byte[32]), repository);
        String uri = totp.provisionUri("employee@example.org", totp.newSecret());
        assertThat(uri).startsWith("otpauth://totp/").contains("algorithm=SHA1").contains("period=30");
    }
}
