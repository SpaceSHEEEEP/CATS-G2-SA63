package sg.edu.iss.cats.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@CapacitorPlugin(name = "CatsVault")
public final class CatsVaultPlugin extends Plugin {
    private static final String KEY_ALIAS = "sg.edu.iss.cats.mobile.v1";
    private static final String FILE = "cats_vault_v1";
    private static final int GCM_TAG_BITS = 128;

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return gen.generateKey();
    }

    private String seal(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] data = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        ByteBuffer combined = ByteBuffer.allocate(1 + iv.length + data.length);
        combined.put((byte) iv.length).put(iv).put(data);
        return Base64.encodeToString(combined.array(), Base64.NO_WRAP);
    }

    private String open(String envelope) throws Exception {
        byte[] bytes = Base64.decode(envelope, Base64.NO_WRAP);
        ByteBuffer combined = ByteBuffer.wrap(bytes);
        int size = combined.get() & 0xff;
        if (size != 12 || bytes.length < 1 + size + 16) throw new SecurityException("Corrupt encrypted vault entry");
        byte[] iv = new byte[size]; combined.get(iv);
        byte[] ciphertext = new byte[combined.remaining()]; combined.get(ciphertext);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private String safeKey(PluginCall call) {
        String name = call.getString("key", "");
        if (!name.matches("[A-Za-z][A-Za-z0-9_-]{0,50}"))
            throw new IllegalArgumentException("Invalid vault key");
        return name;
    }

    @PluginMethod
    public void set(PluginCall call) {
        try {
            String name = safeKey(call);
            String value = call.getString("value");
            if (value == null || value.length() > 500000) throw new IllegalArgumentException("Invalid vault value");
            boolean success = prefs().edit().putString(name, seal(value)).commit();
            if (!success) throw new IllegalStateException("Encrypted write did not complete");
            call.resolve();
        } catch (Exception ex) { call.reject("Secure vault save failed"); }
    }

    @PluginMethod
    public void get(PluginCall call) {
        try {
            String envelope = prefs().getString(safeKey(call), null);
            JSObject response = new JSObject();
            response.put("value", envelope == null ? JSONObject.NULL : open(envelope));
            call.resolve(response);
        } catch (Exception ex) { call.reject("Secure vault unlock failed"); }
    }

    @PluginMethod
    public void remove(PluginCall call) {
        try {
            prefs().edit().remove(safeKey(call)).apply();
            call.resolve();
        } catch (Exception ex) { call.reject("Secure vault removal failed"); }
    }
}
