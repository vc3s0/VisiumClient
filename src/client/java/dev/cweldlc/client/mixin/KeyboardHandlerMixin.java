package dev.cweldlc.client.mixin;

import dev.cweldlc.client.module.ModuleManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void cweldlc$onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (Minecraft.getInstance().screen == null) {
            ModuleManager.getInstance().onKey(key, action);
        }
    }
}
