package dev.cweldlc.client.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class FramerateLimitTrackerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private int framerateLimit;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void cweldlc$unlockMenuFramerate(CallbackInfoReturnable<Integer> cir) {
        if (this.minecraft.getWindow().isIconified()) {
            cir.setReturnValue(10);
            return;
        }

        // When in main menu (level == null), unlock framerate to unlimited (260)
        if (this.minecraft.level == null) {
            cir.setReturnValue(Math.max(this.framerateLimit, 260));
        }
    }
}
