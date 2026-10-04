package dev.cweldlc.client.gui.widgets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.math.Axis;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class AppleClockWidget extends AbstractWidget {

    public enum ClockMode {
        DIGITAL("Cyfrowy"),
        ANALOG("Analogowy");

        private final String label;

        ClockMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final DateTimeFormatter TIME_FORMAT_SHORT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMAT_SEC = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMAT_FULL = DateTimeFormatter.ofPattern("EEEE, d MMMM", new Locale("pl", "PL"));
    private static final DateTimeFormatter DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("EEE, d MMM", new Locale("pl", "PL"));

    private static ClockMode clockMode = ClockMode.DIGITAL;
    private static boolean showSeconds = false;
    private static File configFile;

    static {
        loadConfig();
    }

    private static final float BASE_HEIGHT = 44.0f;
    private static final float EXPANDED_HEIGHT = 114.0f;

    private boolean isExpanded = false;
    private float expandProgress = 0.0f;
    private float hoverProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();

    public AppleClockWidget(int x, int y, int width, int height) {
        super(x, y, width, (int) BASE_HEIGHT, Component.literal("Clock Widget"));
    }

    public float getCurrentHeight() {
        float t = Math.max(0.0f, Math.min(1.0f, expandProgress));
        // Apple fluid spring ease
        float factor = 1.0f - (1.0f - t) * (1.0f - t);
        return BASE_HEIGHT + (EXPANDED_HEIGHT - BASE_HEIGHT) * factor;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        float h = getCurrentHeight();
        return this.visible
                && mouseX >= (double) getX()
                && mouseX < (double) (getX() + getWidth())
                && mouseY >= (double) getY()
                && mouseY < (double) (getY() + h);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) {
            return false;
        }

        boolean insideBase = mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + BASE_HEIGHT;
        boolean insideWidget = isMouseOver(mouseX, mouseY);

        // 1. Right Click anywhere on the widget toggles expansion
        if (button == 1 && insideWidget) {
            this.isExpanded = !this.isExpanded;
            playClickSound(1.3f);
            return true;
        }

        // 2. Left Click interactions
        if (button == 0) {
            if (isExpanded && expandProgress > 0.4f) {
                // Segmented buttons: Cyfrowy vs Analogowy
                float segX = getX() + 10;
                float segY = getY() + 60;
                float segW = getWidth() - 20;
                float segH = 20;

                float btn1W = (segW - 4) / 2.0f;
                float btn2X = segX + btn1W + 4;

                // Clicked "Cyfrowy"
                if (mouseX >= segX && mouseX <= segX + btn1W && mouseY >= segY && mouseY <= segY + segH) {
                    if (clockMode != ClockMode.DIGITAL) {
                        clockMode = ClockMode.DIGITAL;
                        saveConfig();
                        playClickSound(1.1f);
                    }
                    return true;
                }

                // Clicked "Analogowy"
                if (mouseX >= btn2X && mouseX <= btn2X + btn1W && mouseY >= segY && mouseY <= segY + segH) {
                    if (clockMode != ClockMode.ANALOG) {
                        clockMode = ClockMode.ANALOG;
                        saveConfig();
                        playClickSound(1.1f);
                    }
                    return true;
                }

                // Seconds toggle row
                float secY = getY() + 86;
                float secH = 20;
                if (mouseX >= segX && mouseX <= segX + segW && mouseY >= secY && mouseY <= secY + secH) {
                    showSeconds = !showSeconds;
                    saveConfig();
                    playClickSound(showSeconds ? 1.4f : 0.9f);
                    return true;
                }
            }

            // Clicking outside while expanded closes it smoothly
            if (!insideWidget && isExpanded) {
                this.isExpanded = false;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch)
        );
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastTime) / 1000.0f);
        lastTime = now;

        boolean hovered = this.isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1.0f : 0.0f;
        this.hoverProgress += (targetHover - this.hoverProgress) * Math.min(1.0f, dt * 10.0f);

        float targetExpand = isExpanded ? 1.0f : 0.0f;
        this.expandProgress += (targetExpand - this.expandProgress) * Math.min(1.0f, dt * 12.0f);

        float curH = getCurrentHeight();
        this.height = (int) Math.ceil(curH);

        // 1. Sleek Apple LiquidGlass Container (morphs height on expansion)
        GlassRenderUtil.drawGlassPanel(
                graphics,
                getX(),
                getY(),
                getWidth(),
                (int) curH,
                12,
                hovered,
                hoverProgress
        );

        ZonedDateTime zdt = ZonedDateTime.now();
        int hour = zdt.getHour();
        int min = zdt.getMinute();
        int sec = zdt.getSecond();
        int millis = (int) (now % 1000);

        int primaryColor = ThemeManager.getTitleColor();
        int secondaryColor = ThemeManager.getSecondaryTextColor(hoverProgress);

        // 2. Render Clock Face (Digital or Analog)
        if (clockMode == ClockMode.DIGITAL) {
            renderDigitalFace(graphics, zdt, primaryColor, secondaryColor);
        } else {
            renderAnalogFace(graphics, zdt, hour, min, sec, millis, primaryColor, secondaryColor);
        }

        // 3. Subtle Right-Click Hint Dots (•••) on hover
        if (hoverProgress > 0.05f && expandProgress < 0.15f) {
            float hintAlpha = hoverProgress * (1.0f - expandProgress / 0.15f);
            int dotColor = applyAlpha(secondaryColor, hintAlpha * 0.6f);
            float dotY = getY() + 8.0f;
            float dotX = getX() + getWidth() - 14.0f;
            GlassRenderUtil.fillRoundedRect(graphics, dotX, dotY, 2.0f, 2.0f, 1.0f, dotColor);
            GlassRenderUtil.fillRoundedRect(graphics, dotX + 3.5f, dotY, 2.0f, 2.0f, 1.0f, dotColor);
            GlassRenderUtil.fillRoundedRect(graphics, dotX + 7.0f, dotY, 2.0f, 2.0f, 1.0f, dotColor);
        }

        // 4. Customization Menu Drawer (Revealed smoothly on expansion)
        if (expandProgress > 0.05f) {
            renderCustomizationMenu(graphics, mouseX, mouseY, primaryColor, secondaryColor);
        }
    }

    private void renderDigitalFace(GuiGraphics graphics, ZonedDateTime zdt, int primaryColor, int secondaryColor) {
        String dateStr = zdt.format(DATE_FORMAT_FULL);
        if (!dateStr.isEmpty()) {
            dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
        }
        String timeStr = zdt.format(showSeconds ? TIME_FORMAT_SEC : TIME_FORMAT_SHORT);

        // Date header
        MsdfRenderer.renderText(
                Fonts.medium(),
                dateStr,
                8.0f,
                secondaryColor,
                graphics.pose().last().pose(),
                getX() + 12,
                getY() + 9,
                0.0f
        );

        // Digital Time
        float fontSize = showSeconds ? 16.0f : 18.0f;
        MsdfRenderer.renderText(
                Fonts.roundBold(),
                timeStr,
                fontSize,
                primaryColor,
                graphics.pose().last().pose(),
                getX() + 12,
                getY() + (showSeconds ? 22 : 21),
                0.0f
        );
    }

    private void renderAnalogFace(GuiGraphics graphics, ZonedDateTime zdt, int hour, int min, int sec, int millis, int primaryColor, int secondaryColor) {
        float cx = getX() + 24.0f;
        float cy = getY() + 22.0f;
        float r = 15.0f;

        // Dial Background & Rim
        int dialBg = ThemeManager.isDark() ? 0x20FFFFFF : 0x12000000;
        GlassRenderUtil.fillRoundedRect(graphics, cx - r, cy - r, r * 2.0f, r * 2.0f, r, dialBg);
        int dialRim = ThemeManager.isDark() ? 0x30FFFFFF : 0x28334155;
        GlassRenderUtil.drawRoundedOutline(graphics, (int)(cx - r), (int)(cy - r), (int)(r * 2), (int)(r * 2), (int)r, 0.65f, dialRim);

        // 12 Hour Ticks via matrix rotation
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0f);

        for (int i = 0; i < 12; i++) {
            graphics.pose().pushPose();
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(i * 30.0f));
            boolean cardinal = (i % 3 == 0);
            float tickLen = cardinal ? 3.0f : 1.8f;
            float tickW = cardinal ? 1.2f : 0.8f;
            int tickColor = cardinal ? primaryColor : secondaryColor;
            GlassRenderUtil.fillRoundedRect(graphics, -tickW / 2.0f, -r + 1.6f, tickW, tickLen, 0.4f, tickColor);
            graphics.pose().popPose();
        }

        // Hour Hand
        float hourAngle = (hour % 12 + min / 60.0f + sec / 3600.0f) * 30.0f;
        graphics.pose().pushPose();
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(hourAngle));
        GlassRenderUtil.fillRoundedRect(graphics, -1.0f, -8.0f, 2.0f, 9.5f, 1.0f, primaryColor);
        graphics.pose().popPose();

        // Minute Hand
        float minAngle = (min + sec / 60.0f) * 6.0f;
        graphics.pose().pushPose();
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(minAngle));
        GlassRenderUtil.fillRoundedRect(graphics, -0.7f, -12.0f, 1.4f, 14.0f, 0.7f, primaryColor);
        graphics.pose().popPose();

        // Second Hand (Apple Watch signature smooth sweeping orange hand)
        float secAngle = (sec + millis / 1000.0f) * 6.0f;
        graphics.pose().pushPose();
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(secAngle));
        int secondColor = 0xFFFF5533; // Vibrant Apple Orange
        GlassRenderUtil.fillRoundedRect(graphics, -0.5f, -13.5f, 1.0f, 16.5f, 0.5f, secondColor);
        graphics.pose().popPose();

        graphics.pose().popPose(); // Restore translate

        // Center Pivot Pin
        GlassRenderUtil.fillRoundedRect(graphics, cx - 1.5f, cy - 1.5f, 3.0f, 3.0f, 1.5f, 0xFFFF5533);

        // Date and Digital readout on the right
        String dateShort = zdt.format(DATE_FORMAT_SHORT);
        if (!dateShort.isEmpty()) {
            dateShort = Character.toUpperCase(dateShort.charAt(0)) + dateShort.substring(1);
        }
        MsdfRenderer.renderText(
                Fonts.medium(),
                dateShort,
                7.5f,
                secondaryColor,
                graphics.pose().last().pose(),
                getX() + 46,
                getY() + 10,
                0.0f
        );

        String timeStr = zdt.format(showSeconds ? TIME_FORMAT_SEC : TIME_FORMAT_SHORT);
        MsdfRenderer.renderText(
                Fonts.roundBold(),
                timeStr,
                12.5f,
                primaryColor,
                graphics.pose().last().pose(),
                getX() + 46,
                getY() + 21,
                0.0f
        );
    }

    private void renderCustomizationMenu(GuiGraphics graphics, int mouseX, int mouseY, int primaryColor, int secondaryColor) {
        float alphaFactor = Math.max(0.0f, Math.min(1.0f, (expandProgress - 0.05f) / 0.95f));

        // 1. Subtle horizontal glass divider
        int dividerColor = ThemeManager.lerpColor(0x18FFFFFF, 0x1A000000, ThemeManager.getTransitionFactor());
        GlassRenderUtil.fillRoundedRect(
                graphics,
                getX() + 10,
                getY() + 44,
                getWidth() - 20,
                1,
                0.5f,
                applyAlpha(dividerColor, alphaFactor)
        );

        // 2. Section Header: "TRYB ZEGARA"
        MsdfRenderer.renderText(
                Fonts.medium(),
                "TRYB ZEGARA",
                6.5f,
                applyAlpha(secondaryColor, alphaFactor * 0.8f),
                graphics.pose().last().pose(),
                getX() + 12,
                getY() + 50,
                0.0f
        );

        // 3. Segmented Pill Switch [ Cyfrowy | Analogowy ]
        float segX = getX() + 10;
        float segY = getY() + 60;
        float segW = getWidth() - 20;
        float segH = 20;

        int segBg = ThemeManager.lerpColor(0x1E000000, 0x0E000000, ThemeManager.getTransitionFactor());
        GlassRenderUtil.fillRoundedRect(graphics, segX, segY, segW, segH, 6, applyAlpha(segBg, alphaFactor));

        float btn1W = (segW - 4) / 2.0f;
        float btn1X = segX + 2;
        float btn2X = btn1X + btn1W + 2;
        float btnY = segY + 2;
        float btnH = segH - 4;

        boolean hoverDigital = mouseX >= btn1X && mouseX <= btn1X + btn1W && mouseY >= btnY && mouseY <= btnY + btnH;
        boolean hoverAnalog = mouseX >= btn2X && mouseX <= btn2X + btn1W && mouseY >= btnY && mouseY <= btnY + btnH;

        // Digital Pill
        boolean isDig = (clockMode == ClockMode.DIGITAL);
        int digBg = isDig
                ? ThemeManager.lerpColor(0x38FFFFFF, 0x40FFFFFF, ThemeManager.getTransitionFactor())
                : (hoverDigital ? 0x18FFFFFF : 0x00000000);
        if (digBg != 0) {
            GlassRenderUtil.fillRoundedRect(graphics, btn1X, btnY, btn1W, btnH, 5, applyAlpha(digBg, alphaFactor));
            if (isDig) {
                GlassRenderUtil.drawRoundedOutline(graphics, (int)btn1X, (int)btnY, (int)btn1W, (int)btnH, 5, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(1.0f), alphaFactor));
            }
        }
        int digTextColor = isDig ? primaryColor : secondaryColor;
        MsdfRenderer.renderCenteredText(
                Fonts.medium(),
                "Cyfrowy",
                7.5f,
                applyAlpha(digTextColor, alphaFactor),
                graphics.pose().last().pose(),
                btn1X + btn1W / 2.0f,
                btnY + (btnH - 7.5f * 0.72f) / 2.0f,
                0.0f
        );

        // Analog Pill
        boolean isAna = (clockMode == ClockMode.ANALOG);
        int anaBg = isAna
                ? ThemeManager.lerpColor(0x38FFFFFF, 0x40FFFFFF, ThemeManager.getTransitionFactor())
                : (hoverAnalog ? 0x18FFFFFF : 0x00000000);
        if (anaBg != 0) {
            GlassRenderUtil.fillRoundedRect(graphics, btn2X, btnY, btn1W, btnH, 5, applyAlpha(anaBg, alphaFactor));
            if (isAna) {
                GlassRenderUtil.drawRoundedOutline(graphics, (int)btn2X, (int)btnY, (int)btn1W, (int)btnH, 5, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(1.0f), alphaFactor));
            }
        }
        int anaTextColor = isAna ? primaryColor : secondaryColor;
        MsdfRenderer.renderCenteredText(
                Fonts.medium(),
                "Analogowy",
                7.5f,
                applyAlpha(anaTextColor, alphaFactor),
                graphics.pose().last().pose(),
                btn2X + btn1W / 2.0f,
                btnY + (btnH - 7.5f * 0.72f) / 2.0f,
                0.0f
        );

        // 4. Seconds Toggle Row
        float secY = getY() + 86;
        float secH = 20;
        boolean hoverSec = mouseX >= segX && mouseX <= segX + segW && mouseY >= secY && mouseY <= secY + secH;
        int rowBg = hoverSec ? 0x15FFFFFF : 0x0AFFFFFF;
        GlassRenderUtil.fillRoundedRect(graphics, segX, secY, segW, secH, 6, applyAlpha(rowBg, alphaFactor));

        MsdfRenderer.renderText(
                Fonts.medium(),
                "Sekundy",
                7.5f,
                applyAlpha(primaryColor, alphaFactor),
                graphics.pose().last().pose(),
                segX + 8,
                secY + (secH - 7.5f * 0.72f) / 2.0f,
                0.0f
        );

        // iOS Toggle Pill on the right
        float switchW = 28.0f;
        float switchH = 13.0f;
        float switchX = segX + segW - switchW - 6.0f;
        float switchY = secY + (secH - switchH) / 2.0f;

        int switchBg = showSeconds ? 0xFF22C55E : 0x40888888;
        GlassRenderUtil.fillRoundedRect(graphics, switchX, switchY, switchW, switchH, switchH / 2.0f, applyAlpha(switchBg, alphaFactor));

        float thumbSize = switchH - 2.0f;
        float thumbX = showSeconds ? (switchX + switchW - thumbSize - 1.0f) : (switchX + 1.0f);
        GlassRenderUtil.fillRoundedRect(graphics, thumbX, switchY + 1.0f, thumbSize, thumbSize, thumbSize / 2.0f, applyAlpha(0xFFFFFFFF, alphaFactor));
    }

    private static int applyAlpha(int color, float alphaFactor) {
        alphaFactor = Math.max(0.0f, Math.min(1.0f, alphaFactor));
        int a = (int) (((color >> 24) & 0xFF) * alphaFactor);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static File getConfigFile() {
        if (configFile == null) {
            File gameDir = Minecraft.getInstance().gameDirectory;
            if (gameDir == null) {
                gameDir = new File(".");
            }
            configFile = new File(gameDir, "config/cweldlc/clock.json");
        }
        return configFile;
    }

    private static void loadConfig() {
        try {
            File file = getConfigFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("mode")) {
                        clockMode = ClockMode.valueOf(json.get("mode").getAsString());
                    }
                    if (json.has("showSeconds")) {
                        showSeconds = json.get("showSeconds").getAsBoolean();
                    }
                }
            }
        } catch (Exception ignored) {
            clockMode = ClockMode.DIGITAL;
            showSeconds = false;
        }
    }

    private static void saveConfig() {
        try {
            File file = getConfigFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JsonObject json = new JsonObject();
            json.addProperty("mode", clockMode.name());
            json.addProperty("showSeconds", showSeconds);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json.toString());
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
