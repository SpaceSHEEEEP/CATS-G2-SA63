package sg.edu.iss.cats.mfa;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.iss.cats.model.User;
import sg.edu.iss.cats.repository.UserRepository;

@Service
public class TotpService {
    private static final char[] B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;
    private final UserRepository users;

    public TotpService(@Value("${cats.mfa.encryption-key:}") String encodedKey, UserRepository users) {
        this.users = users;
        if (encodedKey.isBlank()) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY must be configured (base64 of 32 random bytes)");
        }
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(encodedKey); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("Invalid MFA_ENCRYPTION_KEY", ex); }
        if (decoded.length != 32) throw new IllegalStateException("MFA_ENCRYPTION_KEY must decode to 32 bytes");
        this.key = new SecretKeySpec(decoded, "AES");
        Arrays.fill(decoded, (byte) 0);
    }

    public String newSecret() {
        byte[] seed = new byte[20];
        random.nextBytes(seed);
        String result = base32(seed);
        Arrays.fill(seed, (byte) 0);
        return result;
    }

    public String provisionUri(String username, String secret) {
        String account = java.net.URLEncoder.encode(username, StandardCharsets.UTF_8);
        return "otpauth://totp/CATS%20NUS-ISS:" + account
                + "?secret=" + secret + "&issuer=CATS%20NUS-ISS&algorithm=SHA1&digits=6&period=30";
    }

    @Transactional
    public boolean activate(Integer userId, String secret, String code) {
        User user = users.findLockedById(userId).orElseThrow();
        if (user.getMfaSecretEncrypted() != null) throw new SecurityException("MFA is already enrolled");
        long step = matchingStep(secret, code, Instant.now().getEpochSecond());
        if (step < 0) return false;
        user.setMfaSecretEncrypted(encrypt(secret));
        user.setMfaLastStep(step);
        users.save(user);
        return true;
    }

    @Transactional
    public boolean verify(Integer userId, String code) {
        User user = users.findLockedById(userId).orElseThrow();
        if (user.getMfaSecretEncrypted() == null) return false;
        long step = matchingStep(decrypt(user.getMfaSecretEncrypted()), code, Instant.now().getEpochSecond());
        if (step < 0 || (user.getMfaLastStep() != null && step <= user.getMfaLastStep())) return false;
        user.setMfaLastStep(step); // Never accept a used time window again.
        users.save(user);
        return true;
    }

    private static long matchingStep(String seed, String input, long epoch) {
        if (input == null || !input.matches("[0-9]{6}")) return -1;
        byte[] decoded = decode32(seed);
        try {
            long now = epoch / 30;
            for (long step = now - 1; step <= now + 1; step++) {
                String expected = otp(decoded, step);
                if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                        input.getBytes(StandardCharsets.US_ASCII))) return step;
            }
            return -1;
        } finally { Arrays.fill(decoded, (byte) 0); }
    }

    private static String otp(byte[] seed, long step) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(seed, "HmacSHA1"));
            byte[] hash = hmac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
            return String.format(java.util.Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (Exception ex) { throw new IllegalStateException("TOTP unavailable", ex); }
    }

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] payload = cipher.doFinal(plaintext.getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + payload.length)
                    .put(iv).put(payload).array());
        } catch (Exception ex) { throw new IllegalStateException("MFA encryption failed", ex); }
    }

    private String decrypt(String ciphertext) {
        try {
            byte[] bytes = Base64.getDecoder().decode(ciphertext);
            if (bytes.length < 29) throw new IllegalArgumentException("Corrupt seed");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOf(bytes, 12)));
            return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.US_ASCII);
        } catch (Exception ex) { throw new IllegalStateException("MFA decryption failed; verify stable encryption key", ex); }
    }

    static String base32(byte[] bytes) {
        StringBuilder output = new StringBuilder(); int buffer = 0, bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xff); bits += 8;
            while (bits >= 5) { bits -= 5; output.append(B32[(buffer >> bits) & 31]); }
        }
        if (bits > 0) output.append(B32[(buffer << (5 - bits)) & 31]);
        return output.toString();
    }

    static byte[] decode32(String value) {
        if (value == null || !value.matches("[A-Z2-7]{32}")) throw new IllegalArgumentException("Invalid TOTP seed");
        byte[] out = new byte[20]; int buffer = 0, bits = 0, pos = 0;
        for (char c : value.toCharArray()) {
            int n = c >= 'A' && c <= 'Z' ? c - 'A' : c - '2' + 26;
            buffer = (buffer << 5) | n; bits += 5;
            if (bits >= 8) { bits -= 8; out[pos++] = (byte) ((buffer >> bits) & 0xff); }
        }
        return out;
    }
}
