package dev.cweldlc.client.module.setting;

public class ColorSetting extends Setting<Integer> {

    public ColorSetting(String name, String description, int defaultColor) {
        super(name, description, defaultColor);
    }

    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public int getAlpha() {
        return (getValue() >> 24) & 0xFF;
    }

    public String getHex() {
        return String.format("#%06X", (0xFFFFFF & getValue()));
    }
}
