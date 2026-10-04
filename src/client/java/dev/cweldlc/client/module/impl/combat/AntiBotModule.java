package dev.cweldlc.client.module.impl.combat;

import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class AntiBotModule extends Module {

    private final ModeSetting mode = addSetting(new ModeSetting("Detection", "Bot filter mode", "Matrix", "Matrix", "Watchdog", "Grim"));
    private final BooleanSetting tabCheck = addSetting(new BooleanSetting("Tab Check", "Ensure entity is in player tablist", true));
    private final BooleanSetting remove = addSetting(new BooleanSetting("Remove Bot", "Clientside remove fake bot entities", true));

    public AntiBotModule() {
        super("AntiBot", "Filters fake entities and anti-cheat bots", Category.COMBAT, GLFW.GLFW_KEY_H);
    }
}
