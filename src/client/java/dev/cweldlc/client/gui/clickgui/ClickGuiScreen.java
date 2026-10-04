package dev.cweldlc.client.gui.clickgui;

import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.ModuleManager;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.ColorSetting;
import dev.cweldlc.client.module.setting.ModeSetting;
import dev.cweldlc.client.module.setting.NumberSetting;
import dev.cweldlc.client.module.setting.Setting;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.ClientSounds;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickGuiScreen extends Screen {

    private final Screen parent;
    private Category currentCategory = Category.COMBAT;
    private String selectedSubTab = "Combat";

    // Active state trackers
    private Module bindingModule = null;
    private NumberSetting draggingSlider = null;
    private ColorSetting activeColorPickerSetting = null;
    private float colorPickerX = 0;
    private float colorPickerY = 0;
    private boolean isDraggingHue = false;
    private boolean isDraggingSV = false;
    private float pickerHue = 0.75f;
    private float pickerSat = 0.55f;
    private float pickerVal = 1.0f;

    // Dimensions
    private static final float WIN_WIDTH = 620.0f;
    private static final float WIN_HEIGHT = 390.0f;
    private static final float SIDEBAR_WIDTH = 138.0f;

    // Animations
    private float openProgress = 0.0f;
    private boolean isClosing = false;
    private float closeProgress = 1.0f;
    private long lastTime = System.currentTimeMillis();

    // Scroll
    private float scrollY = 0.0f;
    private float targetScrollY = 0.0f;

    // Search
    private String searchQuery = "";
    private boolean searchFocused = false;

    // Animation maps
    private final Map<Module, Float> moduleToggleMap = new HashMap<>();
    private final Map<Module, Float> moduleGearHoverMap = new HashMap<>();
    private final Map<BooleanSetting, Float> boolToggleMap = new HashMap<>();
    private final Map<NumberSetting, Float> sliderValueMap = new HashMap<>();
    private final Map<String, Float> tabHoverMap = new HashMap<>();

    public ClickGuiScreen() {
        this(null);
    }

    public ClickGuiScreen(Screen parent) {
        super(Component.literal("VisiumClient ClickGUI"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        openProgress = 0.0f;
        isClosing = false;
        closeProgress = 1.0f;
        lastTime = System.currentTimeMillis();
        ClientSounds.play(ClientSounds.CLICKGUI_OPEN, 1.0f, 0.85f);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastTime) / 1000.0f);
        lastTime = now;

        // 1. Spring interpolation for open/close
        if (!isClosing) {
            openProgress += (1.0f - openProgress) * (1.0f - (float) Math.exp(-dt * 15.0f));
        } else {
            closeProgress += (0.0f - closeProgress) * (1.0f - (float) Math.exp(-dt * 18.0f));
            if (closeProgress <= 0.02f) {
                actuallyClose();
                return;
            }
        }

        scrollY += (targetScrollY - scrollY) * (1.0f - (float) Math.exp(-dt * 14.0f));

        float animProgress = isClosing ? closeProgress : openProgress;
        float currentAlpha = Math.max(0.0f, Math.min(1.0f, animProgress));

        // 2. Cinematic backdrop dim
        int dimAlpha = (int) (currentAlpha * 0x85);
        graphics.fill(0, 0, this.width, this.height, dimAlpha << 24);

        float winX = (this.width - WIN_WIDTH) / 2.0f;
        float winY = (this.height - WIN_HEIGHT) / 2.0f;
        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;

        // 3. Fluid Spring Scale
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0f);
        float scale = 0.90f + 0.10f * animProgress;
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-centerX, -centerY, 0.0f);

        // 4. Main Obsidian Background Panel (Wayne DLC layout)
        int mainBg = applyAlpha(0xFF0D0D12, currentAlpha);
        GlassRenderUtil.fillRoundedRect(graphics, winX, winY, WIN_WIDTH, WIN_HEIGHT, 16.0f, mainBg);

        // Update module animations
        for (Module m : ModuleManager.getInstance().getModules()) {
            m.updateAnimations(dt);
        }

        // 5. Left Sidebar
        renderSidebar(graphics, winX, winY, mouseX, mouseY, dt, currentAlpha);

        // 6. Top Bar (Breadcrumbs & Search)
        float mainX = winX + SIDEBAR_WIDTH + 14.0f;
        float mainW = (winX + WIN_WIDTH) - mainX - 14.0f;
        renderTopBar(graphics, mainX, winY, mainW, mouseX, mouseY, dt, currentAlpha);

        // 7. Content Area: 2-Column Cards Grid
        renderContent(graphics, mainX, winY + 38.0f, mainW, WIN_HEIGHT - 48.0f, mouseX, mouseY, dt, currentAlpha);

        // 8. Floating Color Picker Popup (if open)
        if (activeColorPickerSetting != null) {
            renderColorPickerPopup(graphics, mouseX, mouseY, dt, currentAlpha);
        }

        graphics.pose().popPose();
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderSidebar(GuiGraphics graphics, float winX, float winY, int mouseX, int mouseY, float dt, float alpha) {
        float x = winX;
        float w = SIDEBAR_WIDTH;

        // Top App Icon (Squircle with purple gradient)
        float iconX = x + 14.0f;
        float iconY = winY + 14.0f;
        float iconSize = 28.0f;
        GlassRenderUtil.fillGradientRoundedRect(graphics, iconX, iconY, iconSize, iconSize, 8.0f, applyAlpha(0xFF7C3AED, alpha), applyAlpha(0xFFA855F7, alpha));

        // White stylized glyph inside squircle
        GlassRenderUtil.fillRoundedRect(graphics, iconX + 7.0f, iconY + 8.0f, 4.0f, 12.0f, 2.0f, applyAlpha(0xFFFFFFFF, alpha));
        GlassRenderUtil.fillRoundedRect(graphics, iconX + 17.0f, iconY + 8.0f, 4.0f, 12.0f, 2.0f, applyAlpha(0xFFFFFFFF, alpha));
        GlassRenderUtil.fillRoundedRect(graphics, iconX + 7.0f, iconY + 16.0f, 14.0f, 4.0f, 2.0f, applyAlpha(0xFFFFFFFF, alpha));

        // App Branding Text
        MsdfRenderer.renderText(Fonts.bold(), "Wayne DLC", 10.5f, applyAlpha(0xFFFFFFFF, alpha), graphics.pose().last().pose(), x + 48.0f, winY + 15.0f, 0.0f);
        MsdfRenderer.renderText(Fonts.regular(), "Recode", 7.0f, applyAlpha(0xFF6B7280, alpha), graphics.pose().last().pose(), x + 48.0f, winY + 28.5f, 0.0f);

        float curY = winY + 52.0f;

        // Group 1: MAIN
        curY = renderNavSection(graphics, "Main", curY, x, w, mouseX, mouseY, dt, alpha,
                new String[]{"Combat", "Movement", "Player", "Visuals"},
                new Category[]{Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.RENDER}
        );

        // Group 2: CLIENT
        curY = renderNavSection(graphics, "Client", curY + 6.0f, x, w, mouseX, mouseY, dt, alpha,
                new String[]{"Themes", "Configs"},
                new Category[]{null, null}
        );

        // Group 3: OTHER
        curY = renderNavSection(graphics, "Other", curY + 6.0f, x, w, mouseX, mouseY, dt, alpha,
                new String[]{"Favorites", "Friends"},
                new Category[]{null, null}
        );

        // Bottom User Profile Card
        float profY = winY + WIN_HEIGHT - 44.0f;
        float avatarX = x + 14.0f;
        float avatarY = profY + 3.0f;
        float avatarSize = 24.0f;

        // Circular avatar with cyan/purple gradient
        GlassRenderUtil.fillGradientRoundedRect(graphics, avatarX, avatarY, avatarSize, avatarSize, avatarSize / 2.0f, applyAlpha(0xFF0284C7, alpha), applyAlpha(0xFF6366F1, alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, (int) (avatarSize / 2.0f), 1.0f, applyAlpha(0xFF38BDF8, alpha));

        // Inner initial
        String username = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() : System.getProperty("user.name", "Username_1");
        String initial = username.isEmpty() ? "U" : username.substring(0, 1).toUpperCase();
        MsdfRenderer.renderCenteredText(Fonts.bold(), initial, 11.0f, applyAlpha(0xFFFFFFFF, alpha), graphics.pose().last().pose(), avatarX + avatarSize / 2.0f, avatarY + 7.0f, 0.0f);

        // Username & Expiry date
        MsdfRenderer.renderText(Fonts.bold(), username, 8.5f, applyAlpha(0xFFFFFFFF, alpha), graphics.pose().last().pose(), avatarX + avatarSize + 8.0f, profY + 5.0f, 0.0f);
        MsdfRenderer.renderText(Fonts.regular(), "Till: 12.05.2027", 6.5f, applyAlpha(0xFF6B7280, alpha), graphics.pose().last().pose(), avatarX + avatarSize + 8.0f, profY + 17.0f, 0.0f);

        // Purple spinner status ring
        float ringX = x + w - 18.0f;
        float ringY = profY + 14.0f;
        drawSpinnerRing(graphics, ringX, ringY, 5.0f, applyAlpha(0xFFA78BFA, alpha));

        // Vertical divider
        float divX = winX + w;
        GlassRenderUtil.fillRoundedRect(graphics, divX, winY + 10.0f, 1.0f, WIN_HEIGHT - 20.0f, 0.5f, applyAlpha(0xFF181820, alpha));
    }

    private float renderNavSection(GuiGraphics graphics, String header, float startY, float x, float w, int mouseX, int mouseY, float dt, float alpha, String[] items, Category[] categories) {
        // Section Header Label
        MsdfRenderer.renderText(Fonts.medium(), header, 6.5f, applyAlpha(0xFF6B7280, alpha), graphics.pose().last().pose(), x + 16.0f, startY, 0.0f);
        float itemY = startY + 11.0f;
        float itemH = 22.0f;
        float itemW = w - 24.0f;
        float itemX = x + 12.0f;

        for (int i = 0; i < items.length; i++) {
            String name = items[i];
            Category cat = categories[i];
            boolean selected = selectedSubTab.equalsIgnoreCase(name);
            boolean hovered = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= itemY && mouseY <= itemY + itemH;

            float hover = tabHoverMap.getOrDefault(name, 0.0f);
            hover += ((hovered ? 1.0f : 0.0f) - hover) * (1.0f - (float) Math.exp(-dt * 16.0f));
            tabHoverMap.put(name, hover);

            if (selected) {
                // Active container pill card matching Wayne DLC screenshot
                GlassRenderUtil.fillRoundedRect(graphics, itemX, itemY, itemW, itemH, 6.0f, applyAlpha(0xFF181822, alpha));
                GlassRenderUtil.drawRoundedOutline(graphics, (int) itemX, (int) itemY, (int) itemW, (int) itemH, 6, 0.6f, applyAlpha(0xFF282836, alpha));
            } else if (hover > 0.01f) {
                GlassRenderUtil.fillRoundedRect(graphics, itemX, itemY, itemW, itemH, 6.0f, applyAlpha(0xFF121218, alpha * hover));
            }

            // Category Icon glyph
            int iconColor = selected ? 0xFFA78BFA : (hovered ? 0xFFCBD5E1 : 0xFF6B7280);
            renderNavIcon(graphics, name, itemX + 8.0f, itemY + (itemH - 8.0f) / 2.0f, applyAlpha(iconColor, alpha));

            // Category Title text
            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFE2E8F0 : 0xFF9CA3AF);
            MsdfRenderer.renderText(Fonts.medium(), name, 7.8f, applyAlpha(textColor, alpha), graphics.pose().last().pose(), itemX + 22.0f, itemY + (itemH - 7.8f * 0.72f) / 2.0f, 0.0f);

            itemY += itemH + 2.0f;
        }

        return itemY;
    }

    private void renderNavIcon(GuiGraphics graphics, String name, float x, float y, int color) {
        switch (name.toLowerCase()) {
            case "combat" -> {
                // Crossed swords
                GlassRenderUtil.fillRoundedRect(graphics, x, y, 7.0f, 1.2f, 0.6f, color);
                GlassRenderUtil.fillRoundedRect(graphics, x, y + 6.0f, 7.0f, 1.2f, 0.6f, color);
                GlassRenderUtil.fillRoundedRect(graphics, x + 3.0f, y - 1.0f, 1.2f, 9.0f, 0.6f, color);
            }
            case "movement" -> {
                // Compass / 4-way arrow
                GlassRenderUtil.fillRoundedRect(graphics, x + 3.0f, y, 1.5f, 7.5f, 0.7f, color);
                GlassRenderUtil.fillRoundedRect(graphics, x, y + 3.0f, 7.5f, 1.5f, 0.7f, color);
            }
            case "player" -> {
                // Person head and torso
                GlassRenderUtil.fillRoundedRect(graphics, x + 2.0f, y, 3.5f, 3.5f, 1.75f, color);
                GlassRenderUtil.fillRoundedRect(graphics, x, y + 4.5f, 7.5f, 3.0f, 1.0f, color);
            }
            case "visuals" -> {
                // Eye / diamond
                GlassRenderUtil.fillRoundedRect(graphics, x + 1.0f, y + 2.0f, 6.0f, 4.0f, 2.0f, color);
                GlassRenderUtil.fillRoundedRect(graphics, x + 3.0f, y + 3.0f, 2.0f, 2.0f, 1.0f, 0xFF0D0D12);
            }
            case "themes" -> {
                // Palette
                GlassRenderUtil.fillRoundedRect(graphics, x, y, 7.5f, 7.5f, 3.5f, color);
            }
            default -> {
                // Dot / bullet
                GlassRenderUtil.fillRoundedRect(graphics, x + 2.0f, y + 2.0f, 3.5f, 3.5f, 1.75f, color);
            }
        }
    }

    private void drawSpinnerRing(GuiGraphics graphics, float cx, float cy, float radius, int color) {
        float angle = (System.currentTimeMillis() % 1200L) / 1200.0f * (float) (Math.PI * 2);
        for (int i = 0; i < 6; i++) {
            float a = angle + i * 0.45f;
            float px = cx + (float) Math.cos(a) * radius;
            float py = cy + (float) Math.sin(a) * radius;
            float size = 1.0f + (i * 0.35f);
            GlassRenderUtil.fillRoundedRect(graphics, px - size / 2.0f, py - size / 2.0f, size, size, size / 2.0f, color);
        }
    }

    private void renderTopBar(GuiGraphics graphics, float mainX, float winY, float mainW, int mouseX, int mouseY, float dt, float alpha) {
        float barY = winY + 12.0f;

        // Breadcrumb icon (home/folder)
        GlassRenderUtil.fillRoundedRect(graphics, mainX, barY + 2.0f, 6.0f, 6.0f, 1.0f, applyAlpha(0xFF6B7280, alpha));

        // Breadcrumb text: ClickGui
        MsdfRenderer.renderText(Fonts.medium(), "ClickGui", 7.5f, applyAlpha(0xFF9CA3AF, alpha), graphics.pose().last().pose(), mainX + 9.0f, barY + 2.0f, 0.0f);

        // Arrow →
        MsdfRenderer.renderText(Fonts.bold(), "→", 7.5f, applyAlpha(0xFF4B5563, alpha), graphics.pose().last().pose(), mainX + 46.0f, barY + 2.0f, 0.0f);

        // Crossed swords icon for active category
        renderNavIcon(graphics, selectedSubTab, mainX + 57.0f, barY + 1.5f, applyAlpha(0xFFA78BFA, alpha));

        // Category Name (bold white)
        MsdfRenderer.renderText(Fonts.bold(), selectedSubTab, 7.5f, applyAlpha(0xFFFFFFFF, alpha), graphics.pose().last().pose(), mainX + 68.0f, barY + 2.0f, 0.0f);

        // Search Bar (Right aligned)
        float searchW = 120.0f;
        float searchH = 20.0f;
        float searchX = mainX + mainW - searchW;
        float searchY = barY - 3.0f;

        boolean searchHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH;
        int searchBg = searchFocused ? 0xFF1C1C26 : (searchHovered ? 0xFF181820 : 0xFF14141A);
        GlassRenderUtil.fillRoundedRect(graphics, searchX, searchY, searchW, searchH, 10.0f, applyAlpha(searchBg, alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) searchX, (int) searchY, (int) searchW, (int) searchH, 10, 0.6f, applyAlpha(searchFocused ? 0xFF7C3AED : 0xFF242430, alpha));

        String displayText = searchQuery.isEmpty() ? "Search..." : searchQuery;
        int queryColor = searchQuery.isEmpty() ? 0xFF6B7280 : 0xFFFFFFFF;
        MsdfRenderer.renderText(Fonts.regular(), displayText, 7.0f, applyAlpha(queryColor, alpha), graphics.pose().last().pose(), searchX + 10.0f, searchY + 6.0f, 0.0f);

        // Magnifying glass icon on right
        float magX = searchX + searchW - 14.0f;
        float magY = searchY + 6.0f;
        GlassRenderUtil.fillRoundedRect(graphics, magX, magY, 5.0f, 5.0f, 2.5f, applyAlpha(0xFF6B7280, alpha));
        GlassRenderUtil.fillRoundedRect(graphics, magX + 4.0f, magY + 4.0f, 3.5f, 1.2f, 0.6f, applyAlpha(0xFF6B7280, alpha));
    }

    private void renderContent(GuiGraphics graphics, float mainX, float contentY, float mainW, float contentH, int mouseX, int mouseY, float dt, float alpha) {
        // Filter modules by category or search query
        List<Module> visibleModules;
        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase();
            visibleModules = ModuleManager.getInstance().getModules().stream()
                    .filter(m -> m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        } else {
            Category targetCategory = switch (selectedSubTab.toLowerCase()) {
                case "combat" -> Category.COMBAT;
                case "movement" -> Category.MOVEMENT;
                case "player" -> Category.PLAYER;
                case "visuals" -> Category.RENDER;
                default -> null;
            };

            if (targetCategory != null) {
                visibleModules = ModuleManager.getInstance().getModulesByCategory(targetCategory);
            } else {
                visibleModules = ModuleManager.getInstance().getModulesByCategory(Category.HUD);
            }
        }

        // 2-Column Grid Dimensions
        float colGap = 12.0f;
        float colW = (mainW - colGap) / 2.0f;
        float col1X = mainX;
        float col2X = mainX + colW + colGap;

        // Scissor clip for smooth scrolling
        int scaleFactor = (int) Minecraft.getInstance().getWindow().getGuiScale();
        int scissorX = (int) (mainX * scaleFactor);
        int scissorY = (int) ((this.height - (contentY + contentH)) * scaleFactor);
        int scissorW = (int) (mainW * scaleFactor);
        int scissorH = (int) (contentH * scaleFactor);

        graphics.enableScissor(scissorX, scissorY, scissorX + scissorW, scissorY + scissorH);

        float col1Y = contentY + scrollY;
        float col2Y = contentY + scrollY;

        for (int i = 0; i < visibleModules.size(); i++) {
            Module module = visibleModules.get(i);
            boolean useCol1 = (col1Y <= col2Y);
            float cardX = useCol1 ? col1X : col2X;
            float cardY = useCol1 ? col1Y : col2Y;

            float cardHeight = calculateCardHeight(module);
            renderModuleCard(graphics, module, cardX, cardY, colW, cardHeight, mouseX, mouseY, dt, alpha);

            if (useCol1) {
                col1Y += cardHeight + 10.0f;
            } else {
                col2Y += cardHeight + 10.0f;
            }
        }

        graphics.disableScissor();

        // Clamp scroll range
        float maxContentH = Math.max(col1Y, col2Y) - (contentY + scrollY);
        float minScroll = Math.min(0.0f, contentH - maxContentH - 20.0f);
        targetScrollY = Math.max(minScroll, Math.min(0.0f, targetScrollY));
    }

    private float calculateCardHeight(Module module) {
        float baseH = 34.0f;
        if (!module.isExpanded() || module.getSettings().isEmpty()) {
            return baseH;
        }

        float settingsH = 6.0f;
        for (Setting<?> s : module.getSettings()) {
            settingsH += 21.0f;
        }
        // Include keybind setting row if expanded
        settingsH += 21.0f;

        return baseH + settingsH * module.getExpandProgress();
    }

    private void renderModuleCard(GuiGraphics graphics, Module module, float x, float y, float w, float h, int mouseX, int mouseY, float dt, float alpha) {
        // Card Background (Obsidian #131317 with 12px radius)
        GlassRenderUtil.fillRoundedRect(graphics, x, y, w, h, 12.0f, applyAlpha(0xFF131317, alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) x, (int) y, (int) w, (int) h, 12, 0.6f, applyAlpha(0xFF1C1C22, alpha));

        // 1. Keybind Badge on Left
        float badgeX = x + 10.0f;
        float badgeY = y + 8.0f;
        float badgeSize = 18.0f;
        boolean isBindingThis = (bindingModule == module);
        int badgeBg = isBindingThis ? 0xFF8B5CF6 : (module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN ? 0xFF2A2338 : 0xFF202028);
        GlassRenderUtil.fillRoundedRect(graphics, badgeX, badgeY, badgeSize, badgeSize, 4.0f, applyAlpha(badgeBg, alpha));

        String keyText = isBindingThis ? "..." : (module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN ? getKeyInitial(module.getKeybind()) : "—");
        int keyColor = isBindingThis ? 0xFFFFFFFF : (module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN ? 0xFFA78BFA : 0xFF6B7280);
        MsdfRenderer.renderCenteredText(Fonts.bold(), keyText, 7.5f, applyAlpha(keyColor, alpha), graphics.pose().last().pose(), badgeX + badgeSize / 2.0f, badgeY + 5.0f, 0.0f);

        // 2. Module Name (bold white)
        MsdfRenderer.renderText(Fonts.bold(), module.getName(), 8.8f, applyAlpha(0xFFFFFFFF, alpha), graphics.pose().last().pose(), x + 34.0f, y + 13.0f, 0.0f);

        // 3. Settings Gear Icon
        float gearX = x + w - 46.0f;
        float gearY = y + 11.0f;
        boolean gearHovered = mouseX >= gearX - 3.0f && mouseX <= gearX + 15.0f && mouseY >= gearY - 3.0f && mouseY <= gearY + 15.0f;

        float gearHover = moduleGearHoverMap.getOrDefault(module, 0.0f);
        gearHover += ((gearHovered || module.isExpanded() ? 1.0f : 0.0f) - gearHover) * (1.0f - (float) Math.exp(-dt * 16.0f));
        moduleGearHoverMap.put(module, gearHover);

        int gearColor = GlassRenderUtil.lerpColor(0xFF6B7280, 0xFFA78BFA, gearHover);
        drawGearIcon(graphics, gearX + 6.0f, gearY + 6.0f, 5.0f, applyAlpha(gearColor, alpha));

        // 4. iOS Toggle Switch (Purple #8B5CF6 when ON, Dark Gray #262630 when OFF)
        float switchW = 24.0f;
        float switchH = 13.0f;
        float switchX = x + w - 28.0f;
        float switchY = y + 10.5f;

        float toggleProg = moduleToggleMap.getOrDefault(module, module.isEnabled() ? 1.0f : 0.0f);
        toggleProg += (((module.isEnabled() ? 1.0f : 0.0f) - toggleProg) * (1.0f - (float) Math.exp(-dt * 18.0f)));
        moduleToggleMap.put(module, toggleProg);

        int trackColor = GlassRenderUtil.lerpColor(0xFF262630, 0xFF8B5CF6, toggleProg);
        GlassRenderUtil.fillRoundedRect(graphics, switchX, switchY, switchW, switchH, 6.5f, applyAlpha(trackColor, alpha));

        // Switch circular thumb (animates smoothly between left and right)
        float thumbSize = 9.0f;
        float thumbX = switchX + 2.0f + toggleProg * (switchW - thumbSize - 4.0f);
        float thumbY = switchY + (switchH - thumbSize) / 2.0f;
        int thumbColor = GlassRenderUtil.lerpColor(0xFF71717A, 0xFFFFFFFF, toggleProg);
        GlassRenderUtil.fillRoundedRect(graphics, thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2.0f, applyAlpha(thumbColor, alpha));

        // 5. Settings Drawer (Revealed smoothly)
        if (module.getExpandProgress() > 0.01f) {
            float setAlpha = alpha * module.getExpandProgress();
            float settingY = y + 34.0f;

            // Thin subtle divider line
            GlassRenderUtil.fillRoundedRect(graphics, x + 10.0f, settingY, w - 20.0f, 1.0f, 0.5f, applyAlpha(0xFF1E1E26, setAlpha));
            settingY += 5.0f;

            // Render all settings
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof NumberSetting num) {
                    renderSliderSetting(graphics, num, x, settingY, w, mouseX, mouseY, dt, setAlpha);
                } else if (setting instanceof ModeSetting mode) {
                    renderModeSetting(graphics, mode, x, settingY, w, mouseX, mouseY, dt, setAlpha);
                } else if (setting instanceof BooleanSetting bool) {
                    renderCheckboxSetting(graphics, bool, x, settingY, w, mouseX, mouseY, dt, setAlpha);
                } else if (setting instanceof ColorSetting col) {
                    renderColorSettingRow(graphics, col, x, settingY, w, mouseX, mouseY, dt, setAlpha);
                }
                settingY += 21.0f;
            }

            // Keybind Row inside drawer
            renderKeybindSettingRow(graphics, module, x, settingY, w, mouseX, mouseY, dt, setAlpha);
        }
    }

    private void renderSliderSetting(GuiGraphics graphics, NumberSetting slider, float x, float y, float w, int mouseX, int mouseY, float dt, float alpha) {
        // Label on left
        MsdfRenderer.renderText(Fonts.medium(), slider.getName(), 7.0f, applyAlpha(0xFF9CA3AF, alpha), graphics.pose().last().pose(), x + 12.0f, y + 6.0f, 0.0f);

        // Value text on far right (e.g. "4.5" in purple #9372FF)
        String valStr = String.format("%.1f", slider.getValue());
        if (slider.getStep() >= 1.0) {
            valStr = String.format("%d", slider.getValue().intValue());
        }
        MsdfRenderer.renderText(Fonts.bold(), valStr, 7.0f, applyAlpha(0xFF9372FF, alpha), graphics.pose().last().pose(), x + w - 24.0f, y + 6.0f, 0.0f);

        // Slider track
        float trackW = 50.0f;
        float trackH = 4.0f;
        float trackX = x + w - 82.0f;
        float trackY = y + 8.5f;

        // Background track
        GlassRenderUtil.fillRoundedRect(graphics, trackX, trackY, trackW, trackH, 2.0f, applyAlpha(0xFF22222A, alpha));

        // Active purple fill
        float progress = slider.getSliderProgress();
        float fillW = Math.max(4.0f, trackW * progress);
        GlassRenderUtil.fillGradientRoundedRect(graphics, trackX, trackY, fillW, trackH, 2.0f, applyAlpha(0xFF8B5CF6, alpha), applyAlpha(0xFFA78BFA, alpha));

        // Pill thumb
        float thumbW = 7.0f;
        float thumbH = 6.0f;
        float thumbX = trackX + progress * (trackW - thumbW);
        float thumbY = trackY + (trackH - thumbH) / 2.0f;
        GlassRenderUtil.fillRoundedRect(graphics, thumbX, thumbY, thumbW, thumbH, 3.0f, applyAlpha(0xFFFFFFFF, alpha));

        // Dragging handling
        if (draggingSlider == slider) {
            float mouseP = (mouseX - trackX) / trackW;
            slider.setFromProgress(mouseP);
        }
    }

    private void renderModeSetting(GuiGraphics graphics, ModeSetting mode, float x, float y, float w, int mouseX, int mouseY, float dt, float alpha) {
        // Label on left
        MsdfRenderer.renderText(Fonts.medium(), mode.getName(), 7.0f, applyAlpha(0xFF9CA3AF, alpha), graphics.pose().last().pose(), x + 12.0f, y + 6.0f, 0.0f);

        // Dropdown pill on right (e.g. "Mode1 , Mode2 ↕")
        String text = mode.getValue() + " ↕";
        float textW = Fonts.medium().getWidth(text, 6.8f);
        float pillW = Math.max(54.0f, textW + 12.0f);
        float pillH = 15.0f;
        float pillX = x + w - pillW - 10.0f;
        float pillY = y + 3.0f;

        boolean hovered = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
        int pillBg = hovered ? 0xFF282834 : 0xFF1E1E26;
        GlassRenderUtil.fillRoundedRect(graphics, pillX, pillY, pillW, pillH, 4.0f, applyAlpha(pillBg, alpha));

        MsdfRenderer.renderCenteredText(Fonts.medium(), text, 6.8f, applyAlpha(0xFFD1D5DB, alpha), graphics.pose().last().pose(), pillX + pillW / 2.0f, pillY + 4.5f, 0.0f);
    }

    private void renderCheckboxSetting(GuiGraphics graphics, BooleanSetting bool, float x, float y, float w, int mouseX, int mouseY, float dt, float alpha) {
        // Label on left
        MsdfRenderer.renderText(Fonts.medium(), bool.getName(), 7.0f, applyAlpha(0xFF9CA3AF, alpha), graphics.pose().last().pose(), x + 12.0f, y + 6.0f, 0.0f);

        // Mini iOS toggle switch on right
        float switchW = 20.0f;
        float switchH = 11.0f;
        float switchX = x + w - switchW - 10.0f;
        float switchY = y + 5.0f;

        float toggleProg = boolToggleMap.getOrDefault(bool, bool.getValue() ? 1.0f : 0.0f);
        toggleProg += (((bool.getValue() ? 1.0f : 0.0f) - toggleProg) * (1.0f - (float) Math.exp(-dt * 18.0f)));
        boolToggleMap.put(bool, toggleProg);

        int trackColor = GlassRenderUtil.lerpColor(0xFF262630, 0xFF8B5CF6, toggleProg);
        GlassRenderUtil.fillRoundedRect(graphics, switchX, switchY, switchW, switchH, 5.5f, applyAlpha(trackColor, alpha));

        float thumbSize = 7.0f;
        float thumbX = switchX + 2.0f + toggleProg * (switchW - thumbSize - 4.0f);
        float thumbY = switchY + (switchH - thumbSize) / 2.0f;
        GlassRenderUtil.fillRoundedRect(graphics, thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2.0f, applyAlpha(0xFFFFFFFF, alpha));
    }

    private void renderColorSettingRow(GuiGraphics graphics, ColorSetting col, float x, float y, float w, int mouseX, int mouseY, float dt, float alpha) {
        MsdfRenderer.renderText(Fonts.medium(), col.getName(), 7.0f, applyAlpha(0xFF9CA3AF, alpha), graphics.pose().last().pose(), x + 12.0f, y + 6.0f, 0.0f);

        // Hex pill badge (e.g. #9372FF)
        float pillW = 44.0f;
        float pillH = 15.0f;
        float pillX = x + w - pillW - 10.0f;
        float pillY = y + 3.0f;

        GlassRenderUtil.fillRoundedRect(graphics, pillX, pillY, pillW, pillH, 4.0f, applyAlpha(col.getValue(), alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) pillX, (int) pillY, (int) pillW, (int) pillH, 4, 0.6f, applyAlpha(0xFFFFFFFF, alpha * 0.4f));

        MsdfRenderer.renderCenteredText(Fonts.bold(), col.getHex(), 6.5f, applyAlpha(0xFFFFFFFF, alpha), graphics.pose().last().pose(), pillX + pillW / 2.0f, pillY + 4.5f, 0.0f);
    }

    private void renderKeybindSettingRow(GuiGraphics graphics, Module module, float x, float y, float w, int mouseX, int mouseY, float dt, float alpha) {
        MsdfRenderer.renderText(Fonts.medium(), "Keybind", 7.0f, applyAlpha(0xFF9CA3AF, alpha), graphics.pose().last().pose(), x + 12.0f, y + 6.0f, 0.0f);

        boolean isBinding = (bindingModule == module);
        String name = isBinding ? "Listening..." : (module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN ? getKeyName(module.getKeybind()) : "None");

        float textW = Fonts.medium().getWidth(name, 6.8f);
        float pillW = Math.max(38.0f, textW + 12.0f);
        float pillH = 15.0f;
        float pillX = x + w - pillW - 10.0f;
        float pillY = y + 3.0f;

        int pillBg = isBinding ? 0xFF8B5CF6 : (module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN ? 0xFF352B4E : 0xFF1E1E26);
        GlassRenderUtil.fillRoundedRect(graphics, pillX, pillY, pillW, pillH, 4.0f, applyAlpha(pillBg, alpha));

        int textColor = isBinding ? 0xFFFFFFFF : (module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN ? 0xFFA78BFA : 0xFF6B7280);
        MsdfRenderer.renderCenteredText(Fonts.medium(), name, 6.8f, applyAlpha(textColor, alpha), graphics.pose().last().pose(), pillX + pillW / 2.0f, pillY + 4.5f, 0.0f);
    }

    private void renderColorPickerPopup(GuiGraphics graphics, int mouseX, int mouseY, float dt, float alpha) {
        float cpW = 176.0f;
        float cpH = 142.0f;
        float cpX = colorPickerX;
        float cpY = colorPickerY;

        // Keep inside screen
        cpX = Math.max(10.0f, Math.min(this.width - cpW - 10.0f, cpX));
        cpY = Math.max(10.0f, Math.min(this.height - cpH - 10.0f, cpY));

        // Floating Card Container (#131317, 14px radius, sleek border)
        GlassRenderUtil.fillRoundedRect(graphics, cpX, cpY, cpW, cpH, 14.0f, applyAlpha(0xFF131317, alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) cpX, (int) cpY, (int) cpW, (int) cpH, 14, 0.8f, applyAlpha(0xFF282836, alpha));

        // 1. Saturation / Value Gradient Box
        float svX = cpX + 10.0f;
        float svY = cpY + 10.0f;
        float svW = cpW - 20.0f;
        float svH = 92.0f;

        int baseHueColor = Color.HSBtoRGB(pickerHue, 1.0f, 1.0f);
        GlassRenderUtil.fillRoundedRect(graphics, svX, svY, svW, svH, 8.0f, applyAlpha(baseHueColor, alpha));

        // Horizontal white fade (saturation) + vertical black fade (brightness)
        GlassRenderUtil.fillGradientRoundedRect(graphics, svX, svY, svW, svH, 8.0f, applyAlpha(0x00FFFFFF, 0.0f), applyAlpha(0xFF000000, alpha));

        // Picker Ring Thumb
        float thumbX = svX + pickerSat * svW;
        float thumbY = svY + (1.0f - pickerVal) * svH;
        GlassRenderUtil.fillRoundedRect(graphics, thumbX - 3.5f, thumbY - 3.5f, 7.0f, 7.0f, 3.5f, applyAlpha(0xFFFFFFFF, alpha));
        GlassRenderUtil.fillRoundedRect(graphics, thumbX - 2.0f, thumbY - 2.0f, 4.0f, 4.0f, 2.0f, applyAlpha(activeColorPickerSetting.getValue(), alpha));

        // 2. Hue Rainbow Slider Bar
        float hueX = cpX + 10.0f;
        float hueY = cpY + 112.0f;
        float hueW = cpW - 20.0f;
        float hueH = 8.0f;

        // Draw segmented rainbow bar
        int segments = 12;
        float segW = hueW / segments;
        for (int i = 0; i < segments; i++) {
            float h1 = (float) i / segments;
            float h2 = (float) (i + 1) / segments;
            int c1 = Color.HSBtoRGB(h1, 1.0f, 1.0f);
            int c2 = Color.HSBtoRGB(h2, 1.0f, 1.0f);
            GlassRenderUtil.fillRoundedRect(graphics, hueX + i * segW, hueY, segW + 0.5f, hueH, 4.0f, applyAlpha(c1, alpha));
        }

        // Hue Ring Thumb
        float hueThumbX = hueX + pickerHue * hueW;
        GlassRenderUtil.fillRoundedRect(graphics, hueThumbX - 3.0f, hueY - 1.0f, 6.0f, 10.0f, 3.0f, applyAlpha(0xFFFFFFFF, alpha));

        // Interactive dragging
        if (isDraggingSV) {
            pickerSat = Math.max(0.0f, Math.min(1.0f, (mouseX - svX) / svW));
            pickerVal = Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - svY) / svH));
            updatePickedColor();
        } else if (isDraggingHue) {
            pickerHue = Math.max(0.0f, Math.min(1.0f, (mouseX - hueX) / hueW));
            updatePickedColor();
        }
    }

    private void updatePickedColor() {
        if (activeColorPickerSetting != null) {
            int rgb = Color.HSBtoRGB(pickerHue, pickerSat, pickerVal);
            activeColorPickerSetting.setValue(0xFF000000 | (rgb & 0x00FFFFFF));
        }
    }

    private void drawGearIcon(GuiGraphics graphics, float cx, float cy, float r, int color) {
        GlassRenderUtil.fillRoundedRect(graphics, cx - r, cy - 1.0f, r * 2.0f, 2.0f, 1.0f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx - 1.0f, cy - r, 2.0f, r * 2.0f, 1.0f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx - r * 0.7f, cy - r * 0.7f, r * 1.4f, 2.0f, 1.0f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx - r * 0.7f, cy + r * 0.7f - 2.0f, r * 1.4f, 2.0f, 1.0f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx - 1.5f, cy - 1.5f, 3.0f, 3.0f, 1.5f, 0xFF131317);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float winX = (this.width - WIN_WIDTH) / 2.0f;
        float winY = (this.height - WIN_HEIGHT) / 2.0f;

        // 1. Color Picker Popup interaction
        if (activeColorPickerSetting != null) {
            float cpW = 176.0f;
            float cpH = 142.0f;
            float cpX = colorPickerX;
            float cpY = colorPickerY;
            boolean insidePopup = mouseX >= cpX && mouseX <= cpX + cpW && mouseY >= cpY && mouseY <= cpY + cpH;

            if (insidePopup) {
                float svX = cpX + 10.0f;
                float svY = cpY + 10.0f;
                float svW = cpW - 20.0f;
                float svH = 92.0f;

                float hueX = cpX + 10.0f;
                float hueY = cpY + 112.0f;
                float hueW = cpW - 20.0f;
                float hueH = 8.0f;

                if (mouseX >= svX && mouseX <= svX + svW && mouseY >= svY && mouseY <= svY + svH) {
                    isDraggingSV = true;
                    pickerSat = Math.max(0.0f, Math.min(1.0f, (float) (mouseX - svX) / svW));
                    pickerVal = Math.max(0.0f, Math.min(1.0f, 1.0f - (float) (mouseY - svY) / svH));
                    updatePickedColor();
                    return true;
                } else if (mouseX >= hueX && mouseX <= hueX + hueW && mouseY >= hueY && mouseY <= hueY + hueH) {
                    isDraggingHue = true;
                    pickerHue = Math.max(0.0f, Math.min(1.0f, (float) (mouseX - hueX) / hueW));
                    updatePickedColor();
                    return true;
                }
                return true;
            } else {
                activeColorPickerSetting = null;
                return true;
            }
        }

        // 2. Sidebar Navigation Clicking
        float curY = winY + 52.0f;
        String[] allTabs = {"Combat", "Movement", "Player", "Visuals", "Themes", "Configs", "Favorites", "Friends"};
        float itemW = SIDEBAR_WIDTH - 24.0f;
        float itemX = winX + 12.0f;
        float itemH = 22.0f;

        // Check group 1
        float g1Y = curY + 11.0f;
        for (int i = 0; i < 4; i++) {
            if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= g1Y && mouseY <= g1Y + itemH) {
                selectedSubTab = allTabs[i];
                searchQuery = "";
                targetScrollY = 0.0f;
                playClick(1.05f);
                return true;
            }
            g1Y += itemH + 2.0f;
        }

        // Check group 2
        float g2Y = g1Y + 17.0f;
        for (int i = 4; i < 6; i++) {
            if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= g2Y && mouseY <= g2Y + itemH) {
                selectedSubTab = allTabs[i];
                searchQuery = "";
                targetScrollY = 0.0f;
                playClick(1.05f);
                return true;
            }
            g2Y += itemH + 2.0f;
        }

        // Check group 3
        float g3Y = g2Y + 17.0f;
        for (int i = 6; i < 8; i++) {
            if (mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= g3Y && mouseY <= g3Y + itemH) {
                selectedSubTab = allTabs[i];
                searchQuery = "";
                targetScrollY = 0.0f;
                playClick(1.05f);
                return true;
            }
            g3Y += itemH + 2.0f;
        }

        // 3. Search Bar click
        float mainX = winX + SIDEBAR_WIDTH + 14.0f;
        float mainW = (winX + WIN_WIDTH) - mainX - 14.0f;
        float searchW = 120.0f;
        float searchH = 20.0f;
        float searchX = mainX + mainW - searchW;
        float searchY = winY + 9.0f;

        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // 4. Content Area Module Clicks
        float contentY = winY + 38.0f;
        float colGap = 12.0f;
        float colW = (mainW - colGap) / 2.0f;
        float col1X = mainX;
        float col2X = mainX + colW + colGap;

        List<Module> visibleModules = getVisibleModules();
        float col1Y = contentY + scrollY;
        float col2Y = contentY + scrollY;

        for (int i = 0; i < visibleModules.size(); i++) {
            Module module = visibleModules.get(i);
            boolean useCol1 = (col1Y <= col2Y);
            float cardX = useCol1 ? col1X : col2X;
            float cardY = useCol1 ? col1Y : col2Y;
            float cardH = calculateCardHeight(module);

            // Click inside card
            if (mouseX >= cardX && mouseX <= cardX + colW && mouseY >= cardY && mouseY <= cardY + cardH) {
                // Header interactions (height 34)
                if (mouseY <= cardY + 34.0f) {
                    // Keybind badge click
                    if (mouseX >= cardX + 10.0f && mouseX <= cardX + 28.0f) {
                        bindingModule = (bindingModule == module) ? null : module;
                        playClick(1.1f);
                        return true;
                    }

                    // Gear icon click
                    float gearX = cardX + colW - 46.0f;
                    if (mouseX >= gearX - 3.0f && mouseX <= gearX + 15.0f) {
                        module.setExpanded(!module.isExpanded());
                        playClick(module.isExpanded() ? 1.15f : 0.95f);
                        return true;
                    }

                    // Toggle switch click
                    float switchX = cardX + colW - 28.0f;
                    if (mouseX >= switchX - 2.0f && mouseX <= switchX + 26.0f) {
                        module.toggle();
                        return true;
                    }

                    // Clicking card title also toggles
                    module.toggle();
                    return true;
                } else if (module.isExpanded()) {
                    // Settings Drawer interactions
                    float setY = cardY + 39.0f;
                    for (Setting<?> setting : module.getSettings()) {
                        if (mouseY >= setY && mouseY <= setY + 21.0f) {
                            if (setting instanceof NumberSetting num) {
                                float trackX = cardX + colW - 82.0f;
                                float trackW = 50.0f;
                                if (mouseX >= trackX - 6.0f && mouseX <= trackX + trackW + 6.0f) {
                                    draggingSlider = num;
                                    float mouseP = (float) (mouseX - trackX) / trackW;
                                    num.setFromProgress(mouseP);
                                    playClick(1.2f);
                                    return true;
                                }
                            } else if (setting instanceof ModeSetting mode) {
                                mode.cycle();
                                playClick(1.05f);
                                return true;
                            } else if (setting instanceof BooleanSetting bool) {
                                bool.setValue(!bool.getValue());
                                ClientSounds.play(ClientSounds.TOGGLE, bool.getValue() ? 1.05f : 0.88f, 0.7f);
                                return true;
                            } else if (setting instanceof ColorSetting col) {
                                activeColorPickerSetting = col;
                                colorPickerX = (float) mouseX - 88.0f;
                                colorPickerY = (float) mouseY + 10.0f;
                                float[] hsb = Color.RGBtoHSB(col.getRed(), col.getGreen(), col.getBlue(), null);
                                pickerHue = hsb[0];
                                pickerSat = hsb[1];
                                pickerVal = hsb[2];
                                playClick(1.1f);
                                return true;
                            }
                        }
                        setY += 21.0f;
                    }

                    // Keybind row click
                    if (mouseY >= setY && mouseY <= setY + 21.0f) {
                        bindingModule = (bindingModule == module) ? null : module;
                        playClick(1.1f);
                        return true;
                    }
                }
                return true;
            }

            if (useCol1) {
                col1Y += cardH + 10.0f;
            } else {
                col2Y += cardH + 10.0f;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = null;
        isDraggingSV = false;
        isDraggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScrollY += (float) (verticalAmount * 26.0f);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            Module mod = bindingModule;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                mod.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
                ClientSounds.play(ClientSounds.CRITICAL, 1.0f, 0.8f);
            } else {
                mod.setKeybind(keyCode);
                ClientSounds.play(ClientSounds.APPLEPAY, 1.0f, 0.85f);
            }
            bindingModule = null;
            ModuleManager.getInstance().saveConfig();
            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    ClientSounds.play(ClientSounds.TYPING, 0.88f, 0.6f);
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            closeSmoothly();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && Character.isDefined(codePoint) && codePoint >= 32) {
            if (searchQuery.length() < 24) {
                searchQuery += codePoint;
                ClientSounds.play(ClientSounds.TYPING, 0.95f + (float) (Math.random() * 0.15), 0.6f);
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private List<Module> getVisibleModules() {
        if (!searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase();
            return ModuleManager.getInstance().getModules().stream()
                    .filter(m -> m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        Category targetCategory = switch (selectedSubTab.toLowerCase()) {
            case "combat" -> Category.COMBAT;
            case "movement" -> Category.MOVEMENT;
            case "player" -> Category.PLAYER;
            case "visuals" -> Category.RENDER;
            default -> null;
        };

        if (targetCategory != null) {
            return ModuleManager.getInstance().getModulesByCategory(targetCategory);
        }
        return ModuleManager.getInstance().getModulesByCategory(Category.HUD);
    }

    private void closeSmoothly() {
        if (!isClosing) {
            isClosing = true;
            closeProgress = openProgress;
            playClick(0.9f);
        }
    }

    private void actuallyClose() {
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        closeSmoothly();
    }

    private void playClick(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch)
        );
    }

    private static String getKeyInitial(int keyCode) {
        String name = getKeyName(keyCode);
        if (name.length() > 3) {
            return name.substring(0, 1);
        }
        return name;
    }

    private static String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "None";
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Back";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            default -> {
                String name = GLFW.glfwGetKeyName(keyCode, 0);
                if (name != null && !name.isEmpty()) {
                    yield name.toUpperCase();
                }
                yield "K" + keyCode;
            }
        };
    }

    private static int applyAlpha(int color, float alphaFactor) {
        alphaFactor = Math.max(0.0f, Math.min(1.0f, alphaFactor));
        int a = (int) (((color >> 24) & 0xFF) * alphaFactor);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
