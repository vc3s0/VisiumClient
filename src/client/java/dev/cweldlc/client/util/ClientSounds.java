package dev.cweldlc.client.util;

import dev.cweldlc.CwelDLC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ClientSounds {

    public static final SoundEvent TOGGLE = register("toggle");
    public static final SoundEvent TYPING = register("typing");
    public static final SoundEvent CRITICAL = register("critical");
    public static final SoundEvent WELCOME = register("welcome");
    public static final SoundEvent APPLEPAY = register("applepay");
    public static final SoundEvent CLICKGUI_OPEN = register("clickgui_open");

    private ClientSounds() {}

    private static SoundEvent register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CwelDLC.MOD_ID, name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
    }

    public static void init() {
        // Trigger static init & registration
    }

    public static void play(SoundEvent sound, float pitch, float volume) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getSoundManager() != null && sound != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
            }
        } catch (Throwable ignored) {}
    }

    public static void play(SoundEvent sound) {
        play(sound, 1.0f, 0.8f);
    }
}
