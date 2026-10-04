package dev.cweldlc.client.module.setting;

import java.util.List;

public class ModeSetting extends Setting<String> {

    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultMode, String... modes) {
        super(name, description, defaultMode);
        this.modes = List.of(modes);
    }

    public List<String> getModes() {
        return modes;
    }

    public void cycle() {
        int index = modes.indexOf(getValue());
        if (index == -1) {
            setValue(modes.getFirst());
        } else {
            setValue(modes.get((index + 1) % modes.size()));
        }
    }
}
