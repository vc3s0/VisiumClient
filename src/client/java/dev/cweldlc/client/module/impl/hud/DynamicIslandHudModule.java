package dev.cweldlc.client.module.impl.hud;

import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.media.MediaManager;
import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.notification.NotificationManager;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.AnimatedGifRenderer;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DynamicIslandHudModule extends Module {

    private final BooleanSetting showClock         = addSetting(new BooleanSetting("Show Clock", "Displays time next to the island", true));
    private final BooleanSetting showPing          = addSetting(new BooleanSetting("Show Ping", "Displays latency next to the island", true));
    private final BooleanSetting showMusic         = addSetting(new BooleanSetting("Show Music", "Displays media player when music is active", true));
    private final BooleanSetting showNotifications = addSetting(new BooleanSetting("Show Alerts", "Displays notifications in island", true));

    private float currentW = 68.0f;
    private float currentH = 17.0f;
    private float extendAnim = 0.0f;
    private boolean isExtended = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DynamicIslandHudModule() {
        super("DynamicIsland", "Apple styled interactive Dynamic Island top pill", Category.HUD);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        MediaManager.getInstance().update(0.05f);
    }

    private static int applyAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (((color >> 24) & 0xFF) * alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float delta) {
        if (!isEnabled() || mc.options.hideGui) return;

        float dt = Math.min(delta * 0.05f, 0.1f);
        int screenW = mc.getWindow().getGuiScaledWidth();
        double mouseX = mc.mouseHandler.xpos() * ((double) screenW / mc.getWindow().getScreenWidth());
        double mouseY = mc.mouseHandler.ypos() * ((double) mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());

        boolean mouseFree = mc.screen != null;
        boolean hasMusic = showMusic.getValue() && MediaManager.getInstance().isPlaying();
        NotificationManager.Notification notif = showNotifications.getValue() ? NotificationManager.getCurrent() : null;

        float topY = 7.0f;
        float targetW = 68.0f;
        float targetH = 17.0f;

        if (notif != null) {
            String notifText = notif.title() + " " + notif.message();
            targetW = Math.max(84.0f, Fonts.medium().getWidth(notifText, 6.8f) + 26.0f);
            targetH = 17.0f;
            isExtended = false;
        } else if (hasMusic) {
            boolean isHovered = mouseFree && mouseX >= (screenW / 2.0f - currentW / 2.0f) && mouseX <= (screenW / 2.0f + currentW / 2.0f)
                    && mouseY >= topY && mouseY <= topY + currentH;

            if (isHovered && (mc.screen instanceof ChatScreen || mc.screen instanceof dev.cweldlc.client.gui.clickgui.ClickGuiScreen)) {
                isExtended = true;
            } else if (!mouseFree) {
                isExtended = false;
            }

            if (isExtended) {
                targetW = 174.0f;
                targetH = 64.0f;
            } else {
                String title = MediaManager.getInstance().getTitle();
                float titleW = Fonts.medium().getWidth(title, 6.8f);
                targetW = Math.max(76.0f, Math.min(136.0f, titleW + 28.0f));
                targetH = 17.0f;
            }
        } else {
            targetW = Fonts.medium().getWidth("Visium", 6.8f) + 24.0f;
            targetH = 17.0f;
            isExtended = false;
        }

        // Butter-smooth physics spring interpolation
        currentW += (targetW - currentW) * (1.0f - (float) Math.exp(-dt * 18.0f));
        currentH += (targetH - currentH) * (1.0f - (float) Math.exp(-dt * 18.0f));
        extendAnim += ((isExtended ? 1.0f : 0.0f) - extendAnim) * (1.0f - (float) Math.exp(-dt * 16.0f));

        boolean showDetachedGif = (notif == null && !hasMusic && extendAnim < 0.1f);
        float bubbleSize = currentH;
        float bubbleGap = 5.0f;
        float totalW = showDetachedGif ? (bubbleSize + bubbleGap + currentW) : currentW;
        float groupStartX = screenW / 2.0f - totalW / 2.0f;

        float bubbleX = groupStartX;
        float islandX = showDetachedGif ? (groupStartX + bubbleSize + bubbleGap) : groupStartX;
        float islandY = topY;
        float cornerRadius = Math.min(currentH / 2.0f, 14.0f);

        // 1. Outside Elements (Larger Clock on Left & Ping on Right)
        if (extendAnim < 0.35f) {
            float outerAlpha = Math.max(0.0f, 1.0f - extendAnim * 3.0f);
            int clockColor = applyAlpha(0xFFFFFFFF, outerAlpha);
            int pingColor = applyAlpha(0xFFE5E7EB, outerAlpha);

            // Larger Clock on the Left (positioned relative to leftmost element)
            if (showClock.getValue()) {
                String timeStr = LocalTime.now().format(TIME_FMT);
                float timeW = Fonts.medium().getWidth(timeStr, 8.5f);
                float timeY = islandY + (currentH - 8.5f * 0.72f) / 2.0f;
                float clockRefX = showDetachedGif ? bubbleX : islandX;
                MsdfRenderer.renderText(Fonts.medium(), timeStr, 8.5f, clockColor, graphics.pose().last().pose(), clockRefX - timeW - 9.0f, timeY, 0.0f);
            }

            // Ping on the Right
            if (showPing.getValue()) {
                int ping = 0;
                try {
                    if (mc.getConnection() != null && mc.player != null) {
                        var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                        if (entry != null) {
                            ping = entry.getLatency();
                        }
                    }
                } catch (Throwable ignored) {}

                String pingStr = ping + " ms";
                float pingTextX = islandX + currentW + 8.0f;
                float pingY = islandY + (currentH - 7.0f * 0.72f) / 2.0f;
                MsdfRenderer.renderText(Fonts.medium(), pingStr, 7.0f, pingColor, graphics.pose().last().pose(), pingTextX, pingY, 0.0f);

                // 4 Clean White Signal Strength Bars
                float barBaseX = pingTextX + Fonts.medium().getWidth(pingStr, 7.0f) + 4.5f;
                int[] thresholds = {300, 150, 80, 0};
                for (int b = 0; b < 4; b++) {
                    float bW = 1.6f;
                    float bH = 2.5f + b * 1.5f;
                    float bX = barBaseX + b * 2.6f;
                    float bY = islandY + (currentH - bH) / 2.0f;
                    boolean active = ping <= thresholds[b] || b == 0;
                    int bCol = active ? pingColor : applyAlpha(0x35FFFFFF, outerAlpha);
                    GlassRenderUtil.fillRoundedRect(graphics, bX, bY, bW, bH, 0.6f, bCol);
                }
            }
        }

        // 2. Pure OLED Pitch-Black Backing (Monochrome B&W, no borders)
        int islandBg = 0xF8000000;

        // Detached Satellite Bubble with Visium GIF (visually detached from the main island)
        if (showDetachedGif) {
            GlassRenderUtil.fillRoundedRect(graphics, bubbleX, islandY, bubbleSize, currentH, cornerRadius, islandBg);
            AnimatedGifRenderer.VISIUM_LOGO.render(graphics, bubbleX + 2.0f, islandY + 2.0f, bubbleSize - 4.0f, currentH - 4.0f, (currentH - 4.0f) / 2.0f, 0xFFFFFFFF);
        }

        // Main Island Backing
        GlassRenderUtil.fillRoundedRect(graphics, islandX, islandY, currentW, currentH, cornerRadius, islandBg);

        // 3. Render Island Internal State (Strictly Black & White)
        if (notif != null) {
            renderNotification(graphics, notif, islandX, islandY, currentW, currentH);
        } else if (hasMusic) {
            if (extendAnim > 0.35f) {
                renderExtendedMusic(graphics, islandX, islandY, currentW, currentH, mouseX, mouseY);
            } else {
                renderCollapsedMusic(graphics, islandX, islandY, currentW, currentH);
            }
        } else {
            renderDefault(graphics, islandX, islandY, currentW, currentH);
        }
    }

    private void renderDefault(GuiGraphics graphics, float x, float y, float w, float h) {
        float textW = Fonts.medium().getWidth("Visium", 6.8f);
        float dotSize = 3.5f;
        float totalContent = dotSize + 4.5f + textW;
        float startX = x + (w - totalContent) / 2.0f;

        // Subtle glowing white breathing dot
        float pulse = 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 350.0);
        int dotColor = applyAlpha(0xFFFFFFFF, pulse);
        GlassRenderUtil.fillRoundedRect(graphics, startX, y + (h - dotSize) / 2.0f, dotSize, dotSize, dotSize / 2.0f, dotColor);

        float textX = startX + dotSize + 4.5f;
        float textY = y + (h - 6.8f * 0.72f) / 2.0f;
        MsdfRenderer.renderText(Fonts.medium(), "Visium", 6.8f, 0xFFFFFFFF, graphics.pose().last().pose(), textX, textY, 0.0f);
    }

    private void renderNotification(GuiGraphics graphics, NotificationManager.Notification notif, float x, float y, float w, float h) {
        float dotSize = 4.0f;
        float dotX = x + 7.0f;
        float dotY = y + (h - dotSize) / 2.0f;

        // Pure white indicator dot
        GlassRenderUtil.fillRoundedRect(graphics, dotX, dotY, dotSize, dotSize, 2.0f, 0xFFFFFFFF);

        float textX = dotX + dotSize + 5.0f;
        float textY = y + (h - 6.8f * 0.72f) / 2.0f;
        MsdfRenderer.renderText(Fonts.medium(), notif.title(), 6.8f, 0xFFFFFFFF, graphics.pose().last().pose(), textX, textY, 0.0f);

        float titleW = Fonts.medium().getWidth(notif.title(), 6.8f);
        MsdfRenderer.renderText(Fonts.regular(), notif.message(), 6.2f, 0xFF9CA3AF, graphics.pose().last().pose(), textX + titleW + 4.5f, y + (h - 6.2f * 0.72f) / 2.0f, 0.0f);
    }

    private void renderCollapsedMusic(GuiGraphics graphics, float x, float y, float w, float h) {
        MediaManager media = MediaManager.getInstance();

        // Left: Album Art thumbnail (11x11) with smooth rounded corners
        float thumbSize = 11.0f;
        float thumbX = x + 4.5f;
        float thumbY = y + (h - thumbSize) / 2.0f;

        ResourceLocation cover = media.getAlbumArtLocation();
        if (cover != null) {
            GlassRenderUtil.drawRoundedTexture(graphics, cover, thumbX, thumbY, thumbSize, thumbSize, 2.5f);
        } else {
            GlassRenderUtil.fillRoundedRect(graphics, thumbX, thumbY, thumbSize, thumbSize, 2.5f, 0x25FFFFFF);
            MsdfRenderer.renderCenteredText(Fonts.medium(), "♫", 5.5f, 0xFFFFFFFF, graphics.pose().last().pose(), thumbX + thumbSize / 2.0f, thumbY + 1.8f, 0.0f);
        }

        // Center: Track Title
        float textX = thumbX + thumbSize + 4.5f;
        float textY = y + (h - 6.8f * 0.72f) / 2.0f;
        float maxW = w - (textX - x) - 7.0f;

        String title = media.getTitle();
        if (Fonts.medium().getWidth(title, 6.8f) > maxW) {
            title = title.substring(0, Math.min(title.length(), 16)) + "...";
        }
        MsdfRenderer.renderText(Fonts.medium(), title, 6.8f, 0xFFFFFFFF, graphics.pose().last().pose(), textX, textY, 0.0f);
    }

    private void renderExtendedMusic(GuiGraphics graphics, float x, float y, float w, float h, double mouseX, double mouseY) {
        MediaManager media = MediaManager.getInstance();

        // 1. Album Cover (28x28) with smooth rounded corners
        float coverSize = 28.0f;
        float coverX = x + 9.0f;
        float coverY = y + 8.0f;

        ResourceLocation cover = media.getAlbumArtLocation();
        if (cover != null) {
            GlassRenderUtil.drawRoundedTexture(graphics, cover, coverX, coverY, coverSize, coverSize, 5.0f);
        } else {
            GlassRenderUtil.fillRoundedRect(graphics, coverX, coverY, coverSize, coverSize, 5.0f, 0x25FFFFFF);
            MsdfRenderer.renderCenteredText(Fonts.medium(), "♫", 11.0f, 0xFFFFFFFF, graphics.pose().last().pose(), coverX + coverSize / 2.0f, coverY + 7.5f, 0.0f);
        }

        // 2. Track Title & Artist (Monochrome White & Gray)
        float titleX = coverX + coverSize + 8.0f;
        float maxTitleW = w - (titleX - x) - 10.0f;
        String title = media.getTitle();
        if (Fonts.medium().getWidth(title, 7.5f) > maxTitleW) {
            title = title.substring(0, Math.min(title.length(), 18)) + "...";
        }
        MsdfRenderer.renderText(Fonts.medium(), title, 7.5f, 0xFFFFFFFF, graphics.pose().last().pose(), titleX, y + 8.5f, 0.0f);

        String artist = media.getArtist();
        if (Fonts.regular().getWidth(artist, 6.2f) > maxTitleW) {
            artist = artist.substring(0, Math.min(artist.length(), 20)) + "...";
        }
        MsdfRenderer.renderText(Fonts.regular(), artist, 6.2f, 0xFF9CA3AF, graphics.pose().last().pose(), titleX, y + 18.5f, 0.0f);

        // 3. Timestamps & Progress Timeline Track
        float trackX = x + 10.0f;
        float trackY = y + 41.0f;
        float trackW = w - 20.0f;
        float trackH = 2.0f;

        // Time strings (Elapsed & Duration)
        MsdfRenderer.renderText(Fonts.regular(), media.getFormattedPosition(), 5.5f, 0xFF9CA3AF, graphics.pose().last().pose(), trackX, trackY - 8.0f, 0.0f);
        String durStr = media.getFormattedDuration();
        float durW = Fonts.regular().getWidth(durStr, 5.5f);
        MsdfRenderer.renderText(Fonts.regular(), durStr, 5.5f, 0xFF9CA3AF, graphics.pose().last().pose(), trackX + trackW - durW, trackY - 8.0f, 0.0f);

        GlassRenderUtil.fillRoundedRect(graphics, trackX, trackY, trackW, trackH, 1.0f, 0x25FFFFFF);
        float fillW = trackW * Math.min(1.0f, Math.max(0.0f, media.getProgress()));
        if (fillW > 1.0f) {
            GlassRenderUtil.fillRoundedRect(graphics, trackX, trackY, fillW, trackH, 1.0f, 0xFFFFFFFF);
        }

        // 4. Interactive Transport Buttons (Previous, Play/Pause, Next)
        float btnY = y + 48.0f;
        float centerX = x + w / 2.0f;

        renderButton(graphics, "⏮", centerX - 26.0f, btnY, 14.0f, mouseX, mouseY);
        renderPlayPauseButton(graphics, media.isPlaying(), centerX, btnY, 15.0f, mouseX, mouseY);
        renderButton(graphics, "⏭", centerX + 26.0f, btnY, 14.0f, mouseX, mouseY);
    }

    private void renderPlayPauseButton(GuiGraphics graphics, boolean playing, float cx, float cy, float size, double mouseX, double mouseY) {
        float rx = cx - size / 2.0f;
        float ry = cy - 1.0f;
        boolean hovered = mouseX >= rx && mouseX <= rx + size && mouseY >= ry && mouseY <= ry + size;

        int bgCol = hovered ? 0x40FFFFFF : 0x22FFFFFF;
        GlassRenderUtil.fillRoundedRect(graphics, rx, ry, size, size, size / 2.0f, bgCol);

        String icon = playing ? "⏸" : "▶";
        int col = hovered ? 0xFFFFFFFF : 0xFFE5E7EB;
        MsdfRenderer.renderCenteredText(Fonts.medium(), icon, 8.0f, col, graphics.pose().last().pose(), cx, ry + 2.5f, 0.0f);
    }

    private void renderButton(GuiGraphics graphics, String icon, float cx, float cy, float size, double mouseX, double mouseY) {
        float rx = cx - size / 2.0f;
        float ry = cy - 1.0f;
        boolean hovered = mouseX >= rx && mouseX <= rx + size && mouseY >= ry && mouseY <= ry + size;

        if (hovered) {
            GlassRenderUtil.fillRoundedRect(graphics, rx, ry, size, size, 3.5f, 0x22FFFFFF);
        }
        int col = hovered ? 0xFFFFFFFF : 0xFF9CA3AF;
        MsdfRenderer.renderCenteredText(Fonts.medium(), icon, 7.5f, col, graphics.pose().last().pose(), cx, ry + 2.0f, 0.0f);
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled()) return false;

        float islandX = mc.getWindow().getGuiScaledWidth() / 2.0f - currentW / 2.0f;
        float islandY = 7.0f;

        if (mouseX >= islandX && mouseX <= islandX + currentW && mouseY >= islandY && mouseY <= islandY + currentH) {
            if (isExtended) {
                float centerX = islandX + currentW / 2.0f;
                float btnY = islandY + 48.0f;

                // Previous
                if (Math.abs(mouseX - (centerX - 26.0f)) <= 8 && Math.abs(mouseY - (btnY + 6.0f)) <= 8) {
                    MediaManager.getInstance().previous();
                    dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TOGGLE, 1.2f, 0.7f);
                    return true;
                }
                // Play/Pause
                if (Math.abs(mouseX - centerX) <= 9 && Math.abs(mouseY - (btnY + 6.5f)) <= 9) {
                    MediaManager.getInstance().playPause();
                    dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TOGGLE, 1.0f, 0.7f);
                    return true;
                }
                // Next
                if (Math.abs(mouseX - (centerX + 26.0f)) <= 8 && Math.abs(mouseY - (btnY + 6.0f)) <= 8) {
                    MediaManager.getInstance().next();
                    dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TOGGLE, 1.2f, 0.7f);
                    return true;
                }
                // Seek timeline
                float trackX = islandX + 10.0f;
                float trackW = currentW - 20.0f;
                if (mouseX >= trackX && mouseX <= trackX + trackW && mouseY >= islandY + 36.0f && mouseY <= islandY + 46.0f) {
                    float ratio = (float) (mouseX - trackX) / trackW;
                    MediaManager.getInstance().seekTo(ratio);
                    return true;
                }
            } else {
                isExtended = true;
                dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.CLICKGUI_OPEN, 1.15f, 0.7f);
                return true;
            }
        }
        return false;
    }
}
