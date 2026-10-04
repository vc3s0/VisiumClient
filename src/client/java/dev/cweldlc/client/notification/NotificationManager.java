package dev.cweldlc.client.notification;

public final class NotificationManager {

    public record Notification(String title, String message, int color, long durationMs, long timestamp) {
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > durationMs;
        }

        public float getAlpha() {
            long elapsed = System.currentTimeMillis() - timestamp;
            if (elapsed < 200) {
                return elapsed / 200.0f;
            }
            long remaining = durationMs - elapsed;
            if (remaining < 300) {
                return Math.max(0.0f, remaining / 300.0f);
            }
            return 1.0f;
        }
    }

    private static Notification current;

    private NotificationManager() {}

    public static void post(String title, String message, int color) {
        current = new Notification(title, message, color, 2500L, System.currentTimeMillis());
        dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.APPLEPAY, 1.0f, 0.7f);
    }

    public static void post(String title, String message, boolean state) {
        int color = state ? 0xFF22C55E : 0xFFEF4444;
        post(title, message, color);
    }

    public static Notification getCurrent() {
        if (current != null && current.isExpired()) {
            current = null;
        }
        return current;
    }
}
