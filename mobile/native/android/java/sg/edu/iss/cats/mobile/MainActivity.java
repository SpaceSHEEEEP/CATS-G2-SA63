package sg.edu.iss.cats.mobile;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public final class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(CatsVaultPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
