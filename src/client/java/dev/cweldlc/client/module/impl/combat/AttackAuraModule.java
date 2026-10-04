package dev.cweldlc.client.module.impl.combat;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.ColorSetting;
import dev.cweldlc.client.module.setting.ModeSetting;
import dev.cweldlc.client.module.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AttackAuraModule extends Module {

    private final NumberSetting range = addSetting(new NumberSetting("Range", "Attack reach distance", 4.5, 3.0, 6.0, 0.1));
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Target selection mode", "Switch", "Single", "Switch", "Multi"));
    private final NumberSetting aps = addSetting(new NumberSetting("APS", "Attacks per second", 11.5, 1.0, 20.0, 0.5));
    private final BooleanSetting raytrace = addSetting(new BooleanSetting("Raytrace", "Check line of sight", true));
    private final BooleanSetting checkInvis = addSetting(new BooleanSetting("Invisibles", "Target invisible entities", false));
    private final ColorSetting targetColor = addSetting(new ColorSetting("ESP Color", "Target highlight ESP color", 0xFF9372FF));

    public AttackAuraModule() {
        super("AttackAura", "Automatically attacks nearby entities", Category.COMBAT, GLFW.GLFW_KEY_G);
    }
}
