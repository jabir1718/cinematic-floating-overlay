package com.jabir.cinematicoverlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
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
        layout.setPadding(28, 28, 28, 28);
        layout.setBackgroundColor(Color.rgb(10, 10, 22));

        TextView title = new TextView(this);
        title.setText("CINEMATIC OVERLAY");
        title.setTextSize(25);
        title.setTextColor(Color.CYAN);
        title.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        description.setText(
            "Transparent LIVE event overlay\n" +
            "Neon effects • Comments • Likes • Gifts"
        );
        description.setTextSize(15);
        description.setTextColor(Color.WHITE);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 24, 0, 24);

        status = new TextView(this);
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setText("Enable overlay permission to begin.");

        Button start = new Button(this);
        start.setText("ENABLE / START OVERLAY");

        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent permissionIntent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(permissionIntent);
                return;
            }

            try {
                Intent serviceIntent =
                    new Intent(this, OverlayService.class);

                startService(serviceIntent);

                status.setText(
                    "Overlay started. Open TikTok LIVE."
                );

            } catch (Exception e) {
                status.setText("Error: " + e.getMessage());
            }
        });

        layout.addView(title);
        layout.addView(description);
        layout.addView(status);
        layout.addView(start);

        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (status != null && Settings.canDrawOverlays(this)) {
            status.setText(
                "Permission enabled. Tap START OVERLAY."
            );
        }
    }
}
