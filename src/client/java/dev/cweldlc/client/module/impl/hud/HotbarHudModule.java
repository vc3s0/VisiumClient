package dev.cweldlc.client.module.impl.hud;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;

public class HotbarHudModule extends Module {

    private static HotbarHudModule instance;

    private final BooleanSetting smoothAnimation = addSetting(new BooleanSetting("Smooth Slide", "Smooth selector sliding spring animation", true));
    private final BooleanSetting glassCapsule    = addSetting(new BooleanSetting("Glass Capsule", "Apple LiquidGlass translucent backing", true));
    private final BooleanSetting offhandCapsule  = addSetting(new BooleanSetting("Offhand Capsule", "Glass capsule for offhand item", true));

    public HotbarHudModule() {
        super("CustomHotbar", "Apple LiquidGlass OLED hotbar with smooth sliding selector", Category.HUD);
        setEnabled(true);
        instance = this;
    }

    public static HotbarHudModule getInstance() {
        return instance;
    }

    public boolean isSmoothAnimation() {
        return smoothAnimation.getValue();
    }

    public boolean isGlassCapsule() {
        return glassCapsule.getValue();
    }

    public boolean isOffhandCapsule() {
        return offhandCapsule.getValue();
    }
}
