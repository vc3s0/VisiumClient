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
    public static final SoundEvent ENABLE = register("enable");
    public static final SoundEvent DISABLE = register("disable");
    public static final SoundEvent TYPING = register("typing");
    public static final SoundEvent CRITICAL = register("critical");
    public static final SoundEvent WELCOME = register("welcome");
    public static final SoundEvent APPLEPAY = register("applepay");
    public static final SoundEvent CLICKGUI_OPEN = register("clickgui_open");
    public static final SoundEvent BONK = register("bonk");
    public static final SoundEvent METALLIC = register("metallic");
    public static final SoundEvent KILL = register("playerkill");
    public static final SoundEvent ERROR = register("error");
    public static final SoundEvent SWITCH = register("switch");

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

    public static void playToggle() {
        play(TOGGLE, 1.0f, 0.8f);
    }

    public static void playEnable() {
        play(ENABLE, 1.0f, 0.85f);
    }

    public static void playDisable() {
        play(DISABLE, 0.9f, 0.8f);
    }

    public static void playCategorySwitch() {
        play(SWITCH, 1.15f, 0.75f);
    }

    public static void playOpen() {
        play(CLICKGUI_OPEN, 1.0f, 0.9f);
    }
}
