package dev.cweldlc.client.module.impl.player;

import dev.cweldlc.client.mixin.MinecraftAccessor;
import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.NumberSetting;

public class FastPlaceModule extends Module {

    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks of block placement delay", 0.0, 0.0, 4.0, 1.0));

    public FastPlaceModule() {
        super("FastPlace", "Removes block placement delay", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc != null && isEnabled()) {
            MinecraftAccessor accessor = (MinecraftAccessor) mc;
            if (accessor.cweldlc$getRightClickDelay() > (int) delay.getValue().doubleValue()) {
                accessor.cweldlc$setRightClickDelay((int) delay.getValue().doubleValue());
            }
        }
    }
}
