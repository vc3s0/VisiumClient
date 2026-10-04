package dev.cweldlc.client.mixin;

import dev.cweldlc.client.module.ModuleManager;
import dev.cweldlc.client.module.impl.combat.VelocityModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void cweldlc$onHandleSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (this.minecraft.player != null && packet.getId() == this.minecraft.player.getId()) {
            VelocityModule vel = (VelocityModule) ModuleManager.getInstance().getModule("Velocity");
            if (vel != null && vel.isEnabled()) {
                double hFactor = vel.getHorizontalFactor();
                double vFactor = vel.getVerticalFactor();
                ci.cancel();
                if (hFactor > 0.001 || vFactor > 0.001) {
                    this.minecraft.player.lerpMotion(packet.getXa() * hFactor, packet.getYa() * vFactor, packet.getZa() * hFactor);
                }
            }
        }
    }
}
