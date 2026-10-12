package sg.edu.iss.cats.mobile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

@Service
public class MobileAccessService {
    private final SecureRandom random = new SecureRandom();
    private final MobilePairingRepository pairings;
    private final MobileDeviceTokenRepository devices;
    private final UserRepository users;
    private final int tokenDays;

    public MobileAccessService(MobilePairingRepository pairings, MobileDeviceTokenRepository devices,
            UserRepository users, @Value("${cats.mobile.token-days:30}") int tokenDays) {
        this.pairings = pairings; this.devices = devices; this.users = users;
        if (tokenDays < 1 || tokenDays > 90) throw new IllegalArgumentException("MOBILE_TOKEN_DAYS must be 1–90");
        this.tokenDays = tokenDays;
    }

    private static LocalDateTime utcNow() { return LocalDateTime.now(ZoneOffset.UTC); }
    private String secureRandom(int bytes) {
        byte[] b = new byte[bytes]; random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
    private static String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.US_ASCII))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @Transactional
    public String issueCode(Integer userId) {
        User owner = users.findById(userId).orElseThrow();
        if (!owner.isEnabled() || owner.getMfaSecretEncrypted() == null)
            throw new SecurityException("Complete MFA enrolment before pairing a device");
        String code = secureRandom(24); // 192 bits, not a human-guessable PIN.
        MobilePairing pairing = new MobilePairing(); pairing.setUser(owner);
        pairing.setCodeHash(hash(code)); pairing.setCreatedAt(utcNow());
        pairing.setExpiresAt(utcNow().plusMinutes(5)); pairings.save(pairing);
        return code;
    }

    @Transactional
    public String redeem(String code, String deviceName) {
        if (code == null || !code.matches("[A-Za-z0-9_-]{32}"))
            throw new SecurityException("Invalid or expired pairing code");
        MobilePairing pairing = pairings.findByCodeHash(hash(code))
                .orElseThrow(() -> new SecurityException("Invalid or expired pairing code"));
        if (!pairing.getExpiresAt().isAfter(utcNow()) || !pairing.getUser().isEnabled())
            throw new SecurityException("Invalid or expired pairing code");
        pairings.delete(pairing); pairings.flush(); // consumed transactionally
        String token = secureRandom(32);
        MobileDeviceToken device = new MobileDeviceToken();
        device.setUser(pairing.getUser()); device.setTokenHash(hash(token));
        String name = deviceName == null ? "CATS Mobile" : deviceName.strip();
        device.setDeviceName(name.isEmpty() ? "CATS Mobile" : name.substring(0, Math.min(80, name.length())));
        device.setIssuedAt(utcNow()); device.setExpiresAt(utcNow().plusDays(tokenDays));
        devices.save(device);
        return token;
    }

    @Transactional(readOnly = true)
    public User verifyBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) throw new SecurityException("Bearer token required");
        String token = authorization.substring(7);
        if (!token.matches("[A-Za-z0-9_-]{43}")) throw new SecurityException("Invalid device token");
        MobileDeviceToken found = devices.findByTokenHash(hash(token))
                .orElseThrow(() -> new SecurityException("Unknown device token"));
        if (found.isRevoked() || !found.getExpiresAt().isAfter(utcNow()) || !found.getUser().isEnabled())
            throw new SecurityException("Mobile device access revoked or expired");
        return found.getUser();
    }

    @Transactional
    public void revoke(Integer userId, Long deviceId) {
        MobileDeviceToken token = devices.findById(deviceId).orElseThrow();
        if (!token.getUser().getUserId().equals(userId)) throw new SecurityException("Not your device");
        token.setRevoked(true); devices.save(token);
    }

    @Transactional(readOnly = true)
    public java.util.List<MobileDeviceToken> devices(Integer userId) {
        return devices.findByUser_UserIdOrderByIssuedAtDesc(userId);
    }
}
