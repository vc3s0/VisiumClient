package dev.cweldlc.client.module.impl.combat;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.NumberSetting;

public class VelocityModule extends Module {

    private final NumberSetting horizontal = addSetting(new NumberSetting("Horizontal", "Horizontal knockback percentage", 80.0, 0.0, 100.0, 5.0));
    private final NumberSetting vertical = addSetting(new NumberSetting("Vertical", "Vertical knockback percentage", 100.0, 0.0, 100.0, 5.0));

    public VelocityModule() {
        super("Velocity", "Reduces incoming knockback", Category.COMBAT);
    }

    public double getHorizontalFactor() {
        return horizontal.getValue() / 100.0;
    }

    public double getVerticalFactor() {
        return vertical.getValue() / 100.0;
    }
}
