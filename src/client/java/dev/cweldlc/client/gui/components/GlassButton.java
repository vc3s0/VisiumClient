package dev.cweldlc.client.gui.components;

import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class GlassButton extends AbstractButton {

    private final Consumer<GlassButton> onPressAction;
    private float hoverProgress = 0.0f;
    private long lastTime = System.currentTimeMillis();
    private int cornerRadius = 10;
    private int iconAccentColor = 0;

    public GlassButton(int x, int y, int width, int height, Component message, Consumer<GlassButton> onPress) {
        super(x, y, width, height, message);
        this.onPressAction = onPress;
    }

    public GlassButton withAccent(int accentColor) {
        this.iconAccentColor = accentColor;
        return this;
    }

    public GlassButton withRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }

    private float clickBounce = 0.0f;

    @Override
    public void onPress() {
        this.clickBounce = 1.0f;
        if (this.onPressAction != null) {
            this.onPressAction.accept(this);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastTime) / 1000.0f);
        lastTime = now;

        boolean hovered = this.isHoveredOrFocused();
        float target = hovered ? 1.0f : 0.0f;
        this.hoverProgress += (target - this.hoverProgress) * Math.min(1.0f, dt * 14.0f);
        this.clickBounce = Math.max(0.0f, this.clickBounce - dt * 5.5f);

        float cx = getX() + getWidth() / 2.0f;
        float cy = getY() + getHeight() / 2.0f;
        float scale = (1.0f + hoverProgress * 0.02f) - (float) Math.sin(clickBounce * Math.PI) * 0.06f;

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-cx, -cy, 0.0f);

        // Apple OLED Black Glass Panel
        GlassRenderUtil.drawGlassPanel(graphics, getX(), getY(), getWidth(), getHeight(), cornerRadius, hovered, hoverProgress);

        // Left accent indicator (Apple stealth pill)
        if (iconAccentColor != 0) {
            int dotAlpha = (int) (60 + 195 * hoverProgress);
            int dotColor = (dotAlpha << 24) | (iconAccentColor & 0x00FFFFFF);
            GlassRenderUtil.fillRoundedRect(graphics, getX() + 10, getY() + (getHeight() - 10) / 2, 3, 10, 1, dotColor);
        }

        // Text rendered via MSDF vector font (dynamic theme-aware typography)
        String text = getMessage().getString();
        int textColor = dev.cweldlc.client.theme.ThemeManager.getPrimaryTextColor(hoverProgress);
        float fontSize = 9.5f;
        float textY = getY() + (getHeight() - fontSize * 0.7f) / 2.0f;

        MsdfRenderer.renderCenteredText(
                Fonts.medium(),
                text,
                fontSize,
                textColor,
                graphics.pose().last().pose(),
                getX() + getWidth() / 2.0f,
                textY,
                0.0f
        );

        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
