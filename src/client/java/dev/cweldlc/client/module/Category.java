package dev.cweldlc.client.module;

import net.minecraft.resources.ResourceLocation;

public enum Category {
    COMBAT("Walka", "Combat", 0xFFEF4444, "combat"),
    MOVEMENT("Ruch", "Movement", 0xFF3B82F6, "movement"),
    RENDER("Render", "Render", 0xFFA855F7, "visuals"),
    PLAYER("Gracz", "Player", 0xFF10B981, "player"),
    HUD("Interfejs", "HUD", 0xFFF59E0B, "other");

    private final String plName;
    private final String enName;
    private final int accentColor;
    private final ResourceLocation iconLocation;

    Category(String plName, String enName, int accentColor, String iconName) {
        this.plName = plName;
        this.enName = enName;
        this.accentColor = accentColor;
        this.iconLocation = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/category/" + iconName + ".png");
    }

    public ResourceLocation getIconLocation() {
        return iconLocation;
    }

    public String getDisplayName() {
        return enName;
    }

    public String getPlName() {
        return plName;
    }

    public String getEnName() {
        return enName;
    }

    public int getAccentColor() {
        return accentColor;
    }
}
