package com.jabir.cinematicoverlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Cinematic Floating Overlay");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("Enable permission to start the floating panel.");
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);

        Button startButton = new Button(this);
        startButton.setText("Enable / Start Overlay");

        startButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            } else {
                try {
                    startService(new Intent(this, OverlayService.class));
                    status.setText("Floating panel started!");
                } catch (Exception e) {
                    status.setText("Could not start overlay: " + e.getMessage());
                }
            }
        });

        layout.addView(title);
        layout.addView(status);
        layout.addView(startButton);

        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (status != null && Settings.canDrawOverlays(this)) {
            status.setText("Permission enabled. Tap the button to start overlay.");
        }
    }
}
