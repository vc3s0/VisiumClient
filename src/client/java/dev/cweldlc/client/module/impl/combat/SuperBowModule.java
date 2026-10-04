package dev.cweldlc.client.module.impl.combat;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.NumberSetting;

public class SuperBowModule extends Module {

    private final NumberSetting packets = addSetting(new NumberSetting("Packets", "Arrow velocity boost multiplier", 20.0, 5.0, 50.0, 1.0));
    private final BooleanSetting autoRelease = addSetting(new BooleanSetting("Auto Release", "Releases bow arrow when fully pulled", true));

    public SuperBowModule() {
        super("SuperBow", "Shoots high-velocity critical bow shots", Category.COMBAT);
    }
}
