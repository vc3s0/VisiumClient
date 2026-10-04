package dev.cweldlc.client.mixin;

import dev.cweldlc.client.util.ClientSounds;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EditBox.class)
public class EditBoxMixin {

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void cweldlc$onCharTyped(char codePoint, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        ClientSounds.play(ClientSounds.TYPING, 0.92f + (float) (Math.random() * 0.16), 0.6f);
    }
}
