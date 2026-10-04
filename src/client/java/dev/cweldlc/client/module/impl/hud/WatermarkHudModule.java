package dev.cweldlc.client.module.impl.hud;

import dev.cweldlc.CwelDLC;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatermarkHudModule extends Module {

    private final BooleanSetting showVersion = addSetting(new BooleanSetting("Show Version", "Displays client version", true));
    private final BooleanSetting showUser    = addSetting(new BooleanSetting("Show User", "Displays username", true));
    private final BooleanSetting showFps     = addSetting(new BooleanSetting("Show FPS", "Displays real-time FPS counter", true));
    private final BooleanSetting showPing    = addSetting(new BooleanSetting("Show Ping", "Displays latency in ms", true));
    private final BooleanSetting showTime    = addSetting(new BooleanSetting("Show Time", "Displays local time", true));
    private final BooleanSetting showCoords  = addSetting(new BooleanSetting("Show Coords", "Displays player coordinates", false));
    private final BooleanSetting showBps     = addSetting(new BooleanSetting("Show BPS", "Displays movement speed", false));

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final float BAR_H        = 18.0f;
    private static final float BAR_RADIUS   = 5.0f;
    private static final float H_PAD        = 7.0f;
    private static final float ELEMENT_GAP  = 6.0f;
    private static final float DIVIDER_W    = 1.0f;
    private static final float DIVIDER_H    = 9.0f;
    private static final float ROW_GAP      = 3.0f;
    private static final int DIVIDER_COLOR  = 0xFF71717A; // Zinc-500 sleek gray divider

    private record HudItem(String text, boolean bold, int color) {}

    private float smoothWidth1 = 0.0f;
    private float smoothWidth2 = 0.0f;
    private long lastRenderTime = System.currentTimeMillis();

    public WatermarkHudModule() {
        super("Watermark", "Sleek LiquidGlass HUD watermark", Category.HUD);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float delta) {
        if (!isEnabled() || mc.options.hideGui) return;

        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastRenderTime) / 1000.0f);
        lastRenderTime = now;

        float fontSize = 8.0f;
        float startX = 8.0f;
        float startY = 8.0f;

        // Row 1: Brand | User | FPS | Ping | Time
        List<HudItem> row1 = new ArrayList<>();
        String brand = "VisiumClient" + (showVersion.getValue() ? " " + CwelDLC.CLIENT_VERSION : "");
        row1.add(new HudItem(brand, true, 0xFFFFFFFF));

        if (showUser.getValue()) {
            String username = (mc.player != null) ? mc.player.getName().getString() : (mc.getUser() != null ? mc.getUser().getName() : "User");
            if (!username.isEmpty()) {
                row1.add(new HudItem(username, false, 0xFFD1D5DB));
            }
        }

        if (showFps.getValue()) {
            row1.add(new HudItem(mc.getFps() + "fps", false, 0xFF9CA3AF));
        }

        if (showPing.getValue()) {
            int ping = 0;
            try {
                if (mc.getConnection() != null && mc.player != null) {
                    var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                    if (entry != null) ping = entry.getLatency();
                }
            } catch (Throwable ignored) {}
            row1.add(new HudItem(ping + "ms", false, 0xFF9CA3AF));
        }

        if (showTime.getValue()) {
            row1.add(new HudItem(LocalTime.now().format(TIME_FMT), false, 0xFF9CA3AF));
        }

        smoothWidth1 = renderRow(graphics, row1, startX, startY, fontSize, smoothWidth1, dt);

        // Row 2: Coords | BPS
        List<HudItem> row2 = new ArrayList<>();
        if (showCoords.getValue() && mc.player != null) {
            row2.add(new HudItem("x" + mc.player.getBlockX() + " y" + mc.player.getBlockY() + " z" + mc.player.getBlockZ(), false, 0xFF9CA3AF));
        }
        if (showBps.getValue() && mc.player != null) {
            double dx = mc.player.getX() - mc.player.xOld;
            double dz = mc.player.getZ() - mc.player.zOld;
            double bps = Math.hypot(dx, dz) * 20.0;
            row2.add(new HudItem(String.format(java.util.Locale.US, "%.1fbps", bps), false, 0xFF9CA3AF));
        }

        if (!row2.isEmpty()) {
            smoothWidth2 = renderRow(graphics, row2, startX, startY + BAR_H + ROW_GAP, fontSize, smoothWidth2, dt);
        }
    }

    private float renderRow(GuiGraphics graphics, List<HudItem> items, float x, float y, float fontSize, float currentSmoothWidth, float dt) {
        if (items.isEmpty()) return currentSmoothWidth;

        float dividerSpacing = ELEMENT_GAP * 2.0f + DIVIDER_W;
        float contentW = 0.0f;
        for (int i = 0; i < items.size(); i++) {
            HudItem item = items.get(i);
            var font = item.bold() ? Fonts.medium() : Fonts.regular();
            float itemW = font.getWidth(item.text(), fontSize);
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
        float dividerY = y + (BAR_H - DIVIDER_H) / 2.0f;

        for (int i = 0; i < items.size(); i++) {
            HudItem item = items.get(i);
            if (i > 0) {
                curX += ELEMENT_GAP;
                GlassRenderUtil.fillRoundedRect(graphics, curX, dividerY, DIVIDER_W, DIVIDER_H, 0.5f, DIVIDER_COLOR);
                curX += DIVIDER_W + ELEMENT_GAP;
            }

            var font = item.bold() ? Fonts.medium() : Fonts.regular();
            MsdfRenderer.renderText(font, item.text(), fontSize, item.color(), graphics.pose().last().pose(), curX, textY, 0.0f);
            curX += font.getWidth(item.text(), fontSize);
        }

        graphics.disableScissor();
        return currentSmoothWidth;
    }
}
