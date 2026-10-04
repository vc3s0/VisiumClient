package dev.cweldlc.client.mixin;

import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.gui.chat.CustomChatRenderer;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow private boolean newMessageSinceScroll;

    @Shadow public abstract boolean isChatHidden();
    @Shadow public abstract int getLinesPerPage();
    @Shadow public abstract int getWidth();
    @Shadow public abstract double getScale();
    @Shadow public abstract int getLineHeight();
    @Shadow abstract int getMessageEndIndexAt(double x, double y);
    @Shadow abstract int getMessageLineIndexAt(double x, double y);
    @Shadow abstract double screenToChatX(double x);
    @Shadow abstract double screenToChatY(double y);
    @Shadow abstract void drawTagIcon(GuiGraphics graphics, int x, int y, GuiMessageTag.Icon icon);
    @Shadow abstract int getTagIconLeft(GuiMessage.Line line);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int tickCount, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (this.isChatHidden()) return;

        int linesPerPage = this.getLinesPerPage();
        int totalTrimmed = this.trimmedMessages.size();
        if (totalTrimmed <= 0) return;

        ci.cancel();

        ProfilerFiller profiler = Profiler.get();
        profiler.push("chat");

        float scale = (float) this.getScale();
        int chatWidth = Mth.ceil((float) this.getWidth() / scale);
        int guiHeight = graphics.guiHeight();

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(4.0F, 0.0F, 0.0F);

        int baseY = Mth.floor((float) (guiHeight - 40) / scale);
        double chatX = this.screenToChatX((double) mouseX);
        double chatY = this.screenToChatY((double) mouseY);
        int hoveredEndIdx = this.getMessageEndIndexAt(chatX, chatY);
        int hoveredLineIdx = this.getMessageLineIndexAt(chatX, chatY);

        double chatOpacity = this.minecraft.options.chatOpacity().get() * 0.9D + 0.1D;
        double bgOpacity = this.minecraft.options.textBackgroundOpacity().get();
        double lineSpacing = this.minecraft.options.chatLineSpacing().get();
        int lineHeight = this.getLineHeight();
        int spacingOffset = (int) Math.round(-8.0D * (lineSpacing + 1.0D) + 4.0D * lineSpacing);

        int renderedCount = 0;
        for (int i = 0; i + this.chatScrollbarPos < this.trimmedMessages.size() && i < linesPerPage; ++i) {
            int lineIdx = i + this.chatScrollbarPos;
            GuiMessage.Line line = this.trimmedMessages.get(lineIdx);
            if (line == null) continue;

            int age = tickCount - line.addedTime();
            if (age >= 200 && !focused) continue;

            double timeFactor = focused ? 1.0D : calculateTimeFactor(age);
            int textAlpha = (int) (255.0D * timeFactor * chatOpacity);
            int bgAlpha = (int) (255.0D * timeFactor * bgOpacity);
            renderedCount++;

            if (textAlpha > 3) {
                int lineY = baseY - i * lineHeight;
                int textY = lineY + spacingOffset;

                // Smooth slide-in entrance animation for fresh messages
                float entrance = Math.min(1.0f, (float) age / 6.0f);
                float animOffset = (1.0f - entrance) * -12.0f;

                boolean isHovered = focused && (lineIdx == hoveredLineIdx || lineIdx == hoveredEndIdx);

                graphics.pose().pushPose();
                graphics.pose().translate(animOffset, 0.0f, 0.0f);

                // Render Apple LiquidGlass pill behind message line
                float textWidth = Fonts.regular().getWidth(line.content(), 8.0f);
                int pillWidth = (int) Math.max(textWidth + 8.0f, chatWidth + 4);
                CustomChatRenderer.renderGlassMessagePill(graphics, -3, lineY - lineHeight, pillWidth, lineHeight, (float) bgAlpha / 255.0F, isHovered);

                // Tag indicator bar and icon
                GuiMessageTag tag = line.tag();
                if (tag != null) {
                    int tagColor = (textAlpha << 24) | (tag.indicatorColor() & 0x00FFFFFF);
                    graphics.fill(-4, lineY - lineHeight, -2, lineY, tagColor);
                    if (lineIdx == hoveredEndIdx && tag.icon() != null) {
                        int iconLeft = this.getTagIconLeft(line);
                        this.drawTagIcon(graphics, iconLeft, textY + 9, tag.icon());
                    }
                }

                // Render text with full vector clarity using MSDF Font
                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 0.0F, 50.0F);
                MsdfRenderer.renderText(Fonts.regular(), line.content(), 8.0f, (textAlpha << 24) | 0x00FFFFFF, graphics.pose().last().pose(), 0.0f, textY + 1.0f, 0.0f);
                graphics.pose().popPose();

                graphics.pose().popPose();
            }
        }

        // Custom Glass Scrollbar when focused
        if (focused && totalTrimmed > linesPerPage) {
            float scrollPct = (float) this.chatScrollbarPos / (float) (totalTrimmed - linesPerPage);
            float visibleRatio = (float) linesPerPage / (float) totalTrimmed;
            int trackH = linesPerPage * lineHeight;
            int trackY = baseY - trackH;
            CustomChatRenderer.renderGlassScrollbar(graphics, chatWidth + 2, trackY, trackH, scrollPct, visibleRatio);
        }

        graphics.pose().popPose();
        profiler.pop();
    }

    private static double calculateTimeFactor(int age) {
        double d = (double) age / 200.0D;
        d = 1.0D - d;
        d *= 10.0D;
        d = Mth.clamp(d, 0.0D, 1.0D);
        return d * d;
    }
}
