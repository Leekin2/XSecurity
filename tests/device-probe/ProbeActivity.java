package io.github.leekin2.xsecurity.testprobe;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

/** A disposable, offline test screen. It never reads user data or requests permissions. */
public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER);
        text.setTextSize(30);
        text.setTextColor(0xff101010);
        text.setBackgroundColor(0xff48d888);
        text.setText("XSecurity TEST\n\nFLAG_SECURE\n\nNo personal data");
        setContentView(text);
    }
}
