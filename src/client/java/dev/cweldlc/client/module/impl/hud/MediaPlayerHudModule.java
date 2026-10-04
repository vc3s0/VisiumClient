package dev.cweldlc.client.module.impl.hud;

import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.media.MediaManager;
import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class MediaPlayerHudModule extends Module {

    private final BooleanSetting showSpectrum = addSetting(new BooleanSetting("Visualizer", "Shows animated audio spectrum bars", true));
    private final BooleanSetting showProgress = addSetting(new BooleanSetting("Progress Bar", "Shows track progress timeline", true));
    private float smoothHudProgress = 0.0f;

    public MediaPlayerHudModule() {
        super("MediaPlayer", "Sleek Apple Music floating HUD player", Category.HUD);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        MediaManager.getInstance().update(0.05f);
    }

    private float animProgress = 0.0f;
    private long lastRenderTime = System.currentTimeMillis();

    @Override
    public void onRender2D(GuiGraphics graphics, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastRenderTime) / 1000.0f);
        lastRenderTime = now;

        MediaManager media = MediaManager.getInstance();
        boolean active = isEnabled() && (mc.options == null || !mc.options.hideGui) && media.isPlaying();
        animProgress += ((active ? 1.0f : 0.0f) - animProgress) * (1.0f - (float) Math.exp(-dt * 16.0f));
        if (animProgress <= 0.005f) return;

        float slideY = (1.0f - animProgress) * 20.0f;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, slideY, 0.0f);

        float targetProg = media.getProgress();
        float speed = Math.abs(targetProg - smoothHudProgress) > 0.08f ? 24.0f : 12.0f;
        smoothHudProgress += (targetProg - smoothHudProgress) * (1.0f - (float) Math.exp(-0.05f * speed));

        String title = media.getTitle();
        String artist = media.getArtist();

        float pillW = 180.0f;
        float pillH = 38.0f;
        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();
        float pillX = screenW - pillW - 14.0f;
        float pillY = screenH - pillH - 14.0f;

        // Apple Liquid Glass card
        GlassRenderUtil.drawGlassPanel(graphics, (int) pillX, (int) pillY, (int) pillW, (int) pillH, 10, false, 0.0f);

        // Left: Album Art Cover (24x24) or Vinyl Pill
        float badgeSize = 24.0f;
        float badgeX = pillX + 7.0f;
        float badgeY = pillY + (pillH - badgeSize) / 2.0f;

        ResourceLocation cover = media.getAlbumArtLocation();
        if (cover != null) {
            GlassRenderUtil.fillRoundedRect(graphics, badgeX, badgeY, badgeSize, badgeSize, 5.0f, 0x20000000);
            try {
                Minecraft.getInstance().getTextureManager().getTexture(cover).setFilter(true, false);
            } catch (Exception ignored) {}
            graphics.blit(
                    RenderType::guiTextured,
                    cover,
                    (int) badgeX,
                    (int) badgeY,
                    0.0f,
                    0.0f,
                    (int) badgeSize,
                    (int) badgeSize,
                    (int) badgeSize,
                    (int) badgeSize,
                    (int) badgeSize,
                    (int) badgeSize
            );
            GlassRenderUtil.drawRoundedOutline(graphics, (int) badgeX, (int) badgeY, (int) badgeSize, (int) badgeSize, 5, 0.5f, ThemeManager.getGlassBorderColor(0.4f));
        } else {
            int badgeBg = ThemeManager.lerpColor(0x25FFFFFF, 0x1E000000, ThemeManager.getTransitionFactor());
            GlassRenderUtil.fillRoundedRect(graphics, badgeX, badgeY, badgeSize, badgeSize, 6.0f, badgeBg);
            GlassRenderUtil.drawRoundedOutline(graphics, (int) badgeX, (int) badgeY, (int) badgeSize, (int) badgeSize, 6, 0.5f, ThemeManager.getGlassBorderColor(0.4f));

            int iconColor = media.isPlaying() ? 0xFF3B82F6 : ThemeManager.getTitleColor();
            MsdfRenderer.renderCenteredText(
                    Fonts.medium(),
                    "♫",
                    10.0f,
                    iconColor,
                    graphics.pose().last().pose(),
                    badgeX + badgeSize / 2.0f,
                    badgeY + (badgeSize - 10.0f * 0.72f) / 2.0f,
                    0.0f
            );
        }

        // Title and Artist
        float textX = badgeX + badgeSize + 8.0f;
        float maxTitleW = pillW - (textX - pillX) - 34.0f;
        String displayTitle = Fonts.medium().getWidth(title, 7.5f) > maxTitleW ? title.substring(0, Math.min(title.length(), 16)) + "..." : title;

        MsdfRenderer.renderText(
                Fonts.medium(),
                displayTitle,
                7.5f,
                ThemeManager.getTitleColor(),
                graphics.pose().last().pose(),
                textX,
                pillY + 7.5f,
                0.0f
        );

        String subline = !media.getAlbum().isEmpty() ? artist + " • " + media.getAlbum() : artist + " • " + media.getPlayerName();
        String displayArtist = Fonts.regular().getWidth(subline, 6.2f) > maxTitleW ? artist.substring(0, Math.min(artist.length(), 14)) + " • " + media.getPlayerName() : subline;
        MsdfRenderer.renderText(
                Fonts.regular(),
                displayArtist,
                6.2f,
                ThemeManager.getSecondaryTextColor(0.0f),
                graphics.pose().last().pose(),
                textX,
                pillY + 19.0f,
                0.0f
        );

        // Animated Audio Spectrum Bars
        if (showSpectrum.getValue()) {
            float[] bars = media.getSpectrumBars();
            float barW = 2.0f;
            float barSpacing = 1.5f;
            float totalBarsW = bars.length * barW + (bars.length - 1) * barSpacing;
            float specX = pillX + pillW - totalBarsW - 10.0f;
            float specCenterY = pillY + pillH / 2.0f - 1.0f;
            float maxBarH = 14.0f;

            for (int i = 0; i < bars.length; i++) {
                float bH = Math.max(2.5f, bars[i] * maxBarH);
                float bX = specX + i * (barW + barSpacing);
                float bY = specCenterY - bH / 2.0f;
                int barColor = ThemeManager.lerpColor(0xFF71717A, 0xFFFFFFFF, bars[i]);
                GlassRenderUtil.fillRoundedRect(graphics, bX, bY, barW, bH, 1.0f, barColor);
            }
        }

        // Sleek timeline progress track at bottom of the pill
        if (showProgress.getValue()) {
            float trackH = 1.5f;
            float trackY = pillY + pillH - 3.5f;
            float trackW = pillW - 14.0f;
            float trackX = pillX + 7.0f;
            GlassRenderUtil.fillRoundedRect(graphics, trackX, trackY, trackW, trackH, 0.75f, 0x22FFFFFF);

            float fillW = trackW * Math.max(0.0f, Math.min(1.0f, smoothHudProgress));
            if (fillW > 1.0f) {
                GlassRenderUtil.fillRoundedRect(graphics, trackX, trackY, fillW, trackH, 0.75f, 0xFFFFFFFF);
            }
        }

        graphics.pose().popPose();
    }
}
