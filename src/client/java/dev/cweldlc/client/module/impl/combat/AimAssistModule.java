package dev.cweldlc.client.module.impl.combat;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.ColorSetting;
import dev.cweldlc.client.module.setting.ModeSetting;
import dev.cweldlc.client.module.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AimAssistModule extends Module {

    private final NumberSetting range = addSetting(new NumberSetting("Range", "Maximum targeting radius", 4.5, 2.0, 6.0, 0.1));
    private final NumberSetting smooth = addSetting(new NumberSetting("Smoothness", "Aim rotation interpolation speed", 6.5, 1.0, 15.0, 0.5));
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Target selection priority", "Nearest", "Nearest", "Fov", "Health"));
    private final BooleanSetting weaponOnly = addSetting(new BooleanSetting("Weapons Only", "Only track when holding a weapon", true));
    private final ColorSetting traceColor = addSetting(new ColorSetting("Color Picker", "Target crosshair tracer color", 0xFF9372FF));

    public AimAssistModule() {
        super("AimAssist", "Smoothly assists aiming towards opponents", Category.COMBAT, GLFW.GLFW_KEY_O);
    }
}
