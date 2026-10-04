package dev.cweldlc.client.mixin;

import dev.cweldlc.client.module.impl.hud.HotbarHudModule;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow protected abstract Player getCameraPlayer();
    @Shadow protected abstract void renderSlot(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed);

    @Unique private static float smoothSelectedX = -1.0f;
    @Unique private static long lastHotbarTime = System.currentTimeMillis();

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderItemHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HotbarHudModule mod = HotbarHudModule.getInstance();
        if (mod == null || !mod.isEnabled()) {
            return;
        }

        Player player = this.getCameraPlayer();
        if (player == null) return;

        ci.cancel();

        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastHotbarTime) / 1000.0f);
        lastHotbarTime = now;

        ItemStack offhandItem = player.getOffhandItem();
        HumanoidArm offhandArm = player.getMainArm().getOpposite();

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();
        int midX = screenW / 2;

        int barW = 182;
        int barH = 22;
        int barX = midX - barW / 2;
        int barY = screenH - barH - 1;
        float barRadius = 6.0f;

        // 1. Apple LiquidGlass Hotbar Main Capsule Backing
        if (mod.isGlassCapsule()) {
            // Dark OLED glass
            GlassRenderUtil.fillRoundedRect(guiGraphics, barX, barY, barW, barH, barRadius, 0xD00A0A0C);
            // Ultra-thin subtle glass outline
            GlassRenderUtil.drawRoundedOutline(guiGraphics, barX, barY, barW, barH, barRadius, 0.5f, 0x25FFFFFF);

            // Subtle slot separator lines
            for (int i = 1; i < 9; i++) {
                float sepX = barX + 1.0f + i * 20.0f;
                GlassRenderUtil.fillRoundedRect(guiGraphics, sepX, barY + 3.0f, 1.0f, barH - 6.0f, 0.5f, 0x0EFFFFFF);
            }
        }

        // 2. Animated Sliding Selector Pill Indicator
        int selected = player.getInventory().selected;
        float targetPillX = barX + 1.0f + selected * 20.0f;
        float pillW = 20.0f;
        float pillH = 20.0f;
        float pillY = barY + 1.0f;
        float pillRadius = 5.0f;

        if (!mod.isSmoothAnimation() || smoothSelectedX < 0.0f || Math.abs(smoothSelectedX - targetPillX) > barW) {
            smoothSelectedX = targetPillX;
        } else {
            smoothSelectedX += (targetPillX - smoothSelectedX) * (1.0f - (float) Math.exp(-dt * 24.0f));
        }

        // Frosted white sliding pill
        GlassRenderUtil.fillRoundedRect(guiGraphics, smoothSelectedX, pillY, pillW, pillH, pillRadius, 0x30FFFFFF);
        GlassRenderUtil.drawRoundedOutline(guiGraphics, (int) smoothSelectedX, (int) pillY, (int) pillW, (int) pillH, pillRadius, 0.6f, 0x90FFFFFF);
        // Clean white accent dot
        GlassRenderUtil.fillRoundedRect(guiGraphics, smoothSelectedX + 6.0f, pillY + 1.0f, 8.0f, 1.5f, 0.75f, 0xFFFFFFFF);

        // 3. Floating Offhand Glass Capsule
        if (!offhandItem.isEmpty()) {
            int offhandW = 22;
            int offhandH = 22;
            int offhandX = (offhandArm == HumanoidArm.LEFT) ? (barX - offhandW - 6) : (barX + barW + 6);
            int offhandY = barY;

            if (mod.isOffhandCapsule()) {
                GlassRenderUtil.fillRoundedRect(guiGraphics, offhandX, offhandY, offhandW, offhandH, barRadius, 0xD00A0A0C);
                GlassRenderUtil.drawRoundedOutline(guiGraphics, offhandX, offhandY, offhandW, offhandH, barRadius, 0.5f, 0x25FFFFFF);
            }

            int slotItemY = screenH - 16 - 4;
            int slotItemX = offhandX + 3;
            this.renderSlot(guiGraphics, slotItemX, slotItemY, deltaTracker, player, offhandItem, 0);
        }

        // 4. Render 9 Hotbar Slots
        int seed = 1;
        int slotY = screenH - 16 - 4;
        for (int i = 0; i < 9; ++i) {
            int slotX = barX + 3 + i * 20;
            ItemStack stack = player.getInventory().items.get(i);
            this.renderSlot(guiGraphics, slotX, slotY, deltaTracker, player, stack, seed++);
        }
    }
}
