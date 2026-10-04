package dev.cweldlc.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cweldlc.client.module.ModuleManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void cweldlc$onBobHurt(PoseStack poseStack, float f, CallbackInfo ci) {
        if (ModuleManager.getInstance().getModule("NoHurtCam") != null 
                && ModuleManager.getInstance().getModule("NoHurtCam").isEnabled()) {
            ci.cancel();
        }
    }
}
