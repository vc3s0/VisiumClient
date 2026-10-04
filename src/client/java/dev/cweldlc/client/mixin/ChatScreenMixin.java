package dev.cweldlc.client.mixin;

import dev.cweldlc.client.gui.chat.CustomChatRenderer;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow protected EditBox input;
    @Shadow CommandSuggestions commandSuggestions;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int barX = 4;
        int barY = this.height - 20;
        int barW = this.width - 8;

        int editX = barX + 8;
        int editY = barY + 4;
        int editW = barW - 16;
        int editH = 10;

        this.input.setX(editX);
        this.input.setY(editY);
        this.input.setWidth(editW);
        this.input.setHeight(editH);
        this.input.setBordered(false);
        this.input.setTextColor(0x00000000);
        this.input.setTextColorUneditable(0x00000000);

        // Remove input from renderables so vanilla bitmap font is never drawn,
        // while keeping it in children for focus, typing and click events.
        this.removeWidget(this.input);
        this.addWidget(this.input);
        this.setFocused(this.input);
        this.input.setFocused(true);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        // 1. Render HUD chat messages
        if (this.minecraft != null && this.minecraft.gui != null && this.minecraft.gui.getChat() != null) {
            this.minecraft.gui.getChat().render(graphics, this.minecraft.gui.getGuiTicks(), mouseX, mouseY, true);
        }

        // 2. Render Custom floating chat input bar (Razor-sharp MSDF typography)
        CustomChatRenderer.renderChatInputBar(graphics, this.input, this.width, this.height, mouseX, mouseY, this.font);

        // 3. Render super (other screen widgets, if any)
        super.render(graphics, mouseX, mouseY, partialTick);

        // 5. Render Command Suggestions elevated
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        this.commandSuggestions.render(graphics, mouseX, mouseY);
        graphics.pose().popPose();

        // 6. Tooltips & Hover events
        if (this.minecraft != null && this.minecraft.gui != null && this.minecraft.gui.getChat() != null) {
            GuiMessageTag tag = this.minecraft.gui.getChat().getMessageTagAt((double) mouseX, (double) mouseY);
            if (tag != null && tag.text() != null) {
                graphics.renderTooltip(this.font, this.font.split(tag.text(), 210), mouseX, mouseY);
            } else {
                Style style = this.minecraft.gui.getChat().getClickedComponentStyleAt((double) mouseX, (double) mouseY);
                if (style != null && style.getHoverEvent() != null) {
                    graphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
                }
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        for (dev.cweldlc.client.module.Module mod : dev.cweldlc.client.module.ModuleManager.getInstance().getModules()) {
            if (mod.isEnabled() && mod.onMouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }


    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
            dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TYPING, 0.85f, 0.6f);
        }
    }
}
