package dev.cweldlc.client.mixin;

import dev.cweldlc.client.gui.chat.CustomChatRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Shadow @Final private Screen screen;
    @Shadow @Final Font font;
    @Shadow @Final boolean anchorToBottom;
    @Shadow private List<FormattedCharSequence> commandUsage;
    @Shadow private int commandUsagePosition;
    @Shadow private int commandUsageWidth;

    @Inject(method = "renderUsage", at = @At("HEAD"), cancellable = true)
    private void onRenderUsage(GuiGraphics graphics, CallbackInfo ci) {
        ci.cancel();
        CustomChatRenderer.renderCommandUsage(
                graphics,
                this.commandUsage,
                this.commandUsagePosition,
                this.commandUsageWidth,
                this.anchorToBottom,
                this.screen.height,
                this.font
        );
    }
}
