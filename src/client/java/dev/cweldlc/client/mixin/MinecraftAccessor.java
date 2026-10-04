package dev.cweldlc.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay")
    int cweldlc$getRightClickDelay();

    @Accessor("rightClickDelay")
    void cweldlc$setRightClickDelay(int delay);

    @org.spongepowered.asm.mixin.gen.Invoker("startAttack")
    boolean cweldlc$startAttack();

    @org.spongepowered.asm.mixin.gen.Invoker("startUseItem")
    void cweldlc$startUseItem();
}
