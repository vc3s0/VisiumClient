package dev.cweldlc.client.module.impl.hud;

import dev.cweldlc.CwelDLC;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatermarkHudModule extends Module {

    private final BooleanSetting showIcons   = addSetting(new BooleanSetting("Show Icons", "Displays sleek icons alongside watermark stats", true));
    private final BooleanSetting showVersion = addSetting(new BooleanSetting("Show Version", "Displays client version", true));
    private final BooleanSetting showUser    = addSetting(new BooleanSetting("Show User", "Displays username", true));
    private final BooleanSetting showFps     = addSetting(new BooleanSetting("Show FPS", "Displays real-time FPS counter", true));
    private final BooleanSetting showPing    = addSetting(new BooleanSetting("Show Ping", "Displays latency in ms", true));
    private final BooleanSetting showTime    = addSetting(new BooleanSetting("Show Time", "Displays local time", true));
    private final BooleanSetting showCoords  = addSetting(new BooleanSetting("Show Coords", "Displays player coordinates", false));
    private final BooleanSetting showBps     = addSetting(new BooleanSetting("Show BPS", "Displays movement speed", false));

    private static final ResourceLocation ICON_BRAND     = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/sparkles.png");
    private static final ResourceLocation ICON_USER      = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/category/player.png");
    private static final ResourceLocation ICON_FPS       = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/gauge.png");
    private static final ResourceLocation ICON_WIFI_HIGH = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/wifi_high.png");
    private static final ResourceLocation ICON_WIFI_MID  = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/wifi.png");
    private static final ResourceLocation ICON_WIFI_LOW  = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/wifi_low.png");
    private static final ResourceLocation ICON_CLOCK     = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/clock.png");
    private static final ResourceLocation ICON_COORDS    = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/compass.png");
    private static final ResourceLocation ICON_BPS       = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/category/movement.png");

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final float BAR_H        = 18.0f;
    private static final float BAR_RADIUS   = 5.0f;
    private static final float H_PAD        = 7.0f;
    private static final float ELEMENT_GAP  = 6.0f;
    private static final float ICON_SIZE    = 9.5f;
    private static final float ICON_GAP     = 3.5f;
    private static final float DIVIDER_W    = 1.0f;
    private static final float DIVIDER_H    = 9.0f;
    private static final float ROW_GAP      = 3.0f;
    private static final int DIVIDER_COLOR  = 0xFF71717A; // Zinc-500 sleek gray divider

    private record HudItem(ResourceLocation icon, String text, boolean bold, int color, boolean isFps) {
        HudItem(ResourceLocation icon, String text, boolean bold, int color) {
            this(icon, text, bold, color, false);
        }
    }

    private float smoothWidth1 = 0.0f;
    private float smoothWidth2 = 0.0f;
    private long lastRenderTime = System.currentTimeMillis();

    private int currentFpsVal = -1;
    private int oldFpsVal = -1;
    private float fpsAnim = 1.0f;

    public WatermarkHudModule() {
        super("Watermark", "Sleek LiquidGlass HUD watermark", Category.HUD);
        setEnabled(true);
    }

    private float animProgress = 0.0f;
    private final List<HudItem> row1 = new ArrayList<>(8);
    private final List<HudItem> row2 = new ArrayList<>(4);

    @Override
    public void onRender2D(GuiGraphics graphics, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastRenderTime) / 1000.0f);
        lastRenderTime = now;

        boolean active = isEnabled() && (mc.options == null || !mc.options.hideGui);
        animProgress += ((active ? 1.0f : 0.0f) - animProgress) * (1.0f - (float) Math.exp(-dt * 16.0f));
        if (animProgress <= 0.005f) return;

        float slideY = (1.0f - animProgress) * -14.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, slideY, 0.0f);

        float fontSize = 8.0f;
        float startX = 8.0f;
        float startY = 8.0f;

        // Row 1: Brand | User | FPS | Ping | Time
        row1.clear();
        String brand = "VisiumClient" + (showVersion.getValue() ? " " + CwelDLC.CLIENT_VERSION : "");
        row1.add(new HudItem(ICON_BRAND, brand, true, 0xFFFFFFFF));

        if (showUser.getValue()) {
            String username = (mc.player != null) ? mc.player.getName().getString() : (mc.getUser() != null ? mc.getUser().getName() : "User");
            if (!username.isEmpty()) {
                row1.add(new HudItem(ICON_USER, username, false, 0xFFD1D5DB));
            }
        }

        if (showFps.getValue()) {
            int targetFps = mc.getFps();
            if (currentFpsVal == -1) {
                currentFpsVal = targetFps;
                oldFpsVal = targetFps;
                fpsAnim = 1.0f;
            } else if (targetFps != currentFpsVal) {
                oldFpsVal = currentFpsVal;
                currentFpsVal = targetFps;
                fpsAnim = 0.0f;
            }
            if (fpsAnim < 1.0f) {
                fpsAnim = Math.min(1.0f, fpsAnim + dt * 4.5f);
            }
            row1.add(new HudItem(ICON_FPS, currentFpsVal + "fps", false, 0xFF9CA3AF, true));
        }

        if (showPing.getValue()) {
            int ping = 0;
            try {
                if (mc.getConnection() != null && mc.player != null) {
                    var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                    if (entry != null) ping = entry.getLatency();
                }
            } catch (Throwable ignored) {}

            ResourceLocation pingIcon = ICON_WIFI_HIGH;
            if (ping > 120) {
                pingIcon = ICON_WIFI_LOW;
            } else if (ping > 60) {
                pingIcon = ICON_WIFI_MID;
            }
            row1.add(new HudItem(pingIcon, ping + "ms", false, 0xFF9CA3AF));
        }

        if (showTime.getValue()) {
            row1.add(new HudItem(ICON_CLOCK, LocalTime.now().format(TIME_FMT), false, 0xFF9CA3AF));
        }

        smoothWidth1 = renderRow(graphics, row1, startX, startY, fontSize, smoothWidth1, dt);

        // Row 2: Coords | BPS
        row2.clear();
        if (showCoords.getValue() && mc.player != null) {
            row2.add(new HudItem(ICON_COORDS, "x" + mc.player.getBlockX() + " y" + mc.player.getBlockY() + " z" + mc.player.getBlockZ(), false, 0xFF9CA3AF));
        }
        if (showBps.getValue() && mc.player != null) {
            double dx = mc.player.getX() - mc.player.xOld;
            double dz = mc.player.getZ() - mc.player.zOld;
            double bps = Math.hypot(dx, dz) * 20.0;
            row2.add(new HudItem(ICON_BPS, String.format(java.util.Locale.US, "%.1fbps", bps), false, 0xFF9CA3AF));
        }

        if (!row2.isEmpty()) {
            smoothWidth2 = renderRow(graphics, row2, startX, startY + BAR_H + ROW_GAP, fontSize, smoothWidth2, dt);
        }

        graphics.pose().popPose();
    }

    private float renderRow(GuiGraphics graphics, List<HudItem> items, float x, float y, float fontSize, float currentSmoothWidth, float dt) {
        if (items.isEmpty()) return currentSmoothWidth;

        boolean iconsEnabled = showIcons.getValue();
        float dividerSpacing = ELEMENT_GAP * 2.0f + DIVIDER_W;
        float contentW = 0.0f;
        for (int i = 0; i < items.size(); i++) {
            HudItem item = items.get(i);
            var font = item.bold() ? Fonts.medium() : Fonts.regular();
            float textW;
            if (item.isFps() && fpsAnim < 1.0f && oldFpsVal != -1) {
                float oldW = font.getWidth(oldFpsVal + "fps", fontSize);
                float newW = font.getWidth(currentFpsVal + "fps", fontSize);
                float ease = (float) (0.5 - 0.5 * Math.cos(fpsAnim * Math.PI));
                textW = oldW + (newW - oldW) * ease;
            } else {
                textW = font.getWidth(item.text(), fontSize);
            }
            float itemW = (iconsEnabled && item.icon() != null ? ICON_SIZE + ICON_GAP : 0.0f) + textW;
            contentW += (i == 0 ? 0.0f : dividerSpacing) + itemW;
        }

        float targetPillW = contentW + H_PAD * 2.0f;
        if (currentSmoothWidth <= 0.1f) {
            currentSmoothWidth = targetPillW;
        } else {
            currentSmoothWidth += (targetPillW - currentSmoothWidth) * (1.0f - (float) Math.exp(-dt * 18.0f));
        }

        int bg = ThemeManager.getGlassBaseColor(0.0f);
        GlassRenderUtil.fillRoundedRect(graphics, x, y, currentSmoothWidth, BAR_H, BAR_RADIUS, bg);

        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + currentSmoothWidth), (int) Math.ceil(y + BAR_H));

        float curX = x + H_PAD;
        float textY = y + (BAR_H - fontSize * 0.72f) / 2.0f;
        float iconY = y + (BAR_H - ICON_SIZE) / 2.0f;
        float dividerY = y + (BAR_H - DIVIDER_H) / 2.0f;

        for (int i = 0; i < items.size(); i++) {
            HudItem item = items.get(i);
            if (i > 0) {
                curX += ELEMENT_GAP;
                GlassRenderUtil.fillRoundedRect(graphics, curX, dividerY, DIVIDER_W, DIVIDER_H, 0.5f, DIVIDER_COLOR);
                curX += DIVIDER_W + ELEMENT_GAP;
            }

            if (iconsEnabled && item.icon() != null) {
                drawIcon(graphics, item.icon(), curX, iconY, ICON_SIZE, item.color());
                curX += ICON_SIZE + ICON_GAP;
            }

            var font = item.bold() ? Fonts.medium() : Fonts.regular();

            if (item.isFps() && fpsAnim < 1.0f && oldFpsVal != -1) {
                float ease = (float) (0.5 - 0.5 * Math.cos(fpsAnim * Math.PI));

                // Old FPS rolls up and fades out
                float oldY = textY - ease * 6.5f;
                int oldAlpha = (int) ((1.0f - ease) * 255.0f);
                if (oldAlpha > 2) {
                    int oldColor = (oldAlpha << 24) | (item.color() & 0x00FFFFFF);
                    MsdfRenderer.renderText(font, oldFpsVal + "fps", fontSize, oldColor, graphics.pose().last().pose(), curX, oldY, 0.0f);
                }

                // New FPS rolls in from below with subtle brightness pulse
                float newY = textY + (1.0f - ease) * 6.5f;
                int newAlpha = (int) (ease * 255.0f);
                if (newAlpha > 2) {
                    int pulseR = (int) (156 + (255 - 156) * (1.0f - ease));
                    int pulseG = (int) (163 + (255 - 163) * (1.0f - ease));
                    int pulseB = (int) (175 + (255 - 175) * (1.0f - ease));
                    int newColor = (newAlpha << 24) | (pulseR << 16) | (pulseG << 8) | pulseB;
                    MsdfRenderer.renderText(font, currentFpsVal + "fps", fontSize, newColor, graphics.pose().last().pose(), curX, newY, 0.0f);
                }

                float oldW = font.getWidth(oldFpsVal + "fps", fontSize);
                float newW = font.getWidth(currentFpsVal + "fps", fontSize);
                curX += oldW + (newW - oldW) * ease;
            } else {
                MsdfRenderer.renderText(font, item.text(), fontSize, item.color(), graphics.pose().last().pose(), curX, textY, 0.0f);
                curX += font.getWidth(item.text(), fontSize);
            }
        }

        graphics.disableScissor();
        return currentSmoothWidth;
    }

    private void drawIcon(GuiGraphics graphics, ResourceLocation loc, float x, float y, float size, int color) {
        if (loc == null || size <= 0) return;
        GlassRenderUtil.drawTexture(graphics, loc, x, y, size, size, color);
    }
}
