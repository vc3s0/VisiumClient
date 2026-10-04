package dev.cweldlc.client.module.impl.render;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class FullbrightModule extends Module {

    private final NumberSetting brightness = addSetting(new NumberSetting("Brightness", "Gamma brightness multiplier", 15.0, 1.0, 20.0, 1.0));
    private double previousGamma = 1.0;

    public FullbrightModule() {
        super("Fullbright", "Maximum brightness in darkness and caves", Category.RENDER, GLFW.GLFW_KEY_B);
    }

    @Override
    public void onEnable() {
        if (mc.options != null) {
            previousGamma = mc.options.gamma().get();
            mc.options.gamma().set(brightness.getValue());
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.gamma().set(previousGamma);
        }
    }

    @Override
    public void onTick() {
        if (mc.options != null && isEnabled()) {
            if (mc.options.gamma().get() < brightness.getValue()) {
                mc.options.gamma().set(brightness.getValue());
            }
        }
    }
}
