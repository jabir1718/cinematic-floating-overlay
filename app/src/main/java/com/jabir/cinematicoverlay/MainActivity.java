package com.jabir.cinematicoverlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;

public class MainActivity extends Activity {

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

        Button permissionButton = new Button(this);
        permissionButton.setText("Enable Floating Overlay");

        TextView status = new TextView(this);
        status.setText("Allow permission to display effects over other apps.");
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);

        permissionButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            } else {
                status.setText("Overlay permission is enabled!");
            }
        });

        layout.addView(title);
        layout.addView(status);
        layout.addView(permissionButton);

        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
