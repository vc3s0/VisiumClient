package dev.cweldlc.client.mixin;

import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontTexture.class)
public abstract class FontTextureMixin extends AbstractTexture {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void enableSmoothFontFiltering(GlyphRenderTypes glyphRenderTypes, boolean colored, CallbackInfo ci) {
        // Switch from Minecraft's default GL_NEAREST pixelated filter to GL_LINEAR smooth vector filtering
        this.setFilter(true, false);
    }
}
