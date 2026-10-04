package dev.cweldlc.client.mixin;

import dev.cweldlc.client.module.ModuleManager;
import dev.cweldlc.client.module.impl.combat.VelocityModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void cweldlc$onHandleSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && packet.getId() == mc.player.getId()) {
            VelocityModule vel = (VelocityModule) ModuleManager.getInstance().getModule("Velocity");
            if (vel != null && vel.isEnabled()) {
                double hFactor = vel.getHorizontalFactor();
                double vFactor = vel.getVerticalFactor();
                ci.cancel();
                if (hFactor > 0.001 || vFactor > 0.001) {
                    mc.player.lerpMotion(packet.getXa() * hFactor, packet.getYa() * vFactor, packet.getZa() * hFactor);
                }
            }
        }
    }
}
