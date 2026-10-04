package dev.cweldlc.client.module;

import dev.cweldlc.client.module.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    protected final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private int keybind = GLFW.GLFW_KEY_UNKNOWN;
    private boolean enabled = false;

    // UI state animations
    private float toggleProgress = 0.0f;
    private float expandProgress = 0.0f;
    private boolean expanded = false;
    private long lastTime = System.currentTimeMillis();

    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public Module(String name, String description, Category category, int defaultKeybind) {
        this(name, description, category);
        this.keybind = defaultKeybind;
    }

    protected <S extends Setting<?>> S addSetting(S setting) {
        this.settings.add(setting);
        return setting;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TOGGLE, enabled ? 1.05f : 0.88f, 0.8f);
            dev.cweldlc.client.notification.NotificationManager.post(this.name, enabled ? "Enabled" : "Disabled", enabled);
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender2D(GuiGraphics graphics, float delta) {}
    public boolean onMouseClicked(double mouseX, double mouseY, int button) { return false; }

    public void updateAnimations(float dt) {
        lastTime = System.currentTimeMillis();
        float targetToggle = enabled ? 1.0f : 0.0f;
        this.toggleProgress += (targetToggle - this.toggleProgress) * (1.0f - (float) Math.exp(-dt * 16.0f));

        float targetExpand = expanded ? 1.0f : 0.0f;
        this.expandProgress += (targetExpand - this.expandProgress) * (1.0f - (float) Math.exp(-dt * 14.0f));
    }

    public void updateAnimations() {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastTime) / 1000.0f);
        updateAnimations(dt);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getToggleProgress() {
        return Math.max(0.0f, Math.min(1.0f, toggleProgress));
    }

    public float getEasedToggle() {
        float t = getToggleProgress();
        return t * t * (3.0f - 2.0f * t);
    }

    public float getExpandProgress() {
        return Math.max(0.0f, Math.min(1.0f, expandProgress));
    }

    public float getEasedExpand() {
        float t = getExpandProgress();
        return t * t * (3.0f - 2.0f * t);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }
}
