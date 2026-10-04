package dev.cweldlc.client.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import dev.cweldlc.client.gui.chat.CustomChatRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList.class)
public abstract class SuggestionsListMixin {

    @Shadow @Final private Rect2i rect;
    @Shadow @Final private List<Suggestion> suggestionList;
    @Shadow private int offset;
    @Shadow private int current;
    @Shadow public abstract void select(int index);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        ci.cancel();
        CustomChatRenderer.renderSuggestions(
                graphics,
                this.rect,
                this.suggestionList,
                this.offset,
                this.current,
                Minecraft.getInstance().font,
                mouseX,
                mouseY,
                this::select
        );
    }
}
