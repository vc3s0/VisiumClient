package dev.cweldlc.client.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.cweldlc.CwelDLC;
import dev.cweldlc.client.module.impl.combat.AimAssistModule;
import dev.cweldlc.client.module.impl.combat.AntiBotModule;
import dev.cweldlc.client.module.impl.combat.AttackAuraModule;
import dev.cweldlc.client.module.impl.combat.AutoClickerModule;
import dev.cweldlc.client.module.impl.combat.SuperBowModule;
import dev.cweldlc.client.module.impl.combat.VelocityModule;
import dev.cweldlc.client.module.impl.hud.DynamicIslandHudModule;
import dev.cweldlc.client.module.impl.hud.MediaPlayerHudModule;
import dev.cweldlc.client.module.impl.hud.WatermarkHudModule;
import dev.cweldlc.client.module.impl.movement.SprintModule;
import dev.cweldlc.client.module.impl.player.FastPlaceModule;
import dev.cweldlc.client.module.impl.render.FullbrightModule;
import dev.cweldlc.client.module.impl.render.NoHurtCamModule;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.ModeSetting;
import dev.cweldlc.client.module.setting.NumberSetting;
import dev.cweldlc.client.module.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {

    private static ModuleManager instance;

    private final List<Module> modules = new ArrayList<>();
    private File configFile;

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    private ModuleManager() {
        registerModules();
        loadConfig();
    }

    private void registerModules() {
        // Combat
        modules.add(new AttackAuraModule());
        modules.add(new AntiBotModule());
        modules.add(new AimAssistModule());
        modules.add(new SuperBowModule());
        modules.add(new AutoClickerModule());
        modules.add(new VelocityModule());

        // Movement
        modules.add(new SprintModule());

        // Render
        modules.add(new FullbrightModule());
        modules.add(new NoHurtCamModule());

        // Player
        modules.add(new FastPlaceModule());

        // HUD
        modules.add(new WatermarkHudModule());
        modules.add(new MediaPlayerHudModule());
        modules.add(new DynamicIslandHudModule());
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Module getModule(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public void onTick() {
        for (Module m : modules) {
            m.updateAnimations();
            if (m.isEnabled()) {
                try {
                    m.onTick();
                } catch (Exception e) {
                    CwelDLC.LOGGER.error("Error ticking module: " + m.getName(), e);
                }
            }
        }
    }

    public void onRender2D(GuiGraphics graphics, float delta) {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try {
                    m.onRender2D(graphics, delta);
                } catch (Exception e) {
                    CwelDLC.LOGGER.error("Error rendering module 2D: " + m.getName(), e);
                }
            }
        }
    }

    public void onKey(int key, int action) {
        if (action == GLFW.GLFW_PRESS && key != GLFW.GLFW_KEY_UNKNOWN) {
            for (Module m : modules) {
                if (m.getKeybind() == key) {
                    m.toggle();
                    saveConfig();
                }
            }
        }
    }

    private File getConfigFile() {
        if (configFile == null) {
            File gameDir = Minecraft.getInstance().gameDirectory;
            if (gameDir == null) {
                gameDir = new File(".");
            }
            configFile = new File(gameDir, "config/cweldlc/modules.json");
        }
        return configFile;
    }

    public void loadConfig() {
        try {
            File file = getConfigFile();
            if (!file.exists()) return;

            try (FileReader reader = new FileReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (Module m : modules) {
                    if (json.has(m.getName())) {
                        JsonObject modJson = json.getAsJsonObject(m.getName());
                        if (modJson.has("enabled")) {
                            m.setEnabled(modJson.get("enabled").getAsBoolean());
                        }
                        if (modJson.has("keybind")) {
                            m.setKeybind(modJson.get("keybind").getAsInt());
                        }
                        if (modJson.has("settings")) {
                            JsonObject setJson = modJson.getAsJsonObject("settings");
                            for (Setting<?> s : m.getSettings()) {
                                if (setJson.has(s.getName())) {
                                    JsonElement val = setJson.get(s.getName());
                                    if (s instanceof BooleanSetting bs) {
                                        bs.setValue(val.getAsBoolean());
                                    } else if (s instanceof NumberSetting ns) {
                                        ns.setValue(val.getAsDouble());
                                    } else if (s instanceof ModeSetting ms) {
                                        ms.setValue(val.getAsString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CwelDLC.LOGGER.error("Failed to load module config", e);
        }
    }

    public void saveConfig() {
        try {
            File file = getConfigFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            JsonObject root = new JsonObject();
            for (Module m : modules) {
                JsonObject modJson = new JsonObject();
                modJson.addProperty("enabled", m.isEnabled());
                modJson.addProperty("keybind", m.getKeybind());

                JsonObject setJson = new JsonObject();
                for (Setting<?> s : m.getSettings()) {
                    if (s instanceof BooleanSetting bs) {
                        setJson.addProperty(s.getName(), bs.getValue());
                    } else if (s instanceof NumberSetting ns) {
                        setJson.addProperty(s.getName(), ns.getValue());
                    } else if (s instanceof ModeSetting ms) {
                        setJson.addProperty(s.getName(), ms.getValue());
                    }
                }
                modJson.add("settings", setJson);
                root.add(m.getName(), modJson);
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(root.toString());
            }
        } catch (Exception e) {
            CwelDLC.LOGGER.error("Failed to save module config", e);
        }
    }
}
