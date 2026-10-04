package dev.cweldlc.client.gui.chat;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.Suggestion;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.mixin.EditBoxAccessor;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;

public final class CustomChatRenderer {

    private CustomChatRenderer() {}

    private static float smoothInputFocus = 0.0f;
    private static float smoothSuggestionY = -1.0f;
    private static float openAnimProgress = 0.0f;
    private static long lastChatTime = System.currentTimeMillis();

    public static void resetOpenAnimation() {
        openAnimProgress = 0.0f;
        smoothInputFocus = 1.0f;
        lastChatTime = System.currentTimeMillis();
    }

    public static float getOpenAnimProgress() {
        return openAnimProgress;
    }

    // ==========================================
    // 1. SOLID CHAT INPUT BAR (ROCKREADY STYLE)
    // ==========================================

    public static void renderChatInputBar(GuiGraphics graphics, EditBox input, int screenWidth, int screenHeight, int mouseX, int mouseY, Font font) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastChatTime) / 1000.0f);
        lastChatTime = now;

        openAnimProgress += (1.0f - openAnimProgress) * (1.0f - (float) Math.exp(-dt * 22.0f));
        if (openAnimProgress > 0.999f) openAnimProgress = 1.0f;

        int barX = 4;
        int barY = screenHeight - 20;
        int barW = screenWidth - 8;
        int barH = 16;
        float barR = 6.0f;

        float slideY = (1.0f - openAnimProgress) * 26.0f;
        float scale = 0.94f + 0.06f * openAnimProgress;

        graphics.pose().pushPose();
        float centerX = barX + barW / 2.0f;
        float centerY = barY + barH / 2.0f;
        graphics.pose().translate(centerX, centerY + slideY, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-centerX, -centerY, 0.0f);

        boolean isHovered = mouseX >= barX && mouseX <= barX + barW && mouseY >= barY && mouseY <= barY + barH;
        boolean isFocused = input.isFocused();
        float targetHover = isFocused ? 1.0f : (isHovered ? 0.6f : 0.0f);
        smoothInputFocus += (targetHover - smoothInputFocus) * (1.0f - (float) Math.exp(-dt * 16.0f));

        // 1. Solid antialiased rounded background (0x0C0C0C Dark mode)
        int baseColor = ThemeManager.getGlassBaseColor(smoothInputFocus);
        GlassRenderUtil.fillRoundedRect(graphics, barX, barY, barW, barH, barR, baseColor);

        // Smooth outline glow when focused
        if (smoothInputFocus > 0.02f) {
            int glowAlpha = (int) (110 * smoothInputFocus);
            int glowColor = (glowAlpha << 24) | 0x00FFFFFF;
            GlassRenderUtil.drawRoundedOutline(graphics, barX, barY, barW, barH, barR, 0.5f, glowColor);
        }

        // 2. High-definition MSDF vector typography & scrolling
        String value = input.getValue();
        float textY = barY + (barH - 8.0f * 0.72f) / 2.0f;

        int cursorPos = Math.min(value.length(), Math.max(0, input.getCursorPosition()));
        int highlightPos = cursorPos;
        if (input instanceof EditBoxAccessor accessor) {
            highlightPos = Math.min(value.length(), Math.max(0, accessor.getHighlightPos()));
        }

        float visibleTextW = barW - 16.0f;
        float cursorOffset = Fonts.regular().getWidth(value.substring(0, cursorPos), 8.0f);
        float scrollX = 0.0f;
        if (cursorOffset > visibleTextW) {
            scrollX = cursorOffset - visibleTextW;
        }

        graphics.enableScissor(barX + 6, barY, barX + barW - 6, barY + barH);

        if (value.isEmpty()) {
            MsdfRenderer.renderText(Fonts.regular(), "Napisz wiadomość lub zacznij od / aby wpisać komendę...", 8.0f, 0xFF6B7280, graphics.pose().last().pose(), barX + 8.0f, textY, 0.0f);
        } else {
            // Text selection highlight
            int selStart = Math.min(cursorPos, highlightPos);
            int selEnd = Math.max(cursorPos, highlightPos);
            if (selStart != selEnd) {
                float selStartX = barX + 8.0f + Fonts.regular().getWidth(value.substring(0, selStart), 8.0f) - scrollX;
                float selEndX = barX + 8.0f + Fonts.regular().getWidth(value.substring(0, selEnd), 8.0f) - scrollX;
                GlassRenderUtil.fillRoundedRect(graphics, selStartX, barY + 2.5f, Math.max(2.0f, selEndX - selStartX), 11.0f, 2.0f, 0x45FFFFFF);
            }

            MsdfRenderer.renderText(Fonts.regular(), value, 8.0f, 0xFFFFFFFF, graphics.pose().last().pose(), barX + 8.0f - scrollX, textY, 0.0f);
        }

        // 3. Smooth antialiased blinking cursor (when not selecting a range)
        if (isFocused && cursorPos == highlightPos) {
            float blink = (float) Math.sin((System.currentTimeMillis() % 1000) / 1000.0 * Math.PI * 2.0);
            if (blink > -0.2f) {
                float cAlpha = Math.min(1.0f, (blink + 0.2f) / 0.8f);
                int cursorColor = ((int) (255 * cAlpha) << 24) | 0x00FFFFFF;
                float cursorX = barX + 8.0f + cursorOffset - scrollX;
                GlassRenderUtil.fillRoundedRect(graphics, cursorX, barY + 3.0f, 1.0f, 10.0f, 0.5f, cursorColor);
            }
        }

        graphics.disableScissor();
        graphics.pose().popPose();
    }

    // ==========================================
    // 2. SOLID COMMAND SUGGESTIONS POPUP
    // ==========================================

    public static void renderSuggestions(
            GuiGraphics graphics,
            Rect2i rect,
            List<Suggestion> suggestionList,
            int offset,
            int current,
            Font font,
            int mouseX,
            int mouseY,
            Consumer<Integer> selectAction
    ) {
        if (suggestionList == null || suggestionList.isEmpty()) return;

        int lineLimit = 10;
        int count = Math.min(suggestionList.size() - offset, lineLimit);
        if (count <= 0) return;

        int rowH = 13;
        int pad = 4;
        boolean hasScrollbar = suggestionList.size() > lineLimit;
        int scrollbarPad = hasScrollbar ? 7 : 0;

        // Calculate card width based on widest visible suggestion using MSDF font
        float maxTextWidth = 80.0f;
        for (int i = 0; i < count; i++) {
            Suggestion s = suggestionList.get(offset + i);
            maxTextWidth = Math.max(maxTextWidth, Fonts.medium().getWidth(s.getText(), 8.0f));
        }
        int cardW = (int) Math.max(rect.getWidth() + 20, maxTextWidth + 24 + scrollbarPad);
        cardW = Math.max(cardW, 140);
        int cardH = count * rowH + pad * 2;

        int cardX = Math.max(4, Math.min(rect.getX(), graphics.guiWidth() - cardW - 4));
        int cardY = rect.getY() - (cardH - rect.getHeight());
        if (cardY + cardH > graphics.guiHeight() - 22) {
            cardY = graphics.guiHeight() - 22 - cardH;
        }
        if (cardY < 4) cardY = 4;

        // 1. Solid rounded card background
        int cardBg = ThemeManager.getGlassBaseColor(0.2f);
        GlassRenderUtil.fillRoundedRect(graphics, cardX, cardY, cardW, cardH, 6.0f, cardBg);

        // Draw Sliding Selection Pill Indicator if a suggestion is active
        int visibleCurrent = current - offset;
        int rowW = cardW - pad * 2 - scrollbarPad;
        if (visibleCurrent >= 0 && visibleCurrent < count) {
            float targetPillY = cardY + pad + visibleCurrent * rowH;
            if (smoothSuggestionY < 0.0f || Math.abs(smoothSuggestionY - targetPillY) > cardH) {
                smoothSuggestionY = targetPillY;
            } else {
                smoothSuggestionY += (targetPillY - smoothSuggestionY) * (1.0f - (float) Math.exp(-0.05f * 20.0f));
            }
            GlassRenderUtil.fillRoundedRect(graphics, cardX + pad, smoothSuggestionY, rowW, rowH, 4.0f, 0x30FFFFFF);
            GlassRenderUtil.fillRoundedRect(graphics, cardX + pad + 2.0f, smoothSuggestionY + 2.5f, 2.0f, rowH - 5.0f, 1.0f, 0xFFFFFFFF);
        } else {
            smoothSuggestionY = -1.0f;
        }

        // 2. Suggestion Items with razor-sharp MSDF typography
        for (int i = 0; i < count; i++) {
            int index = offset + i;
            Suggestion s = suggestionList.get(index);
            int rowX = cardX + pad;
            int rowY = cardY + pad + i * rowH;

            // Mouse hover selection update
            boolean isHovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY < rowY + rowH;
            if (isHovered && selectAction != null) {
                selectAction.accept(index);
            }

            boolean isSelected = (index == current);
            float rowTextY = rowY + (rowH - 7.5f * 0.72f) / 2.0f;

            if (isSelected) {
                // Crisp vector white text
                MsdfRenderer.renderText(Fonts.medium(), s.getText(), 7.5f, 0xFFFFFFFF, graphics.pose().last().pose(), rowX + 8.0f, rowTextY, 0.0f);
            } else {
                if (isHovered) {
                    int hoverRowBg = ThemeManager.isWhite() ? 0x15000000 : 0x18FFFFFF;
                    GlassRenderUtil.fillRoundedRect(graphics, rowX, rowY, rowW, rowH, 4.0f, hoverRowBg);
                }
                int textColor = ThemeManager.getSecondaryTextColor(0.0f);
                MsdfRenderer.renderText(Fonts.regular(), s.getText(), 7.5f, textColor, graphics.pose().last().pose(), rowX + 8.0f, rowTextY, 0.0f);
            }
        }

        // 3. Scrollbar
        if (hasScrollbar) {
            float trackX = cardX + cardW - 5.0f;
            float trackY = cardY + pad;
            float trackH = cardH - pad * 2;
            float trackW = 2.5f;

            int trackBg = ThemeManager.isWhite() ? 0x25000000 : 0x20FFFFFF;
            GlassRenderUtil.fillRoundedRect(graphics, trackX, trackY, trackW, trackH, 1.25f, trackBg);

            float maxOffset = (float) (suggestionList.size() - count);
            float scrollPct = Math.max(0.0f, Math.min(1.0f, (float) offset / maxOffset));
            float thumbH = Math.max(12.0f, trackH * ((float) count / (float) suggestionList.size()));
            float thumbY = trackY + (trackH - thumbH) * scrollPct;

            GlassRenderUtil.fillRoundedRect(graphics, trackX, thumbY, trackW, thumbH, 1.25f, 0x80FFFFFF);
        }

        // 4. Tooltip Card for Selected Suggestion with MSDF font
        if (current >= 0 && current < suggestionList.size()) {
            Suggestion currentSuggestion = suggestionList.get(current);
            Message tooltipMsg = currentSuggestion.getTooltip();
            if (tooltipMsg != null) {
                Component comp = ComponentUtils.fromMessage(tooltipMsg);
                String tipStr = comp.getString();
                int tipW = (int) Fonts.regular().getWidth(tipStr, 7.0f) + 12;
                int tipH = 16;
                int tipX = cardX + cardW + 4;
                if (tipX + tipW > graphics.guiWidth() - 4) {
                    tipX = cardX - tipW - 4;
                }
                int tipY = cardY + pad + (current - offset) * rowH - 1;

                if (tipX >= 4 && tipX + tipW <= graphics.guiWidth()) {
                    int tipBg = ThemeManager.getGlassBaseColor(0.3f);
                    GlassRenderUtil.fillRoundedRect(graphics, tipX, tipY, tipW, tipH, 5.0f, tipBg);
                    float tipTextY = tipY + (tipH - 7.0f * 0.72f) / 2.0f;
                    MsdfRenderer.renderText(Fonts.regular(), tipStr, 7.0f, ThemeManager.getPrimaryTextColor(0.0f), graphics.pose().last().pose(), tipX + 6.0f, tipTextY, 0.0f);
                } else {
                    graphics.renderTooltip(font, comp, mouseX, mouseY);
                }
            }
        }
    }

    // ==========================================
    // 3. SOLID COMMAND USAGE BADGE
    // ==========================================

    public static void renderCommandUsage(
            GuiGraphics graphics,
            List<FormattedCharSequence> usage,
            int position,
            int usageWidth,
            boolean anchorToBottom,
            int screenHeight,
            Font font
    ) {
        if (usage == null || usage.isEmpty()) return;

        int padX = 6;
        int padY = 4;
        int lineH = 12;
        int badgeW = usageWidth + padX * 2;
        int badgeH = usage.size() * lineH + padY * 2 - 2;

        int badgeX = Math.max(4, position - 1);
        int badgeY = anchorToBottom ? (screenHeight - 24 - badgeH) : 72;

        // 1. Solid rounded badge backing
        int badgeBg = ThemeManager.getGlassBaseColor(0.1f);
        GlassRenderUtil.fillRoundedRect(graphics, badgeX, badgeY, badgeW, badgeH, 6.0f, badgeBg);

        // 2. High-res MSDF syntax lines
        for (int i = 0; i < usage.size(); i++) {
            FormattedCharSequence line = usage.get(i);
            MsdfRenderer.renderText(Fonts.regular(), line, 7.5f, ThemeManager.getPrimaryTextColor(0.0f), graphics.pose().last().pose(), badgeX + padX, badgeY + padY + i * lineH, 0.0f);
        }
    }

    // ==========================================
    // 4. SOLID CHAT HUD MESSAGE CARDS
    // ==========================================

    public static void renderGlassMessagePill(GuiGraphics graphics, int x, int y, int width, int height, float alpha, boolean hovered) {
        if (alpha <= 0.01f || width <= 0 || height <= 0) return;

        boolean isWhite = ThemeManager.isWhite();
        int baseAlpha = (int) (220 * alpha);
        int rgb = isWhite ? 0xFFFFFF : 0x0C0C0C;
        int baseColor = (baseAlpha << 24) | rgb;

        // Solid rounded card backing
        GlassRenderUtil.fillRoundedRect(graphics, x, y, width, height, 4.0f, baseColor);

        // When mouse hovers over message in chat screen, show clean outline
        if (hovered) {
            int borderAlpha = (int) (90 * alpha);
            int borderRgb = isWhite ? 0x202020 : 0xFFFFFF;
            int borderColor = (borderAlpha << 24) | borderRgb;
            GlassRenderUtil.drawRoundedOutline(graphics, x, y, width, height, 4.0f, 0.5f, borderColor);

            int hoverFillAlpha = (int) (30 * alpha);
            int hoverFill = (hoverFillAlpha << 24) | (isWhite ? 0x000000 : 0xFFFFFF);
            GlassRenderUtil.fillRoundedRect(graphics, x, y, width, height, 4.0f, hoverFill);
        }
    }

    public static void renderGlassScrollbar(GuiGraphics graphics, int x, int y, int height, float scrollPct, float visibleRatio) {
        if (height <= 4) return;
        float trackW = 2.5f;

        int trackBg = ThemeManager.isWhite() ? 0x25000000 : 0x20FFFFFF;
        GlassRenderUtil.fillRoundedRect(graphics, x, y, trackW, height, 1.25f, trackBg);

        float thumbH = Math.max(10.0f, height * visibleRatio);
        float thumbY = y + (height - thumbH) * (1.0f - scrollPct);
        GlassRenderUtil.fillRoundedRect(graphics, x, thumbY, trackW, thumbH, 1.25f, 0x80FFFFFF);
    }
}
