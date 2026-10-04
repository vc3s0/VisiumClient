package dev.cweldlc.client.theme;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ThemeManager {

    public enum Theme {
        BLACK("Czarny", "Dark"),
        WHITE("Biały", "Light");

        private final String displayName;
        private final String enName;

        Theme(String displayName, String enName) {
            this.displayName = displayName;
            this.enName = enName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEnName() {
            return enName;
        }
    }

    private static Theme currentTheme = Theme.BLACK;
    private static File configFile;

    // Fluid Animation Engine State
    private static float themeProgress = 0.0f; // 0.0f = pure Black, 1.0f = pure White
    private static long lastTime = System.currentTimeMillis();

    // Shockwave Ripple State
    private static float rippleX = -1;
    private static float rippleY = -1;
    private static long rippleStartTime = 0;
    private static final float RIPPLE_DURATION = 550.0f; // ms
    private static boolean rippleToWhite = true;

    static {
        load();
        themeProgress = isWhite() ? 1.0f : 0.0f;
    }

    private static File getConfigFile() {
        if (configFile == null) {
            File gameDir = Minecraft.getInstance().gameDirectory;
            if (gameDir == null) {
                gameDir = new File(".");
            }
            configFile = new File(gameDir, "config/visiumclient.json");
        }
        return configFile;
    }

    public static Theme getTheme() {
        return Theme.BLACK;
    }

    public static boolean isDark() {
        return true;
    }

    public static boolean isWhite() {
        return false;
    }

    public static void setTheme(Theme theme) {
        currentTheme = Theme.BLACK;
    }

    public static void toggleTheme() {
        // Locked to pure black theme
    }

    public static void startRipple(float originX, float originY) {
        // No ripple
    }

    public static void load() {
        currentTheme = Theme.BLACK;
    }

    public static void save() {
    }

    // --- Fluid Animation Progress ---

    public static float getTransitionFactor() {
        return 0.0f; // Pure dark permanently
    }

    // --- Dynamic Color Palettes (Interpolated in Real-Time) ---

    public static int getGlassBaseColor(float hoverProgress) {
        // RockReady DARK background: (12, 12, 12) -> #0C0C0C with solid 92-96% opacity
        int darkAlpha = (int) (0xEA + (hoverProgress * 0x0B));
        return (darkAlpha << 24) | 0x0C0C0C;
    }

    public static int getGlassTopHighlight(float hoverProgress) {
        int darkAlpha = (int) (0x12 + (hoverProgress * 0x1A));
        return (darkAlpha << 24) | 0xFFFFFF;
    }

    public static int getGlassBorderColor(float hoverProgress) {
        return 0; // Borderless
    }

    public static int getPrimaryTextColor(float hoverProgress) {
        // RockReady DARK text: pure white (255, 255, 255)
        return lerpColor(0xFFE5E7EB, 0xFFFFFFFF, hoverProgress);
    }

    public static int getSecondaryTextColor(float hoverProgress) {
        return lerpColor(0xFF9CA3AF, 0xFFD1D5DB, hoverProgress);
    }

    public static int getTitleColor() {
        return 0xFFFFFFFF;
    }

    public static int getSubtitleColor() {
        return 0xFF9CA3AF;
    }

    public static int getOverlayColor() {
        return 0x70000000;
    }

    public static int getVignetteTop() {
        return 0x70000000;
    }

    public static int getVignetteBottom() {
        return 0x70000000;
    }

    // --- Interactive LiquidGlass Ripple Shockwave ---

    public static void renderThemeRipple(GuiGraphics graphics, int screenWidth, int screenHeight) {
        // No theme ripple needed
    }

    public static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
