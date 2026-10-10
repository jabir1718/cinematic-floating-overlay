package com.jabir.cinematicoverlay;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private CinematicView overlayView;
    private Socket socket;

    private final Handler handler =
        new Handler(Looper.getMainLooper());

    private final List<String> comments = new ArrayList<>();
    private final List<String> events = new ArrayList<>();

    private String connection = "CONNECTING";
    private int totalLikes = 0;
    private int eventCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager =
            (WindowManager) getSystemService(WINDOW_SERVICE);

        overlayView = new CinematicView();

        int windowType =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.START;

        try {
            windowManager.addView(overlayView, params);
            connectToServer();
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void connectToServer() {
        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay = 2000;
            options.timeout = 5000;

            socket = IO.socket(
                "http://127.0.0.1:3000",
                options
            );

            socket.on(Socket.EVENT_CONNECT, args -> {
                connection = "CONNECTED";
                refresh();
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                connection = "DISCONNECTED";
                refresh();
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                connection = "SERVER OFFLINE";
                refresh();
            });

            socket.on("connection-status", args -> {
                String value = stringify(args);
                connection = value.length() > 45
                    ? value.substring(0, 45)
                    : value;
                refresh();
            });

            socket.on("live-event", args -> {
                addEvent("LIVE", stringify(args));
            });

            socket.on("chat", args -> {
                addComment(stringify(args));
            });

            socket.on("like", args -> {
                totalLikes++;
                addEvent("LIKE", stringify(args));
            });

            socket.on("gift", args -> {
                addEvent("GIFT", stringify(args));
            });

            socket.on("member", args -> {
                addEvent("JOIN", stringify(args));
            });

            socket.on("social", args -> {
                addEvent("SOCIAL", stringify(args));
            });

            socket.on("roomUser", args -> {
                addEvent("VIEWERS", stringify(args));
            });

            socket.connect();

        } catch (Exception e) {
            connection = "CONNECTION ERROR";
            refresh();
        }
    }

    private String stringify(Object[] args) {
        if (args == null || args.length == 0) {
            return "Live event received";
        }

        Object value = args[0];

        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;

            String[] keys = {
                "nickname",
                "uniqueId",
                "user",
                "comment",
                "text",
                "giftName",
                "gift_name",
                "count",
                "total"
            };

            StringBuilder result = new StringBuilder();

            for (String key : keys) {
                if (obj.has(key)) {
                    String text = obj.optString(key, "");

                    if (!text.isEmpty()
                            && !"null".equals(text)) {
                        if (result.length() > 0) {
                            result.append(" • ");
                        }

                        result.append(key)
                              .append(": ")
                              .append(text);
                    }
                }
            }

            if (result.length() > 0) {
                return result.toString();
            }
        }

        return String.valueOf(value);
    }

    private void addComment(String text) {
        synchronized (comments) {
            comments.add(0, shorten(text, 100));

            while (comments.size() > 5) {
                comments.remove(comments.size() - 1);
            }
        }

        refresh();
    }

    private void addEvent(String type, String text) {
        synchronized (events) {
            events.add(0, type + "  |  " + shorten(text, 90));

            while (events.size() > 5) {
                events.remove(events.size() - 1);
            }
        }

        eventCount++;
        refresh();
    }

    private String shorten(String text, int max) {
        if (text == null) return "";

        text = text.replace("\n", " ")
                   .replace("\r", " ");

        if (text.length() > max) {
            return text.substring(0, max - 3) + "...";
        }

        return text;
    }

    private void refresh() {
        handler.post(() -> {
            if (overlayView != null) {
                overlayView.invalidate();
            }
        });
    }

    private class CinematicView extends View {

        private final Paint paint = new Paint(
            Paint.ANTI_ALIAS_FLAG
        );

        private final float density =
            getResources().getDisplayMetrics().density;

        private long startedAt = System.currentTimeMillis();

        CinematicView() {
            super(OverlayService.this);

            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            setBackgroundColor(Color.TRANSPARENT);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (overlayView == CinematicView.this) {
                        invalidate();
                        handler.postDelayed(this, 150);
                    }
                }
            }, 150);
        }

        private float dp(float value) {
            return value * density;
        }

        private void neonText(
                Canvas canvas,
                String text,
                float x,
                float y,
                float size,
                int color
        ) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.create(
                "sans-serif-condensed",
                Typeface.BOLD
            ));
            paint.setTextSize(dp(size));
            paint.setColor(color);
            paint.setShadowLayer(dp(7), 0, 0, color);

            canvas.drawText(text, x, y, paint);

            paint.clearShadowLayer();
        }

        private void neonLine(
                Canvas canvas,
                float x1,
                float y1,
                float x2,
                float y2,
                int color
        ) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setColor(color);
            paint.setShadowLayer(dp(8), 0, 0, color);

            canvas.drawLine(x1, y1, x2, y2, paint);

            paint.clearShadowLayer();
            paint.setStyle(Paint.Style.FILL);
        }

        private void outline(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                int color
        ) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.2f));
            paint.setColor(color);
            paint.setShadowLayer(dp(8), 0, 0, color);

            canvas.drawRoundRect(
                new RectF(left, top, right, bottom),
                dp(8),
                dp(8),
                paint
            );

            paint.clearShadowLayer();
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Transparent background: never paint a full-screen box.
            int width = getWidth();
            int height = getHeight();

            if (width <= 0 || height <= 0) return;

            float margin = dp(12);
            float left = margin;
            float right = width - margin;

            long elapsed =
                System.currentTimeMillis() - startedAt;

            boolean pulse = (elapsed / 500) % 2 == 0;

            int cyan = Color.rgb(0, 245, 255);
            int pink = Color.rgb(255, 35, 150);
            int purple = Color.rgb(155, 70, 255);
            int white = Color.WHITE;

            int primary = pulse ? cyan : purple;

            // Top cinematic header.
            float headerBottom = dp(48);

            outline(
                canvas,
                left,
                dp(12),
                right,
                headerBottom,
                primary
            );

            neonText(
                canvas,
                "CINEMATIC  /  LIVE",
                left + dp(12),
                dp(32),
                16,
                white
            );

            neonText(
                canvas,
                connection,
                right - dp(125),
                dp(32),
                10,
                connection.equals("CONNECTED")
                    ? cyan : pink
            );

            neonLine(
                canvas,
                left,
                headerBottom + dp(5),
                right,
                headerBottom + dp(5),
                primary
            );

            // Left-side comments panel.
            float panelTop = dp(68);
            float panelBottom = Math.min(
                height * 0.48f,
                dp(310)
            );

            float panelWidth = Math.min(
                width * 0.58f,
                dp(265)
            );

            outline(
                canvas,
                left,
                panelTop,
                left + panelWidth,
                panelBottom,
                cyan
            );

            neonText(
                canvas,
                "LIVE FEED",
                left + dp(9),
                panelTop + dp(20),
                12,
                cyan
            );

            List<String> commentCopy;

            synchronized (comments) {
                commentCopy = new ArrayList<>(comments);
            }

            float commentY = panelTop + dp(43);

            for (String comment : commentCopy) {
                if (commentY > panelBottom - dp(8)) break;

                neonText(
                    canvas,
                    shorten(comment, 34),
                    left + dp(9),
                    commentY,
                    10,
                    white
                );

                commentY += dp(23);
            }

            if (commentCopy.isEmpty()) {
                neonText(
                    canvas,
                    "Waiting for comments...",
                    left + dp(9),
                    commentY,
                    10,
                    Color.LTGRAY
                );
            }

            // Right-side event rail.
            float railWidth = Math.min(
                width * 0.40f,
                dp(170)
            );

            float railLeft = right - railWidth;
            float railTop = dp(92);
            float railBottom = Math.min(
                height * 0.68f,
                dp(440)
            );

            outline(
                canvas,
                railLeft,
                railTop,
                right,
                railBottom,
                pink
            );

            neonText(
                canvas,
                "EVENTS",
                railLeft + dp(8),
                railTop + dp(20),
                12,
                pink
            );

            neonText(
                canvas,
                "LIKES: " + totalLikes,
                railLeft + dp(8),
                railTop + dp(39),
                10,
                cyan
            );

            neonText(
                canvas,
                "EVENTS: " + eventCount,
                railLeft + dp(8),
                railTop + dp(55),
                10,
                white
            );

            List<String> eventCopy;

            synchronized (events) {
                eventCopy = new ArrayList<>(events);
            }

            float eventY = railTop + dp(78);

            for (String event : eventCopy) {
                if (eventY > railBottom - dp(8)) break;

                neonText(
                    canvas,
                    shorten(event, 22),
                    railLeft + dp(8),
                    eventY,
                    9,
                    white
                );

                eventY += dp(27);
            }

            // Bottom cinematic edge marks.
            float bottom = height - dp(22);

            neonLine(
                canvas,
                left,
                bottom,
                left + dp(70),
                bottom,
                cyan
            );

            neonLine(
                canvas,
                right - dp(70),
                bottom,
                right,
                bottom,
                pink
            );

            neonText(
                canvas,
                "LIVE EVENT SYSTEM",
                width / 2f - dp(62),
                bottom - dp(5),
                9,
                white
            );
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }

        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }

            overlayView = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
