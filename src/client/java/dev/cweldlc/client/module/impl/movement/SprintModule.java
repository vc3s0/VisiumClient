package dev.cweldlc.client.module.impl.movement;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public class SprintModule extends Module {

    private final BooleanSetting keepSprint = addSetting(new BooleanSetting("KeepSprint", "Maintains sprinting after hitting targets", true));

    public SprintModule() {
        super("Sprint", "Automatically sprints when moving forward", Category.MOVEMENT, GLFW.GLFW_KEY_V);
    }

    @Override
    public void onTick() {
        if (mc.player != null && mc.options != null) {
            if (!mc.player.isShiftKeyDown() && !mc.player.horizontalCollision && mc.player.input.hasForwardImpulse()) {
                mc.player.setSprinting(true);
            }
        }
    }

    public boolean isKeepSprint() {
        return keepSprint.getValue();
    }
}
