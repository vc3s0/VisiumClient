package dev.cweldlc.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import dev.cweldlc.CwelDLC;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class ExpandableThemeWidget extends AbstractWidget {

    private static final ResourceLocation ICON_DARK = ResourceLocation.fromNamespaceAndPath(
            CwelDLC.MOD_ID, "textures/gui/icons/theme_dark.png"
    );
    private static final ResourceLocation ICON_LIGHT = ResourceLocation.fromNamespaceAndPath(
            CwelDLC.MOD_ID, "textures/gui/icons/theme_light.png"
    );

    private static final float COLLAPSED_WIDTH = 32.0f;
    private static final float EXPANDED_WIDTH = 96.0f;
    private static final float HEIGHT = 30.0f;

    private float expandProgress = 0.0f;
    private float hoverProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();

    public ExpandableThemeWidget(int x, int y) {
        super(x, y, (int) COLLAPSED_WIDTH, (int) HEIGHT, Component.literal("Theme Switcher"));
    }

    public float getCurrentWidth() {
        float t = Math.max(0.0f, Math.min(1.0f, expandProgress));
        float factor = 1.0f - (1.0f - t) * (1.0f - t);
        return COLLAPSED_WIDTH + (EXPANDED_WIDTH - COLLAPSED_WIDTH) * factor;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        float w = getCurrentWidth();
        return this.visible
                && mouseX >= (double) getX()
                && mouseX < (double) (getX() + w)
                && mouseY >= (double) getY()
                && mouseY < (double) (getY() + getHeight());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        float cx = getX() + getCurrentWidth() / 2.0f;
        float cy = getY() + getHeight() / 2.0f;
        ThemeManager.startRipple(cx, cy);
        ThemeManager.toggleTheme();

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastTime) / 1000.0f);
        lastTime = now;

        boolean hovered = isMouseOver(mouseX, mouseY);
        float targetExpand = hovered ? 1.0f : 0.0f;

        // Apple spring damping
        expandProgress += (targetExpand - expandProgress) * Math.min(1.0f, dt * 14.0f);
        hoverProgress += ((hovered ? 1.0f : 0.0f) - hoverProgress) * Math.min(1.0f, dt * 12.0f);

        float curW = getCurrentWidth();
        this.width = (int) Math.ceil(curW);

        // 1. Dynamic Island Glass Capsule
        GlassRenderUtil.drawGlassPanel(
                graphics,
                getX(),
                getY(),
                (int) curW,
                getHeight(),
                15,
                hovered,
                hoverProgress
        );

        // 2. Render SVG Sun-Moon Icon with smooth rotation
        float iconSize = 16.0f;
        int half = Math.round(iconSize / 2.0f);
        float iconCenterX = getX() + 16.0f;
        float iconCenterY = getY() + getHeight() / 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        graphics.pose().pushPose();
        graphics.pose().translate(iconCenterX, iconCenterY, 0.0f);

        // 180-degree fluid rotation on theme change
        float rot = ThemeManager.getTransitionFactor() * 180.0f;
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(rot));

        // When in Dark theme (factor ~ 0.0), draw white icon. When in White theme (factor ~ 1.0), draw dark icon.
        ResourceLocation activeIcon = ThemeManager.isDark() ? ICON_LIGHT : ICON_DARK;

        try {
            Minecraft.getInstance().getTextureManager().getTexture(activeIcon).setFilter(true, false);
        } catch (Exception ignored) {
        }

        graphics.blit(
                RenderType::guiTextured,
                activeIcon,
                -half,
                -half,
                0.0f,
                0.0f,
                (int) iconSize,
                (int) iconSize,
                128,
                128,
                128,
                128
        );

        // Flush batch immediately while current translation & rotation matrix are bound
        graphics.flush();
        graphics.pose().popPose();

        // 3. Fading typography label when expanded
        if (expandProgress > 0.08f) {
            float textAlpha = Math.max(0.0f, Math.min(1.0f, (expandProgress - 0.08f) / 0.92f));
            int baseTextColor = ThemeManager.getPrimaryTextColor(hoverProgress);
            int a = (int) (((baseTextColor >> 24) & 0xFF) * textAlpha);
            int textColor = (a << 24) | (baseTextColor & 0x00FFFFFF);

            String text = ThemeManager.isDark() ? "Ciemny" : "Jasny";
            float textX = getX() + 30.0f;
            float textY = getY() + (getHeight() - 8.5f * 0.72f) / 2.0f;

            MsdfRenderer.renderText(
                    Fonts.medium(),
                    text,
                    8.5f,
                    textColor,
                    graphics.pose().last().pose(),
                    textX,
                    textY,
                    0.0f
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
