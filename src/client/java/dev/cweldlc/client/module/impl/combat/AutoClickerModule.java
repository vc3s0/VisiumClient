package dev.cweldlc.client.module.impl.combat;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.NumberSetting;

import java.util.Random;

public class AutoClickerModule extends Module {

    private final NumberSetting cps = addSetting(new NumberSetting("CPS", "Clicks per second speed", 12.0, 6.0, 20.0, 1.0));
    private final BooleanSetting jitter = addSetting(new BooleanSetting("Jitter", "Randomized click timing interval", true));
    private final BooleanSetting rightClick = addSetting(new BooleanSetting("Right Click", "Automatically clicks right mouse button", false));

    private long lastClickTime = 0;
    private final Random random = new Random();

    public AutoClickerModule() {
        super("AutoClicker", "Automates rapid mouse clicks", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.screen != null) return;

        boolean shouldClick = mc.options.keyAttack.isDown() || (rightClick.getValue() && mc.options.keyUse.isDown());
        if (!shouldClick) return;

        long now = System.currentTimeMillis();
        double baseDelay = 1000.0 / cps.getValue();
        if (jitter.getValue()) {
            baseDelay += (random.nextDouble() - 0.5) * 30.0;
        }

        if (now - lastClickTime >= baseDelay) {
            lastClickTime = now;
            if (mc.options.keyAttack.isDown()) {
                ((dev.cweldlc.client.mixin.MinecraftAccessor) mc).cweldlc$startAttack();
            } else if (rightClick.getValue() && mc.options.keyUse.isDown()) {
                ((dev.cweldlc.client.mixin.MinecraftAccessor) mc).cweldlc$startUseItem();
            }
        }
    }
}
