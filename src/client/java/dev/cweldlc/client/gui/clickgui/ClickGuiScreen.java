package dev.cweldlc.client.gui.clickgui;

import com.mojang.math.Axis;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.media.MediaManager;
import dev.cweldlc.client.module.Category;
import dev.cweldlc.client.module.Module;
import dev.cweldlc.client.module.ModuleManager;
import dev.cweldlc.client.module.setting.BooleanSetting;
import dev.cweldlc.client.module.setting.ModeSetting;
import dev.cweldlc.client.module.setting.NumberSetting;
import dev.cweldlc.client.module.setting.Setting;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClickGuiScreen extends Screen {

    private final Screen parent;
    private Category currentCategory = Category.COMBAT;
    private Module bindingModule = null;
    private NumberSetting draggingSlider = null;

    private static final float WIN_WIDTH = 550.0f;
    private static final float WIN_HEIGHT = 330.0f;
    private static final float SIDEBAR_WIDTH = 112.0f;

    // Icons
    private static final ResourceLocation PLAY_ICON = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/play.png");
    private static final ResourceLocation PAUSE_ICON = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/pause.png");
    private static final ResourceLocation SKIP_BACK_ICON = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/skip_back.png");
    private static final ResourceLocation SKIP_FORWARD_ICON = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/skip_forward.png");
    private static final ResourceLocation SEARCH_ICON = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/icons/category/search.png");

    // Opening & Closing Animation Engine
    private float openProgress = 0.0f;
    private boolean isClosing = false;
    private float closeProgress = 1.0f;
    private long lastTime = System.currentTimeMillis();

    // Category Sliding Pill Animation (Vertical on Sidebar)
    private float indicatorY = -1.0f;

    // Scrolling
    private float scrollY = 0.0f;
    private float targetScrollY = 0.0f;

    // Module Hover Animation Tracker
    private final Map<Module, Float> moduleHoverMap = new HashMap<>();

    // Category Hover Animation Tracker
    private final Map<Category, Float> catHoverMap = new HashMap<>();

    // Settings Animation Trackers
    private final Map<Setting<?>, Float> settingHoverMap = new HashMap<>();
    private final Map<BooleanSetting, Float> boolToggleMap = new HashMap<>();
    private final Map<NumberSetting, Float> sliderProgressMap = new HashMap<>();
    private final Map<NumberSetting, Float> sliderHoverMap = new HashMap<>();
    private final Map<ModeSetting, Float> modeBounceMap = new HashMap<>();

    // Keybind Animation Trackers (Pulse, Morph Width, Success Flash)
    private final Map<Module, Float> bindProgressMap = new HashMap<>();
    private final Map<Module, Float> bindWidthMap = new HashMap<>();
    private final Map<Module, Float> bindFlashMap = new HashMap<>();

    // Live Search Engine
    private String searchQuery = "";
    private boolean searchFocused = false;
    private float searchBoxWidth = 58.0f;

    // Media Player Card Engine & Fluid Animations
    private boolean showMediaPlayer = true;
    private float mediaAnimProgress = 1.0f;
    private boolean isDraggingScrubber = false;

    // Transport buttons hover & click bounce animations
    private float prevHoverAnim = 0.0f;
    private float playHoverAnim = 0.0f;
    private float nextHoverAnim = 0.0f;
    private float skip10PrevHoverAnim = 0.0f;
    private float skip10NextHoverAnim = 0.0f;

    private float prevBounce = 0.0f;
    private float playBounce = 0.0f;
    private float nextBounce = 0.0f;
    private float skip10PrevBounce = 0.0f;
    private float skip10NextBounce = 0.0f;

    // Scrub bar animations
    private float smoothProgress = 0.0f;
    private float scrubHoverAnim = 0.0f;
    private float rewindWaveAnim = 0.0f;
    private float forwardWaveAnim = 0.0f;

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
        dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.CLICKGUI_OPEN, 1.0f, 0.85f);
        lastTime = System.currentTimeMillis();
        openProgress = 0.0f;
        isClosing = false;
        closeProgress = 1.0f;
        indicatorY = -1.0f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Intentionally custom rendered in render()
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.05f, (now - lastTime) / 1000.0f);
        lastTime = now;

        // Media & Animations Engine Update
        MediaManager.getInstance().update(dt);
        mediaAnimProgress += ((showMediaPlayer ? 1.0f : 0.0f) - mediaAnimProgress) * (1.0f - (float) Math.exp(-dt * 14.0f));
        float targetSearchW = searchFocused ? 90.0f : (searchQuery.isEmpty() ? 56.0f : 80.0f);
        searchBoxWidth += (targetSearchW - searchBoxWidth) * (1.0f - (float) Math.exp(-dt * 18.0f));

        // Transport button spring bounces decay
        prevBounce = Math.max(0.0f, prevBounce - dt * 4.8f);
        playBounce = Math.max(0.0f, playBounce - dt * 4.8f);
        nextBounce = Math.max(0.0f, nextBounce - dt * 4.8f);
        skip10PrevBounce = Math.max(0.0f, skip10PrevBounce - dt * 4.8f);
        skip10NextBounce = Math.max(0.0f, skip10NextBounce - dt * 4.8f);

        // Progress bar rewind & forward wave animations decay
        rewindWaveAnim = Math.max(0.0f, rewindWaveAnim - dt * 2.2f);
        forwardWaveAnim = Math.max(0.0f, forwardWaveAnim - dt * 2.2f);

        // Fluid smooth progress gliding
        float targetProg = MediaManager.getInstance().getProgress();
        if (isDraggingScrubber) {
            smoothProgress = targetProg;
        } else {
            float speed = Math.abs(targetProg - smoothProgress) > 0.08f ? 24.0f : 12.0f;
            smoothProgress += (targetProg - smoothProgress) * (1.0f - (float) Math.exp(-dt * speed));
        }

        // 1. Update Opening & Closing Animations
        if (!isClosing) {
            openProgress += (1.0f - openProgress) * (1.0f - (float) Math.exp(-dt * 14.0f));
        } else {
            closeProgress += (0.0f - closeProgress) * (1.0f - (float) Math.exp(-dt * 16.0f));
            if (closeProgress <= 0.02f) {
                actuallyClose();
                return;
            }
        }

        scrollY += (targetScrollY - scrollY) * (1.0f - (float) Math.exp(-dt * 12.0f));

        float animProgress = isClosing ? closeProgress : openProgress;
        float currentAlpha = Math.max(0.0f, Math.min(1.0f, animProgress));

        // 2. Cinematic backdrop blur/dim
        int dimAlpha = (int) (currentAlpha * 0x6E);
        graphics.fill(0, 0, this.width, this.height, dimAlpha << 24);

        float winX = (this.width - WIN_WIDTH) / 2.0f;
        float winY = (this.height - WIN_HEIGHT) / 2.0f;
        float centerX = this.width / 2.0f;
        float centerY = this.height / 2.0f;

        // 3. Apple Spring Scale (0.88 -> 1.0 on open, 1.0 -> 0.88 on close)
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0f);
        float scale = 0.88f + 0.12f * animProgress;
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-centerX, -centerY, 0.0f);

        // 4. Main Apple Liquid Glass Window Card (Radius 16)
        GlassRenderUtil.drawGlassPanel(
                graphics,
                (int) winX,
                (int) winY,
                (int) WIN_WIDTH,
                (int) WIN_HEIGHT,
                16,
                false,
                0.0f
        );

        // Smoothly update module animations each frame
        for (Module m : ModuleManager.getInstance().getModules()) {
            m.updateAnimations(dt);
        }

        float divX = winX + SIDEBAR_WIDTH + 2.0f;
        float mainX = divX + 12.0f;
        float mainW = (winX + WIN_WIDTH) - mainX - 12.0f;

        // 5. Left Sidebar: Branding + Vertical Category Tabs
        renderSidebar(graphics, winX, winY, mouseX, mouseY, dt, currentAlpha);

        // 6. Main Area Header: Current Category Title + Stats + Live Search + Quick Controls
        renderMainHeader(graphics, mainX, winY, mainW, mouseX, mouseY, dt, currentAlpha);

        // 7. Content Area: Modules Grid with smooth drawer expand animations
        renderModules(graphics, mainX, winY, mainW, mouseX, mouseY, dt, currentAlpha);

        // 7. Floating Apple Music Media Player Card
        if (mediaAnimProgress > 0.01f) {
            renderMediaPlayer(graphics, winX, winY, mouseX, mouseY, dt, currentAlpha * mediaAnimProgress);
        }

        graphics.pose().popPose();

        super.render(graphics, mouseX, mouseY, delta);
    }


    private void drawTintedIcon(GuiGraphics graphics, ResourceLocation loc, float x, float y, float size, int color) {
        try {
            Minecraft.getInstance().getTextureManager().getTexture(loc).setFilter(true, false);
        } catch (Exception ignored) {}
        graphics.blit(
                RenderType::guiTextured,
                loc,
                (int) x,
                (int) y,
                0.0f,
                0.0f,
                (int) size,
                (int) size,
                128,
                128,
                128,
                128,
                color
        );
    }

    private void renderMusicIcon(GuiGraphics graphics, float cx, float cy, float size, int color) {
        float x = cx - size / 2.0f;
        float y = cy - size / 2.0f;
        // Vector double beamed music note ♫
        // Left note head
        GlassRenderUtil.fillRoundedRect(graphics, x, y + size - 3.5f, 3.2f, 2.8f, 1.4f, color);
        // Right note head
        GlassRenderUtil.fillRoundedRect(graphics, x + size - 3.2f, y + size - 4.5f, 3.2f, 2.8f, 1.4f, color);
        // Left stem
        GlassRenderUtil.fillRoundedRect(graphics, x + 2.2f, y + 1.0f, 1.1f, size - 4.0f, 0.5f, color);
        // Right stem
        GlassRenderUtil.fillRoundedRect(graphics, x + size - 1.0f, y, 1.1f, size - 4.0f, 0.5f, color);
        // Top connecting beam
        GlassRenderUtil.fillRoundedRect(graphics, x + 2.2f, y, size - 2.2f, 1.8f, 0.6f, color);
    }

    private void drawVectorChevron(GuiGraphics graphics, float cx, float cy, int color) {
        // A crisp 7px wide modern downward chevron
        GlassRenderUtil.fillRoundedRect(graphics, cx - 3.0f, cy - 1.5f, 2.0f, 1.2f, 0.5f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx - 1.5f, cy - 0.5f, 2.0f, 1.2f, 0.5f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx - 0.5f, cy + 0.5f, 1.0f, 1.2f, 0.5f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx + 0.5f, cy - 0.5f, 2.0f, 1.2f, 0.5f, color);
        GlassRenderUtil.fillRoundedRect(graphics, cx + 2.0f, cy - 1.5f, 2.0f, 1.2f, 0.5f, color);
    }

    private void renderSidebar(GuiGraphics graphics, float winX, float winY, int mouseX, int mouseY, float dt, float screenAlpha) {
        float sidebarX = winX;
        float sidebarW = SIDEBAR_WIDTH;

        // Visium Branding & Category Indicator Dot
        int titleColor = applyAlpha(ThemeManager.getTitleColor(), screenAlpha);
        MsdfRenderer.renderText(
                Fonts.roundBold(),
                "Visium",
                12.5f,
                titleColor,
                graphics.pose().last().pose(),
                sidebarX + 16.0f,
                winY + 14.0f,
                0.0f
        );

        // Glowing Category Dot
        GlassRenderUtil.fillRoundedRect(
                graphics,
                sidebarX + 57.0f,
                winY + 18.0f,
                4.0f,
                4.0f,
                2.0f,
                applyAlpha(currentCategory.getAccentColor(), screenAlpha)
        );

        // Version tag
        MsdfRenderer.renderText(
                Fonts.medium(),
                "v1.0",
                6.0f,
                applyAlpha(0xFF60A5FA, screenAlpha),
                graphics.pose().last().pose(),
                sidebarX + 66.0f,
                winY + 17.5f,
                0.0f
        );

        // Horizontal divider below branding
        int divColor = ThemeManager.lerpColor(0x18FFFFFF, 0x1A000000, ThemeManager.getTransitionFactor());
        GlassRenderUtil.fillRoundedRect(graphics, sidebarX + 12.0f, winY + 34.0f, sidebarW - 20.0f, 1.0f, 0.5f, applyAlpha(divColor, screenAlpha));

        // Section label
        MsdfRenderer.renderText(
                Fonts.medium(),
                "CATEGORIES",
                5.5f,
                applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), screenAlpha),
                graphics.pose().last().pose(),
                sidebarX + 16.0f,
                winY + 43.0f,
                0.0f
        );

        // Category Vertical Stack
        float catStartY = winY + 54.0f;
        float catH = 26.0f;
        float catSpacing = 4.0f;
        float catW = sidebarW - 20.0f;
        float catX = sidebarX + 10.0f;

        Category[] categories = Category.values();
        float targetPillY = catStartY + currentCategory.ordinal() * (catH + catSpacing);

        // Initialize indicator position on first frame
        if (indicatorY < 0.0f) {
            indicatorY = targetPillY;
        } else {
            indicatorY += (targetPillY - indicatorY) * (1.0f - (float) Math.exp(-dt * 18.0f));
        }

        // Draw Sliding Glass Pill Indicator underneath active tab
        int pillBg = ThemeManager.lerpColor(0x35FFFFFF, 0x3EFFFFFF, ThemeManager.getTransitionFactor());
        GlassRenderUtil.fillRoundedRect(graphics, catX, indicatorY, catW, catH, 7, applyAlpha(pillBg, screenAlpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int)catX, (int)indicatorY, (int)catW, (int)catH, 7, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(1.0f), screenAlpha));

        float iconSize = 10.0f;
        float iconGap = 6.0f;

        for (Category cat : categories) {
            float rowY = catStartY + cat.ordinal() * (catH + catSpacing);
            boolean hovered = mouseX >= catX && mouseX <= catX + catW && mouseY >= rowY && mouseY <= rowY + catH;
            boolean selected = (cat == currentCategory);

            float catHover = catHoverMap.getOrDefault(cat, 0.0f);
            catHover += ((hovered ? 1.0f : 0.0f) - catHover) * (1.0f - (float) Math.exp(-dt * 16.0f));
            catHoverMap.put(cat, catHover);

            int textColor = selected
                    ? ThemeManager.getTitleColor()
                    : ThemeManager.getSecondaryTextColor(catHover);
            int normalIcon = ThemeManager.lerpColor(ThemeManager.getSecondaryTextColor(0.0f), ThemeManager.getTitleColor(), catHover);
            int iconColor = selected ? cat.getAccentColor() : normalIcon;

            float iconX = catX + 8.0f;
            float iconY = rowY + (catH - iconSize) / 2.0f;

            // Render category Lucide icon
            drawTintedIcon(graphics, cat.getIconLocation(), iconX, iconY, iconSize, applyAlpha(iconColor, screenAlpha));

            // Render category title text
            MsdfRenderer.renderText(
                    Fonts.medium(),
                    cat.getDisplayName(),
                    8.0f,
                    applyAlpha(textColor, screenAlpha),
                    graphics.pose().last().pose(),
                    iconX + iconSize + iconGap,
                    rowY + (catH - 8.0f * 0.72f) / 2.0f,
                    0.0f
            );

            // Active modules indicator dot
            boolean hasActive = ModuleManager.getInstance().getModulesByCategory(cat).stream().anyMatch(Module::isEnabled);
            if (hasActive) {
                GlassRenderUtil.fillRoundedRect(graphics, catX + catW - 7.0f, rowY + (catH - 3.5f) / 2.0f, 3.5f, 3.5f, 1.75f, applyAlpha(0xFF10B981, screenAlpha));
            }
        }

        // Active modules count at bottom of sidebar
        long activeCount = ModuleManager.getInstance().getModules().stream().filter(Module::isEnabled).count();
        MsdfRenderer.renderText(
                Fonts.regular(),
                activeCount + " active",
                6.0f,
                applyAlpha(0xFF6B7280, screenAlpha),
                graphics.pose().last().pose(),
                sidebarX + 16.0f,
                winY + WIN_HEIGHT - 16.0f,
                0.0f
        );

        // Vertical divider between sidebar and main area
        float divX = winX + sidebarW + 2.0f;
        GlassRenderUtil.fillRoundedRect(graphics, divX, winY + 12.0f, 1.0f, WIN_HEIGHT - 24.0f, 0.5f, applyAlpha(divColor, screenAlpha));
    }

    private void renderMainHeader(GuiGraphics graphics, float mainX, float winY, float mainW, int mouseX, int mouseY, float dt, float screenAlpha) {
        // Category Title & Subtitle
        String catTitle = currentCategory.getDisplayName();
        int titleColor = applyAlpha(ThemeManager.getTitleColor(), screenAlpha);
        MsdfRenderer.renderText(
                Fonts.roundBold(),
                catTitle,
                12.0f,
                titleColor,
                graphics.pose().last().pose(),
                mainX + 2.0f,
                winY + 12.0f,
                0.0f
        );

        float titleW = Fonts.roundBold().getWidth(catTitle, 12.0f);
        List<Module> mods = ModuleManager.getInstance().getModulesByCategory(currentCategory);
        MsdfRenderer.renderText(
                Fonts.regular(),
                mods.size() + " modules",
                6.5f,
                applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), screenAlpha),
                graphics.pose().last().pose(),
                mainX + 4.0f + titleW + 6.0f,
                winY + 16.5f,
                0.0f
        );

        // Right Header Controls: Media Player Toggle + Live Search Box
        float btnSize = 21.0f;
        float btnY = winY + 10.0f;

        // Media Player Toggle Button (♫)
        float musicBtnX = mainX + mainW - btnSize;
        boolean hoverMusic = mouseX >= musicBtnX && mouseX <= musicBtnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize;
        int musicBg = showMediaPlayer
                ? 0x403B82F6
                : (hoverMusic ? 0x2AFFFFFF : 0x16FFFFFF);
        GlassRenderUtil.fillRoundedRect(graphics, musicBtnX, btnY, btnSize, btnSize, 6.0f, applyAlpha(musicBg, screenAlpha));
        int musicIconColor = showMediaPlayer ? 0xFF93C5FD : (hoverMusic ? 0xFFFFFFFF : 0xFF9CA3AF);
        renderMusicIcon(graphics, musicBtnX + btnSize / 2.0f, btnY + btnSize / 2.0f, 10.0f, applyAlpha(musicIconColor, screenAlpha));

        // Live Search Input Box with Lucide search icon
        float searchX = musicBtnX - searchBoxWidth - 6.0f;
        boolean hoverSearch = mouseX >= searchX && mouseX <= searchX + searchBoxWidth && mouseY >= btnY && mouseY <= btnY + btnSize;
        int searchBg = (searchFocused || hoverSearch)
                ? ThemeManager.lerpColor(0x35FFFFFF, 0x26000000, ThemeManager.getTransitionFactor())
                : ThemeManager.lerpColor(0x20FFFFFF, 0x14000000, ThemeManager.getTransitionFactor());
        GlassRenderUtil.fillRoundedRect(graphics, searchX, btnY, searchBoxWidth, btnSize, 6.0f, applyAlpha(searchBg, screenAlpha));
        float sBorderAlpha = searchFocused ? 0.85f : (hoverSearch ? 0.5f : 0.2f);
        GlassRenderUtil.drawRoundedOutline(graphics, (int) searchX, (int) btnY, (int) searchBoxWidth, (int) btnSize, 6, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(sBorderAlpha), screenAlpha));

        // Lucide Magnifying Glass Icon
        float sIconSize = 10.0f;
        int sIconColor = searchFocused ? 0xFF60A5FA : ThemeManager.getSecondaryTextColor(hoverSearch ? 0.8f : 0.0f);
        drawTintedIcon(graphics, SEARCH_ICON, searchX + 6.0f, btnY + (btnSize - sIconSize) / 2.0f, sIconSize, applyAlpha(sIconColor, screenAlpha));

        // Search text / placeholder + caret
        String queryDisplay = searchQuery.isEmpty() ? (searchFocused ? "" : "Search") : searchQuery;
        if (searchFocused && ((System.currentTimeMillis() / 450) % 2 == 0)) {
            queryDisplay += "|";
        }
        int queryColor = searchQuery.isEmpty() ? ThemeManager.getSecondaryTextColor(0.0f) : ThemeManager.getTitleColor();
        MsdfRenderer.renderText(Fonts.regular(), queryDisplay, 7.0f, applyAlpha(queryColor, screenAlpha), graphics.pose().last().pose(), searchX + 19.0f, btnY + (btnSize - 7.0f * 0.72f) / 2.0f, 0.0f);

        // Clear '✕' button if query is not empty
        if (!searchQuery.isEmpty()) {
            float clearX = searchX + searchBoxWidth - 11.0f;
            boolean hoverClear = mouseX >= clearX - 3 && mouseX <= clearX + 8 && mouseY >= btnY && mouseY <= btnY + btnSize;
            int clearCol = hoverClear ? 0xFFEF4444 : ThemeManager.getSecondaryTextColor(0.0f);
            MsdfRenderer.renderText(Fonts.medium(), "✕", 6.0f, applyAlpha(clearCol, screenAlpha), graphics.pose().last().pose(), clearX, btnY + (btnSize - 6.0f * 0.72f) / 2.0f, 0.0f);
        }

        // Header glass divider line
        int divColor = ThemeManager.lerpColor(0x18FFFFFF, 0x1A000000, ThemeManager.getTransitionFactor());
        GlassRenderUtil.fillRoundedRect(graphics, mainX, winY + 34.0f, mainW, 1.0f, 0.5f, applyAlpha(divColor, screenAlpha));
    }

    private void renderModules(GuiGraphics graphics, float mainX, float winY, float mainW, int mouseX, int mouseY, float dt, float screenAlpha) {
        List<Module> modules;
        if (!searchQuery.trim().isEmpty()) {
            String q = searchQuery.trim().toLowerCase();
            modules = ModuleManager.getInstance().getModules().stream()
                    .filter(m -> m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        } else {
            modules = ModuleManager.getInstance().getModulesByCategory(currentCategory);
        }

        float contentX = mainX;
        float contentY = winY + 40.0f;
        float contentW = mainW;
        float colWidth = (contentW - 10.0f) / 2.0f; // 2 column layout
        float visibleH = WIN_HEIGHT - 48.0f;

        if (modules.isEmpty()) {
            String noModMsg = "No modules found matching \"" + searchQuery + "\"";
            MsdfRenderer.renderCenteredText(
                    Fonts.medium(),
                    noModMsg,
                    8.5f,
                    applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), screenAlpha),
                    graphics.pose().last().pose(),
                    contentX + contentW / 2.0f,
                    contentY + visibleH / 2.0f,
                    0.0f
            );
            return;
        }

        graphics.enableScissor(
                (int) Math.floor(contentX - 2.0f),
                (int) Math.floor(contentY - 2.0f),
                (int) Math.ceil(contentX + contentW + 2.0f),
                (int) Math.ceil(contentY + visibleH + 4.0f)
        );

        float[] colY = new float[]{contentY + scrollY, contentY + scrollY};

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % 2;
            float cardX = contentX + col * (colWidth + 10.0f);
            float cardY = colY[col];

            float baseCardH = 44.0f;
            float settingsH = calculateSettingsHeight(module);
            float cardH = baseCardH + settingsH * module.getEasedExpand();

            renderModuleCard(graphics, module, cardX, cardY, colWidth, cardH, baseCardH, mouseX, mouseY, dt, screenAlpha);

            colY[col] += cardH + 8.0f;
        }

        graphics.disableScissor();

        float maxContentH = Math.max(colY[0], colY[1]) - (contentY + scrollY);
        if (maxContentH > visibleH) {
            targetScrollY = Math.max(visibleH - maxContentH, Math.min(0.0f, targetScrollY));
        } else {
            targetScrollY = 0.0f;
        }
    }

    private void renderMediaPlayer(GuiGraphics graphics, float winX, float winY, int mouseX, int mouseY, float dt, float alpha) {
        MediaManager media = MediaManager.getInstance();
        float cardW = WIN_WIDTH;
        float cardH = 48.0f;
        float cardX = winX;
        float cardY = winY + WIN_HEIGHT + 8.0f;

        if (cardY + cardH > this.height - 4.0f) {
            cardY = this.height - cardH - 4.0f;
        }

        // Draw Liquid Glass Panel (Radius 12)
        GlassRenderUtil.drawGlassPanel(graphics, (int) cardX, (int) cardY, (int) cardW, (int) cardH, 12, false, 0.0f);

        // Left: Album Art Cover (32x32) with rounded corners
        float coverSize = 32.0f;
        float coverX = cardX + 9.0f;
        float coverY = cardY + (cardH - coverSize) / 2.0f;

        ResourceLocation coverLoc = media.getAlbumArtLocation();
        boolean hasCover = (coverLoc != null);
        if (hasCover) {
            try {
                Minecraft.getInstance().getTextureManager().getTexture(coverLoc).setFilter(true, false);
            } catch (Exception ignored) {}
            graphics.blit(
                    RenderType::guiTextured,
                    coverLoc,
                    (int) coverX,
                    (int) coverY,
                    0.0f,
                    0.0f,
                    (int) coverSize,
                    (int) coverSize,
                    (int) coverSize,
                    (int) coverSize,
                    (int) coverSize,
                    (int) coverSize
            );
            // Subtle glass hairline rim over the cover
            GlassRenderUtil.drawRoundedOutline(graphics, (int) coverX, (int) coverY, (int) coverSize, (int) coverSize, 5, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(0.4f), alpha));
        } else {
            // Elegant frosted badge with animated vinyl icon
            int iconBg = ThemeManager.lerpColor(0x30FFFFFF, 0x22000000, ThemeManager.getTransitionFactor());
            GlassRenderUtil.fillRoundedRect(graphics, coverX, coverY, coverSize, coverSize, 7.0f, applyAlpha(iconBg, alpha));
            GlassRenderUtil.drawRoundedOutline(graphics, (int) coverX, (int) coverY, (int) coverSize, (int) coverSize, 7, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(0.5f), alpha));

            int noteColor = media.isPlaying() ? 0xFF3B82F6 : ThemeManager.getTitleColor();
            MsdfRenderer.renderCenteredText(
                    Fonts.medium(),
                    "♫",
                    12.0f,
                    applyAlpha(noteColor, alpha),
                    graphics.pose().last().pose(),
                    coverX + coverSize / 2.0f,
                    coverY + (coverSize - 12.0f * 0.72f) / 2.0f,
                    0.0f
            );
        }

        // Track Info next to album art
        float titleX = coverX + coverSize + 9.0f;
        String title = media.getTitle();
        float maxTitleW = 120.0f;
        String displayTitle = Fonts.medium().getWidth(title, 8.0f) > maxTitleW ? title.substring(0, Math.min(title.length(), 18)) + "..." : title;
        MsdfRenderer.renderText(
                Fonts.medium(),
                displayTitle,
                8.0f,
                applyAlpha(ThemeManager.getTitleColor(), alpha),
                graphics.pose().last().pose(),
                titleX,
                cardY + 12.0f,
                0.0f
        );

        String artist = media.getArtist();
        String albumOrSource = !media.getAlbum().isEmpty() ? media.getAlbum() : media.getPlayerName();
        String artistLine = artist + " • " + albumOrSource;
        String displayArtist = Fonts.regular().getWidth(artistLine, 6.5f) > maxTitleW + 20.0f ? artist.substring(0, Math.min(artist.length(), 15)) + " • " + albumOrSource : artistLine;
        MsdfRenderer.renderText(
                Fonts.regular(),
                displayArtist,
                6.5f,
                applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), alpha),
                graphics.pose().last().pose(),
                titleX,
                cardY + 25.5f,
                0.0f
        );

        // Center: Transport Controls & Scrub Bar
        float ctrlCenterX = cardX + cardW / 2.0f + 15.0f;
        float ctrlBtnY = cardY + 7.0f;

        // Previous button with SKIP_BACK_ICON (Lucide skip-back)
        float prevSize = 20.0f;
        float prevX = ctrlCenterX - 36.0f;
        float prevY = ctrlBtnY - 1.0f;
        boolean hoverPrev = mouseX >= prevX && mouseX <= prevX + prevSize && mouseY >= prevY && mouseY <= prevY + prevSize;
        prevHoverAnim += ((hoverPrev ? 1.0f : 0.0f) - prevHoverAnim) * (1.0f - (float) Math.exp(-dt * 18.0f));

        float prevScale = (1.0f + prevHoverAnim * 0.10f) - (float) Math.sin(prevBounce * Math.PI) * 0.16f;
        graphics.pose().pushPose();
        graphics.pose().translate(prevX + prevSize / 2.0f, prevY + prevSize / 2.0f, 0.0f);
        graphics.pose().scale(prevScale, prevScale, 1.0f);
        graphics.pose().translate(-(prevX + prevSize / 2.0f), -(prevY + prevSize / 2.0f), 0.0f);

        if (prevHoverAnim > 0.01f) {
            GlassRenderUtil.fillRoundedRect(graphics, prevX, prevY, prevSize, prevSize, 5.0f, applyAlpha(0x28FFFFFF, alpha * prevHoverAnim));
            GlassRenderUtil.drawRoundedOutline(graphics, (int) prevX, (int) prevY, (int) prevSize, (int) prevSize, 5, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(0.5f), alpha * prevHoverAnim));
        }
        try {
            Minecraft.getInstance().getTextureManager().getTexture(SKIP_BACK_ICON).setFilter(true, false);
        } catch (Exception ignored) {}
        float prevIconSize = 12.0f;
        graphics.blit(
                RenderType::guiTextured,
                SKIP_BACK_ICON,
                (int) (prevX + (prevSize - prevIconSize) / 2.0f),
                (int) (prevY + (prevSize - prevIconSize) / 2.0f),
                0.0f,
                0.0f,
                (int) prevIconSize,
                (int) prevIconSize,
                128,
                128,
                128,
                128
        );
        graphics.pose().popPose();

        // Play / Pause button with PLAY_ICON / PAUSE_ICON (Lucide play / pause)
        float playSize = 22.0f;
        float playX = ctrlCenterX - playSize / 2.0f;
        float playY = ctrlBtnY - 2.0f;
        boolean hoverPlay = mouseX >= playX && mouseX <= playX + playSize && mouseY >= playY && mouseY <= playY + playSize;
        playHoverAnim += ((hoverPlay ? 1.0f : 0.0f) - playHoverAnim) * (1.0f - (float) Math.exp(-dt * 18.0f));

        float playScale = (1.0f + playHoverAnim * 0.10f) - (float) Math.sin(playBounce * Math.PI) * 0.18f;
        graphics.pose().pushPose();
        graphics.pose().translate(playX + playSize / 2.0f, playY + playSize / 2.0f, 0.0f);
        graphics.pose().scale(playScale, playScale, 1.0f);
        graphics.pose().translate(-(playX + playSize / 2.0f), -(playY + playSize / 2.0f), 0.0f);

        int playBg = hoverPlay
                ? ThemeManager.lerpColor(0x48FFFFFF, 0x38000000, ThemeManager.getTransitionFactor())
                : ThemeManager.lerpColor(0x28FFFFFF, 0x1A000000, ThemeManager.getTransitionFactor());
        if (playHoverAnim > 0.01f) {
            playBg = ThemeManager.lerpColor(playBg, 0x483B82F6, playHoverAnim * 0.65f);
        }
        GlassRenderUtil.fillRoundedRect(graphics, playX, playY, playSize, playSize, playSize / 2.0f, applyAlpha(playBg, alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) playX, (int) playY, (int) playSize, (int) playSize, (int)(playSize / 2.0f), 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(0.4f + playHoverAnim * 0.5f), alpha));

        ResourceLocation playPauseIcon = media.isPlaying() ? PAUSE_ICON : PLAY_ICON;
        try {
            Minecraft.getInstance().getTextureManager().getTexture(playPauseIcon).setFilter(true, false);
        } catch (Exception ignored) {}
        float pIconSize = 12.0f;
        float pIconOffX = media.isPlaying() ? 0.0f : 0.6f;
        graphics.blit(
                RenderType::guiTextured,
                playPauseIcon,
                (int) (playX + (playSize - pIconSize) / 2.0f + pIconOffX),
                (int) (playY + (playSize - pIconSize) / 2.0f),
                0.0f,
                0.0f,
                (int) pIconSize,
                (int) pIconSize,
                128,
                128,
                128,
                128
        );
        graphics.pose().popPose();

        // Next button with SKIP_FORWARD_ICON (Lucide skip-forward)
        float nextSize = 20.0f;
        float nextX = ctrlCenterX + 16.0f;
        float nextY = ctrlBtnY - 1.0f;
        boolean hoverNext = mouseX >= nextX && mouseX <= nextX + nextSize && mouseY >= nextY && mouseY <= nextY + nextSize;
        nextHoverAnim += ((hoverNext ? 1.0f : 0.0f) - nextHoverAnim) * (1.0f - (float) Math.exp(-dt * 18.0f));

        float nextScale = (1.0f + nextHoverAnim * 0.10f) - (float) Math.sin(nextBounce * Math.PI) * 0.16f;
        graphics.pose().pushPose();
        graphics.pose().translate(nextX + nextSize / 2.0f, nextY + nextSize / 2.0f, 0.0f);
        graphics.pose().scale(nextScale, nextScale, 1.0f);
        graphics.pose().translate(-(nextX + nextSize / 2.0f), -(nextY + nextSize / 2.0f), 0.0f);

        if (nextHoverAnim > 0.01f) {
            GlassRenderUtil.fillRoundedRect(graphics, nextX, nextY, nextSize, nextSize, 5.0f, applyAlpha(0x28FFFFFF, alpha * nextHoverAnim));
            GlassRenderUtil.drawRoundedOutline(graphics, (int) nextX, (int) nextY, (int) nextSize, (int) nextSize, 5, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(0.5f), alpha * nextHoverAnim));
        }
        try {
            Minecraft.getInstance().getTextureManager().getTexture(SKIP_FORWARD_ICON).setFilter(true, false);
        } catch (Exception ignored) {}
        float nextIconSize = 12.0f;
        graphics.blit(
                RenderType::guiTextured,
                SKIP_FORWARD_ICON,
                (int) (nextX + (nextSize - nextIconSize) / 2.0f),
                (int) (nextY + (nextSize - nextIconSize) / 2.0f),
                0.0f,
                0.0f,
                (int) nextIconSize,
                (int) nextIconSize,
                128,
                128,
                128,
                128
        );
        graphics.pose().popPose();

        // Quick Skip -10s indicator with tactile spring & pill badge
        float skip10PrevX = ctrlCenterX - 56.0f;
        float skip10PillW = 17.0f;
        float skip10PillH = 12.0f;
        float skip10Y = ctrlBtnY + 3.0f;
        boolean hoverSkip10Prev = mouseX >= skip10PrevX && mouseX <= skip10PrevX + skip10PillW && mouseY >= skip10Y && mouseY <= skip10Y + skip10PillH;
        skip10PrevHoverAnim += ((hoverSkip10Prev ? 1.0f : 0.0f) - skip10PrevHoverAnim) * (1.0f - (float) Math.exp(-dt * 18.0f));

        float skip10PrevScale = (1.0f + skip10PrevHoverAnim * 0.12f) - (float) Math.sin(skip10PrevBounce * Math.PI) * 0.18f;
        graphics.pose().pushPose();
        graphics.pose().translate(skip10PrevX + skip10PillW / 2.0f, skip10Y + skip10PillH / 2.0f, 0.0f);
        graphics.pose().scale(skip10PrevScale, skip10PrevScale, 1.0f);
        graphics.pose().translate(-(skip10PrevX + skip10PillW / 2.0f), -(skip10Y + skip10PillH / 2.0f), 0.0f);

        int pillPrevBg = ThemeManager.lerpColor(0x14FFFFFF, 0x10000000, ThemeManager.getTransitionFactor());
        if (skip10PrevHoverAnim > 0.01f) {
            pillPrevBg = ThemeManager.lerpColor(pillPrevBg, 0x383B82F6, skip10PrevHoverAnim);
        }
        GlassRenderUtil.fillRoundedRect(graphics, skip10PrevX, skip10Y, skip10PillW, skip10PillH, 3.5f, applyAlpha(pillPrevBg, alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) skip10PrevX, (int) skip10Y, (int) skip10PillW, (int) skip10PillH, 3, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(0.2f + skip10PrevHoverAnim * 0.5f), alpha));

        MsdfRenderer.renderCenteredText(
                Fonts.medium(),
                "-10",
                5.5f,
                applyAlpha(skip10PrevHoverAnim > 0.1f ? 0xFF93C5FD : ThemeManager.getSecondaryTextColor(0.0f), alpha),
                graphics.pose().last().pose(),
                skip10PrevX + skip10PillW / 2.0f,
                skip10Y + (skip10PillH - 5.5f * 0.72f) / 2.0f,
                0.0f
        );
        graphics.pose().popPose();

        // Quick Skip +10s indicator with tactile spring & pill badge
        float skip10NextX = ctrlCenterX + 39.0f;
        boolean hoverSkip10Next = mouseX >= skip10NextX && mouseX <= skip10NextX + skip10PillW && mouseY >= skip10Y && mouseY <= skip10Y + skip10PillH;
        skip10NextHoverAnim += ((hoverSkip10Next ? 1.0f : 0.0f) - skip10NextHoverAnim) * (1.0f - (float) Math.exp(-dt * 18.0f));

        float skip10NextScale = (1.0f + skip10NextHoverAnim * 0.12f) - (float) Math.sin(skip10NextBounce * Math.PI) * 0.18f;
        graphics.pose().pushPose();
        graphics.pose().translate(skip10NextX + skip10PillW / 2.0f, skip10Y + skip10PillH / 2.0f, 0.0f);
        graphics.pose().scale(skip10NextScale, skip10NextScale, 1.0f);
        graphics.pose().translate(-(skip10NextX + skip10PillW / 2.0f), -(skip10Y + skip10PillH / 2.0f), 0.0f);

        int pillNextBg = ThemeManager.lerpColor(0x14FFFFFF, 0x10000000, ThemeManager.getTransitionFactor());
        if (skip10NextHoverAnim > 0.01f) {
            pillNextBg = ThemeManager.lerpColor(pillNextBg, 0x383B82F6, skip10NextHoverAnim);
        }
        GlassRenderUtil.fillRoundedRect(graphics, skip10NextX, skip10Y, skip10PillW, skip10PillH, 3.5f, applyAlpha(pillNextBg, alpha));
        GlassRenderUtil.drawRoundedOutline(graphics, (int) skip10NextX, (int) skip10Y, (int) skip10PillW, (int) skip10PillH, 3, 0.5f, applyAlpha(ThemeManager.getGlassBorderColor(0.2f + skip10NextHoverAnim * 0.5f), alpha));

        MsdfRenderer.renderCenteredText(
                Fonts.medium(),
                "+10",
                5.5f,
                applyAlpha(skip10NextHoverAnim > 0.1f ? 0xFF93C5FD : ThemeManager.getSecondaryTextColor(0.0f), alpha),
                graphics.pose().last().pose(),
                skip10NextX + skip10PillW / 2.0f,
                skip10Y + (skip10PillH - 5.5f * 0.72f) / 2.0f,
                0.0f
        );
        graphics.pose().popPose();

        // Interactive Scrub Bar with Timestamps & Fluid Animations
        float scrubY = cardY + 31.0f;
        float scrubW = 160.0f;
        float scrubX = ctrlCenterX - scrubW / 2.0f;

        // Position text
        String posStr = media.getFormattedPosition();
        MsdfRenderer.renderText(
                Fonts.regular(),
                posStr,
                6.0f,
                applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), alpha),
                graphics.pose().last().pose(),
                scrubX - Fonts.regular().getWidth(posStr, 6.0f) - 6.0f,
                scrubY - 1.5f,
                0.0f
        );

        // Duration text
        String durStr = media.getFormattedDuration();
        MsdfRenderer.renderText(
                Fonts.regular(),
                durStr,
                6.0f,
                applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), alpha),
                graphics.pose().last().pose(),
                scrubX + scrubW + 6.0f,
                scrubY - 1.5f,
                0.0f
        );

        // Scrub track with hover expansion
        boolean hoverScrub = mouseX >= scrubX - 4.0f && mouseX <= scrubX + scrubW + 4.0f && mouseY >= scrubY - 5.0f && mouseY <= scrubY + 9.0f;
        scrubHoverAnim += (((hoverScrub || isDraggingScrubber) ? 1.0f : 0.0f) - scrubHoverAnim) * (1.0f - (float) Math.exp(-dt * 18.0f));

        float trackH = 3.0f + scrubHoverAnim * 1.5f;
        float trackDrawY = scrubY + (3.0f - trackH) / 2.0f;
        GlassRenderUtil.fillRoundedRect(graphics, scrubX, trackDrawY, scrubW, trackH, trackH / 2.0f, applyAlpha(0x22FFFFFF, alpha));

        // Smooth Progress Fill
        float fillW = scrubW * Math.max(0.0f, Math.min(1.0f, smoothProgress));
        if (fillW > 0.5f) {
            GlassRenderUtil.fillRoundedRect(graphics, scrubX, trackDrawY, fillW, trackH, trackH / 2.0f, applyAlpha(0xFF3B82F6, alpha));
        }

        // Rewind Wave Animation Sweep (Reverse Energy Pulse & Chevrons)
        if (rewindWaveAnim > 0.01f && fillW > 4.0f) {
            float waveT = rewindWaveAnim; // 1.0 down to 0.0
            float waveHeadX = scrubX + fillW * waveT;
            float waveLen = Math.min(26.0f, fillW);
            float waveLeft = Math.max(scrubX, waveHeadX - waveLen * 0.5f);
            float waveRight = Math.min(scrubX + fillW, waveHeadX + waveLen * 0.5f);
            if (waveRight > waveLeft) {
                GlassRenderUtil.fillRoundedRect(graphics, waveLeft, trackDrawY - 0.5f, waveRight - waveLeft, trackH + 1.0f, (trackH + 1.0f) / 2.0f, applyAlpha(0xEE60A5FA, alpha * rewindWaveAnim));
            }

            // Floating rewind chevrons drifting smoothly leftward
            float drift = (1.0f - rewindWaveAnim) * 16.0f;
            MsdfRenderer.renderCenteredText(
                    Fonts.medium(),
                    "« «",
                    6.0f,
                    applyAlpha(0xFF60A5FA, alpha * rewindWaveAnim),
                    graphics.pose().last().pose(),
                    scrubX + fillW * 0.5f - drift,
                    scrubY - 8.0f,
                    0.0f
            );
        }

        // Forward Wave Animation Sweep (Forward Energy Pulse & Chevrons)
        if (forwardWaveAnim > 0.01f && fillW > 4.0f) {
            float waveT = 1.0f - forwardWaveAnim; // 0.0 to 1.0
            float waveHeadX = scrubX + fillW * waveT;
            float waveLen = Math.min(26.0f, fillW);
            float waveLeft = Math.max(scrubX, waveHeadX - waveLen * 0.5f);
            float waveRight = Math.min(scrubX + fillW, waveHeadX + waveLen * 0.5f);
            if (waveRight > waveLeft) {
                GlassRenderUtil.fillRoundedRect(graphics, waveLeft, trackDrawY - 0.5f, waveRight - waveLeft, trackH + 1.0f, (trackH + 1.0f) / 2.0f, applyAlpha(0xEE93C5FD, alpha * forwardWaveAnim));
            }

            // Floating forward chevrons drifting smoothly rightward
            float drift = (1.0f - forwardWaveAnim) * 16.0f;
            MsdfRenderer.renderCenteredText(
                    Fonts.medium(),
                    "» »",
                    6.0f,
                    applyAlpha(0xFF93C5FD, alpha * forwardWaveAnim),
                    graphics.pose().last().pose(),
                    scrubX + fillW * 0.5f + drift,
                    scrubY - 8.0f,
                    0.0f
            );
        }

        // Animated Thumb Knob with Glow Halo
        if (fillW > 0.5f) {
            float thumbR = 2.5f + scrubHoverAnim * 1.5f;
            float thumbX = scrubX + fillW;
            float thumbY = trackDrawY + trackH / 2.0f;

            if (scrubHoverAnim > 0.02f) {
                float haloR = thumbR + 2.5f;
                GlassRenderUtil.fillRoundedRect(graphics, thumbX - haloR, thumbY - haloR, haloR * 2.0f, haloR * 2.0f, haloR, applyAlpha(0x353B82F6, alpha * scrubHoverAnim));
            }
            GlassRenderUtil.fillRoundedRect(graphics, thumbX - thumbR, thumbY - thumbR, thumbR * 2.0f, thumbR * 2.0f, thumbR, applyAlpha(0xFFFFFFFF, alpha));
        }

        // Scrub Hover Time Tooltip
        if (scrubHoverAnim > 0.05f && hoverScrub) {
            float hoverProg = Math.max(0.0f, Math.min(1.0f, ((float) mouseX - scrubX) / scrubW));
            long hoverMs = (long) (media.getDurationMs() * hoverProg);
            String hoverTimeStr = String.format("%d:%02d", (hoverMs / 60000), (hoverMs / 1000) % 60);

            float pillW = Fonts.medium().getWidth(hoverTimeStr, 6.0f) + 8.0f;
            float pillH = 11.0f;
            float pillX = Math.max(scrubX, Math.min(scrubX + scrubW - pillW, (float) mouseX - pillW / 2.0f));
            float pillY = trackDrawY - pillH - 4.0f - scrubHoverAnim * 2.0f;

            GlassRenderUtil.fillRoundedRect(graphics, pillX, pillY, pillW, pillH, 3.5f, applyAlpha(0xE01E293B, alpha * scrubHoverAnim));
            GlassRenderUtil.drawRoundedOutline(graphics, (int) pillX, (int) pillY, (int) pillW, (int) pillH, 3, 0.5f, applyAlpha(0x30FFFFFF, alpha * scrubHoverAnim));
            MsdfRenderer.renderCenteredText(
                    Fonts.medium(),
                    hoverTimeStr,
                    6.0f,
                    applyAlpha(0xFFFFFFFF, alpha * scrubHoverAnim),
                    graphics.pose().last().pose(),
                    pillX + pillW / 2.0f,
                    pillY + (pillH - 6.0f * 0.72f) / 2.0f,
                    0.0f
            );
        }

        // Right: Animated Audio Spectrum Bars & Source Tag
        float[] bars = media.getSpectrumBars();
        float barW = 3.0f;
        float barSpacing = 2.5f;
        float totalBarsW = bars.length * barW + (bars.length - 1) * barSpacing;
        float specX = cardX + cardW - totalBarsW - 16.0f;
        float specCenterY = cardY + cardH / 2.0f;
        float maxBarH = 20.0f;

        for (int i = 0; i < bars.length; i++) {
            float bH = Math.max(3.0f, bars[i] * maxBarH);
            float bX = specX + i * (barW + barSpacing);
            float bY = specCenterY - bH / 2.0f;
            int barColor = ThemeManager.lerpColor(0xFF3B82F6, 0xFF60A5FA, bars[i]);
            GlassRenderUtil.fillRoundedRect(graphics, bX, bY, barW, bH, 1.5f, applyAlpha(barColor, alpha));
        }
    }

    private float calculateSettingsHeight(Module module) {
        float h = 26.0f; // divider + keybind row
        for (Setting<?> s : module.getSettings()) {
            if (s instanceof NumberSetting) h += 26.0f;
            else if (s instanceof BooleanSetting) h += 18.0f;
            else if (s instanceof ModeSetting) h += 20.0f;
        }
        return h + 6.0f; // bottom padding
    }

    private void renderModuleCard(GuiGraphics graphics, Module module, float cardX, float cardY, float cardW, float cardH, float baseCardH, int mouseX, int mouseY, float dt, float screenAlpha) {
        boolean hovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + baseCardH;

        // Smooth hover progress interpolation
        float currentHover = moduleHoverMap.getOrDefault(module, 0.0f);
        float targetHover = hovered ? 1.0f : 0.0f;
        currentHover += (targetHover - currentHover) * (1.0f - (float) Math.exp(-dt * 14.0f));
        moduleHoverMap.put(module, currentHover);

        int cardBg = ThemeManager.lerpColor(0x16FFFFFF, 0x0E000000, ThemeManager.getTransitionFactor());
        if (module.isEnabled()) {
            cardBg = ThemeManager.lerpColor(0x28FFFFFF, 0x1A000000, ThemeManager.getTransitionFactor());
        }

        // Frosted card tile (Radius 10)
        GlassRenderUtil.fillRoundedRect(graphics, cardX, cardY, cardW, cardH, 10, applyAlpha(cardBg, screenAlpha));

        // Smooth hairline border illumination on hover or when enabled
        float borderAlpha = Math.max(currentHover, module.isEnabled() ? 0.35f : 0.0f);
        if (borderAlpha > 0.01f) {
            GlassRenderUtil.drawRoundedOutline(
                    graphics,
                    (int)cardX,
                    (int)cardY,
                    (int)cardW,
                    (int)cardH,
                    10,
                    0.5f,
                    applyAlpha(ThemeManager.getGlassBorderColor(borderAlpha), screenAlpha)
            );
        }

        // Left accent indicator line for enabled modules
        if (module.isEnabled()) {
            GlassRenderUtil.fillRoundedRect(
                    graphics,
                    cardX + 6.0f,
                    cardY + 12.0f,
                    2.5f,
                    20.0f,
                    1.25f,
                    applyAlpha(module.getCategory().getAccentColor(), screenAlpha)
            );
        }

        // Module Name (English)
        int titleColor = module.isEnabled() ? ThemeManager.getTitleColor() : ThemeManager.getSecondaryTextColor(currentHover);
        MsdfRenderer.renderText(
                Fonts.medium(),
                module.getName(),
                9.0f,
                applyAlpha(titleColor, screenAlpha),
                graphics.pose().last().pose(),
                cardX + (module.isEnabled() ? 14.0f : 10.0f),
                cardY + 9.5f,
                0.0f
        );

        // Category Tag Badge when live search is active
        if (!searchQuery.trim().isEmpty()) {
            float nameW = Fonts.medium().getWidth(module.getName(), 9.0f);
            float badgeX = cardX + (module.isEnabled() ? 14.0f : 10.0f) + nameW + 6.0f;
            float badgeY = cardY + 9.5f;
            float cIconSize = 8.0f;
            drawTintedIcon(graphics, module.getCategory().getIconLocation(), badgeX, badgeY + 1.0f, cIconSize, applyAlpha(module.getCategory().getAccentColor(), screenAlpha * 0.90f));
            MsdfRenderer.renderText(
                    Fonts.regular(),
                    module.getCategory().getDisplayName(),
                    6.0f,
                    applyAlpha(module.getCategory().getAccentColor(), screenAlpha * 0.90f),
                    graphics.pose().last().pose(),
                    badgeX + cIconSize + 3.0f,
                    badgeY + 1.5f,
                    0.0f
            );
        }

        // Module Description (English)
        MsdfRenderer.renderText(
                Fonts.regular(),
                module.getDescription(),
                6.8f,
                applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), screenAlpha),
                graphics.pose().last().pose(),
                cardX + (module.isEnabled() ? 14.0f : 10.0f),
                cardY + 23.5f,
                0.0f
        );

        // Right-side controls: Settings Chevron & iOS Switch
        float switchW = 28.0f;
        float switchH = 14.0f;
        float switchX = cardX + cardW - switchW - 10.0f;
        float switchY = cardY + (baseCardH - switchH) / 2.0f;

        // Animated Rotating Chevron Indicator (shown if module has settings)
        if (!module.getSettings().isEmpty()) {
            float gearX = switchX - 16.0f;
            float gearY = cardY + (baseCardH - 12.0f) / 2.0f;
            boolean hoverGear = mouseX >= gearX - 4 && mouseX <= gearX + 12 && mouseY >= gearY - 4 && mouseY <= gearY + 14;

            graphics.pose().pushPose();
            graphics.pose().translate(gearX + 3.0f, gearY + 6.0f, 0.0f);
            // Smooth 180-degree rotation when drawer expands
            float chevRot = module.getEasedExpand() * 180.0f;
            graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(chevRot));

            int chevColor = hoverGear ? 0xFFFFFFFF : 0xFF9CA3AF;
            drawVectorChevron(graphics, 0.0f, 0.0f, applyAlpha(chevColor, screenAlpha));
            graphics.pose().popPose();
        }

        // Apple iOS Switch
        float toggleProg = module.getToggleProgress();
        // Crossfade track color to vibrant Emerald Green
        int switchTrackColor = ThemeManager.lerpColor(0x3564748B, 0xFF22C55E, toggleProg);
        GlassRenderUtil.fillRoundedRect(graphics, switchX, switchY, switchW, switchH, switchH / 2.0f, applyAlpha(switchTrackColor, screenAlpha));

        float thumbSize = switchH - 2.5f;
        float thumbX = switchX + 1.25f + toggleProg * (switchW - thumbSize - 2.5f);
        GlassRenderUtil.fillRoundedRect(graphics, thumbX, switchY + 1.25f, thumbSize, thumbSize, thumbSize / 2.0f, applyAlpha(0xFFFFFFFF, screenAlpha));

        // Settings Drawer rendering (with smooth scissor clipping and height expansion)
        if (module.getExpandProgress() > 0.005f) {
            float drawerH = cardH - baseCardH;
            if (drawerH > 1.0f) {
                graphics.enableScissor(
                        (int) Math.floor(cardX),
                        (int) Math.floor(cardY + baseCardH),
                        (int) Math.ceil(cardX + cardW),
                        (int) Math.ceil(cardY + cardH)
                );
                renderSettingsDrawer(graphics, module, cardX, cardY + baseCardH, cardW, mouseX, mouseY, dt, screenAlpha);
                graphics.disableScissor();
            }
        }
    }

    private void renderSettingsDrawer(GuiGraphics graphics, Module module, float startX, float startY, float width, int mouseX, int mouseY, float dt, float screenAlpha) {
        float drawerAlpha = module.getEasedExpand() * screenAlpha;
        float currentY = startY + 2.0f;

        // Subtle divider line
        int divColor = applyAlpha(ThemeManager.lerpColor(0x18FFFFFF, 0x18000000, ThemeManager.getTransitionFactor()), drawerAlpha);
        GlassRenderUtil.fillRoundedRect(graphics, startX + 10.0f, currentY, width - 20.0f, 1.0f, 0.5f, divColor);
        currentY += 6.0f;

        // Keybind selector row (English) with smooth binding & flash animations
        boolean isBinding = (bindingModule == module);
        float targetBind = isBinding ? 1.0f : 0.0f;
        float bindProg = bindProgressMap.getOrDefault(module, 0.0f);
        bindProg += (targetBind - bindProg) * (1.0f - (float) Math.exp(-dt * 18.0f));
        bindProgressMap.put(module, bindProg);

        float flash = bindFlashMap.getOrDefault(module, 0.0f);
        if (flash > 0.001f) {
            flash += (0.0f - flash) * (1.0f - (float) Math.exp(-dt * 8.0f));
            bindFlashMap.put(module, flash);
        }

        // Cycling dots for "Press key..."
        int dotCount = (int) ((System.currentTimeMillis() / 320) % 4);
        String dotStr = switch (dotCount) {
            case 1 -> ".";
            case 2 -> "..";
            case 3 -> "...";
            default -> "";
        };
        String keyName = isBinding ? ("Press key" + dotStr) : getKeyName(module.getKeybind());

        // Smooth width morphing
        float targetBoxW = Math.max(48.0f, Fonts.medium().getWidth(keyName, 7.0f) + 16.0f);
        float boxW = bindWidthMap.getOrDefault(module, targetBoxW);
        boxW += (targetBoxW - boxW) * (1.0f - (float) Math.exp(-dt * 20.0f));
        bindWidthMap.put(module, boxW);

        float boxH = 14.0f;
        float boxX = startX + width - boxW - 12.0f;
        float boxY = currentY;

        float labelY = boxY + (boxH - 7.0f * 0.72f) / 2.0f;
        MsdfRenderer.renderText(
                Fonts.medium(),
                "Keybind:",
                7.0f,
                applyAlpha(ThemeManager.getSecondaryTextColor(0.0f), drawerAlpha),
                graphics.pose().last().pose(),
                startX + 12.0f,
                labelY,
                0.0f
        );

        // Breathing / Heartbeat pulse scale when binding
        float pulse = (float) (Math.sin(System.currentTimeMillis() / 140.0) * 0.5 + 0.5);
        float bindScale = 1.0f + 0.045f * bindProg * (0.35f + 0.65f * pulse);

        graphics.pose().pushPose();
        graphics.pose().translate(boxX + boxW / 2.0f, boxY + boxH / 2.0f, 0.0f);
        graphics.pose().scale(bindScale, bindScale, 1.0f);
        graphics.pose().translate(-(boxX + boxW / 2.0f), -(boxY + boxH / 2.0f), 0.0f);

        // Dynamic background with pulse and success flash
        int activeBlue = ThemeManager.lerpColor(0xFF2563EB, 0xFF3B82F6, pulse);
        int baseBg = applyAlpha(0x22FFFFFF, drawerAlpha);
        int keyBg = ThemeManager.lerpColor(baseBg, activeBlue, bindProg);
        if (flash > 0.005f) {
            keyBg = ThemeManager.lerpColor(keyBg, 0xFF10B981, flash);
        }
        GlassRenderUtil.fillRoundedRect(graphics, boxX, boxY, boxW, boxH, 4.0f, keyBg);

        // Glowing animated border
        if (bindProg > 0.02f) {
            int glowBorder = ThemeManager.lerpColor(0x803B82F6, 0xFF93C5FD, pulse);
            GlassRenderUtil.drawRoundedOutline(
                    graphics,
                    (int) boxX,
                    (int) boxY,
                    (int) boxW,
                    (int) boxH,
                    4,
                    0.6f + 0.6f * pulse,
                    applyAlpha(glowBorder, drawerAlpha * bindProg)
            );
        } else if (flash > 0.02f) {
            GlassRenderUtil.drawRoundedOutline(
                    graphics,
                    (int) boxX,
                    (int) boxY,
                    (int) boxW,
                    (int) boxH,
                    4,
                    1.0f,
                    applyAlpha(0xFF34D399, drawerAlpha * flash)
            );
        }

        // Text color & centered rendering
        int textBase = ThemeManager.getTitleColor();
        int textColor = (bindProg > 0.1f) ? 0xFFFFFFFF : textBase;
        if (flash > 0.01f) {
            textColor = ThemeManager.lerpColor(textColor, 0xFFECFDF5, flash);
        }

        float textCenterX = boxX + boxW / 2.0f;
        float textCenterY = boxY + (boxH - 7.0f * 0.72f) / 2.0f;
        MsdfRenderer.renderCenteredText(
                Fonts.medium(),
                keyName,
                7.0f,
                applyAlpha(textColor, drawerAlpha),
                graphics.pose().last().pose(),
                textCenterX,
                textCenterY,
                0.0f
        );

        graphics.pose().popPose();
        currentY += 18.0f;

        // Module settings list (English)
        for (Setting<?> setting : module.getSettings()) {
            float rowH = (setting instanceof NumberSetting) ? 26.0f : ((setting instanceof BooleanSetting) ? 18.0f : 20.0f);
            boolean isRowHovered = mouseX >= startX + 6.0f && mouseX <= startX + width - 6.0f && mouseY >= currentY - 2.0f && mouseY <= currentY + rowH - 2.0f;
            float sHover = settingHoverMap.getOrDefault(setting, 0.0f);
            sHover += ((isRowHovered ? 1.0f : 0.0f) - sHover) * (1.0f - (float) Math.exp(-dt * 16.0f));
            settingHoverMap.put(setting, sHover);

            if (sHover > 0.01f) {
                GlassRenderUtil.fillRoundedRect(graphics, startX + 6.0f, currentY - 2.0f, width - 12.0f, rowH, 4.0f, applyAlpha(0x10FFFFFF, drawerAlpha * sHover));
            }

            if (setting instanceof NumberSetting num) {
                float sliderW = width - 24.0f;
                float sliderX = startX + 12.0f;

                String valStr = String.format("%.1f", num.getValue());
                MsdfRenderer.renderText(
                        Fonts.medium(),
                        setting.getName(),
                        7.0f,
                        applyAlpha(ThemeManager.getSecondaryTextColor(sHover), drawerAlpha),
                        graphics.pose().last().pose(),
                        sliderX,
                        currentY,
                        0.0f
                );
                MsdfRenderer.renderText(
                        Fonts.medium(),
                        valStr,
                        7.0f,
                        applyAlpha(ThemeManager.getTitleColor(), drawerAlpha),
                        graphics.pose().last().pose(),
                        sliderX + sliderW - Fonts.medium().getWidth(valStr, 7.0f),
                        currentY,
                        0.0f
                );

                currentY += 10.0f;
                // Slider Track
                float trackH = 4.0f;

                float targetProg = num.getSliderProgress();
                float sProg = sliderProgressMap.getOrDefault(num, targetProg);
                if (draggingSlider == num) {
                    sProg = targetProg;
                } else {
                    sProg += (targetProg - sProg) * (1.0f - (float) Math.exp(-dt * 20.0f));
                }
                sliderProgressMap.put(num, sProg);

                boolean hoverSlider = mouseX >= sliderX - 4.0f && mouseX <= sliderX + sliderW + 4.0f && mouseY >= currentY - 4.0f && mouseY <= currentY + 8.0f;
                float slHover = sliderHoverMap.getOrDefault(num, 0.0f);
                slHover += (((hoverSlider || draggingSlider == num) ? 1.0f : 0.0f) - slHover) * (1.0f - (float) Math.exp(-dt * 18.0f));
                sliderHoverMap.put(num, slHover);

                GlassRenderUtil.fillRoundedRect(graphics, sliderX, currentY, sliderW, trackH, 2.0f, applyAlpha(0x20FFFFFF, drawerAlpha));
                // Filled progress with smooth animation
                float fillW = sliderW * Math.max(0.0f, Math.min(1.0f, sProg));
                GlassRenderUtil.fillRoundedRect(graphics, sliderX, currentY, fillW, trackH, 2.0f, applyAlpha(0xFF3B82F6, drawerAlpha));

                // Slider Thumb with hover halo & expansion
                float thumbW = 7.0f + slHover * 1.5f;
                float thumbH = 9.0f + slHover * 2.0f;
                float thumbX = sliderX + fillW - thumbW / 2.0f;
                float thumbY = currentY + trackH / 2.0f - thumbH / 2.0f;

                if (slHover > 0.02f) {
                    GlassRenderUtil.fillRoundedRect(graphics, thumbX - 2.5f, thumbY - 2.0f, thumbW + 5.0f, thumbH + 4.0f, 4.0f, applyAlpha(0x353B82F6, drawerAlpha * slHover));
                }
                GlassRenderUtil.fillRoundedRect(graphics, thumbX, thumbY, thumbW, thumbH, 3.5f, applyAlpha(0xFFFFFFFF, drawerAlpha));

                currentY += 16.0f;
            } else if (setting instanceof BooleanSetting bool) {
                float rowX = startX + 12.0f;
                float swW = 22.0f;
                float swH = 12.0f;
                float swX = startX + width - swW - 12.0f;
                float swY = currentY + 1.0f;
                float bLabelY = currentY + (swH - 7.0f * 0.72f) / 2.0f;

                MsdfRenderer.renderText(
                        Fonts.medium(),
                        bool.getName(),
                        7.0f,
                        applyAlpha(ThemeManager.getSecondaryTextColor(sHover), drawerAlpha),
                        graphics.pose().last().pose(),
                        rowX,
                        bLabelY,
                        0.0f
                );

                // Smooth animated switch toggle & color transition
                float targetToggle = bool.getValue() ? 1.0f : 0.0f;
                float bProg = boolToggleMap.getOrDefault(bool, targetToggle);
                bProg += (targetToggle - bProg) * (1.0f - (float) Math.exp(-dt * 18.0f));
                boolToggleMap.put(bool, bProg);

                int trackColor = ThemeManager.lerpColor(0x30888888, 0xFF22C55E, bProg);
                GlassRenderUtil.fillRoundedRect(graphics, swX, swY, swW, swH, swH / 2.0f, applyAlpha(trackColor, drawerAlpha));

                float thSize = swH - 2.0f;
                float thX = swX + 1.0f + bProg * (swW - thSize - 2.0f);
                GlassRenderUtil.fillRoundedRect(graphics, thX, swY + 1.0f, thSize, thSize, thSize / 2.0f, applyAlpha(0xFFFFFFFF, drawerAlpha));

                currentY += 18.0f;
            } else if (setting instanceof ModeSetting mode) {
                float rowX = startX + 12.0f;
                String mVal = mode.getValue();
                float mPillW = Math.max(48.0f, Fonts.medium().getWidth(mVal, 7.0f) + 16.0f);
                float mPillH = 14.0f;
                float mPillX = startX + width - mPillW - 12.0f;
                float mPillY = currentY;

                float mLabelY = mPillY + (mPillH - 7.0f * 0.72f) / 2.0f;
                MsdfRenderer.renderText(
                        Fonts.medium(),
                        mode.getName() + ":",
                        7.0f,
                        applyAlpha(ThemeManager.getSecondaryTextColor(sHover), drawerAlpha),
                        graphics.pose().last().pose(),
                        rowX,
                        mLabelY,
                        0.0f
                );

                boolean hoverMode = mouseX >= mPillX && mouseX <= mPillX + mPillW && mouseY >= mPillY && mouseY <= mPillY + mPillH;
                float bounce = modeBounceMap.getOrDefault(mode, 0.0f);
                bounce = Math.max(0.0f, bounce - dt * 5.0f);
                modeBounceMap.put(mode, bounce);

                float modeScale = (1.0f + (hoverMode ? 0.03f : 0.0f)) - (float) Math.sin(bounce * Math.PI) * 0.10f;
                graphics.pose().pushPose();
                graphics.pose().translate(mPillX + mPillW / 2.0f, mPillY + mPillH / 2.0f, 0.0f);
                graphics.pose().scale(modeScale, modeScale, 1.0f);
                graphics.pose().translate(-(mPillX + mPillW / 2.0f), -(mPillY + mPillH / 2.0f), 0.0f);

                int pillBg = hoverMode ? 0x40FFFFFF : 0x25FFFFFF;
                GlassRenderUtil.fillRoundedRect(graphics, mPillX, mPillY, mPillW, mPillH, 4.0f, applyAlpha(pillBg, drawerAlpha));
                float mTextCenterX = mPillX + mPillW / 2.0f;
                float mTextCenterY = mPillY + (mPillH - 7.0f * 0.72f) / 2.0f;
                MsdfRenderer.renderCenteredText(
                        Fonts.medium(),
                        mVal,
                        7.0f,
                        applyAlpha(ThemeManager.getTitleColor(), drawerAlpha),
                        graphics.pose().last().pose(),
                        mTextCenterX,
                        mTextCenterY,
                        0.0f
                );
                graphics.pose().popPose();

                currentY += 20.0f;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isClosing) return false;

        // Give enabled HUD modules (such as Dynamic Island) priority to capture clicks
        for (dev.cweldlc.client.module.Module mod : dev.cweldlc.client.module.ModuleManager.getInstance().getModules()) {
            if (mod.isEnabled() && mod.onMouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        float winX = (this.width - WIN_WIDTH) / 2.0f;
        float winY = (this.height - WIN_HEIGHT) / 2.0f;

        float divX = winX + SIDEBAR_WIDTH + 2.0f;
        float mainX = divX + 12.0f;
        float mainW = (winX + WIN_WIDTH) - mainX - 12.0f;

        // 1. Header Right Controls Click: Media Toggle + Search Box
        float btnSize = 21.0f;
        float btnY = winY + 10.0f;
        float musicBtnX = mainX + mainW - btnSize;
        float searchX = musicBtnX - searchBoxWidth - 6.0f;

        if (button == 0) {
            // Media Player toggle click
            if (mouseX >= musicBtnX && mouseX <= musicBtnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize) {
                showMediaPlayer = !showMediaPlayer;
                playClick(showMediaPlayer ? 1.3f : 0.95f);
                return true;
            }

            // Search Box click
            if (mouseX >= searchX && mouseX <= searchX + searchBoxWidth && mouseY >= btnY && mouseY <= btnY + btnSize) {
                if (!searchQuery.isEmpty() && mouseX >= searchX + searchBoxWidth - 14.0f) {
                    searchQuery = "";
                    searchFocused = false;
                } else {
                    searchFocused = true;
                }
                playClick(1.3f);
                return true;
            } else {
                searchFocused = false;
            }
        }

        // 2. Left Sidebar Category Click
        float catStartY = winY + 54.0f;
        float catH = 26.0f;
        float catSpacing = 4.0f;
        float catW = SIDEBAR_WIDTH - 20.0f;
        float catX = winX + 10.0f;

        for (Category cat : Category.values()) {
            float rowY = catStartY + cat.ordinal() * (catH + catSpacing);
            if (mouseX >= catX && mouseX <= catX + catW && mouseY >= rowY && mouseY <= rowY + catH && button == 0) {
                if (currentCategory != cat) {
                    currentCategory = cat;
                    searchQuery = "";
                    targetScrollY = 0.0f;
                    playClick(1.2f);
                }
                return true;
            }
        }

        // 3. Floating Media Player Card Interaction
        if (showMediaPlayer && mediaAnimProgress > 0.4f) {
            float cardW = WIN_WIDTH;
            float cardH = 48.0f;
            float cardX = winX;
            float cardY = winY + WIN_HEIGHT + 8.0f;
            if (cardY + cardH > this.height - 4.0f) {
                cardY = this.height - cardH - 4.0f;
            }

            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                MediaManager media = MediaManager.getInstance();
                float ctrlCenterX = cardX + cardW / 2.0f + 15.0f;
                float ctrlBtnY = cardY + 7.0f;

                // Scrub bar click & seek
                float scrubW = 160.0f;
                float scrubX = ctrlCenterX - scrubW / 2.0f;
                float scrubY = cardY + 31.0f;
                if (mouseX >= scrubX - 4.0f && mouseX <= scrubX + scrubW + 4.0f && mouseY >= scrubY - 6.0f && mouseY <= scrubY + 10.0f) {
                    float prog = (float) Math.max(0.0f, Math.min(1.0f, (mouseX - scrubX) / scrubW));
                    float curProg = media.getProgress();
                    if (prog < curProg - 0.03f) {
                        rewindWaveAnim = 1.0f;
                    } else if (prog > curProg + 0.03f) {
                        forwardWaveAnim = 1.0f;
                    }
                    media.seekTo(prog);
                    isDraggingScrubber = true;
                    playClick(1.2f);
                    return true;
                }

                // Quick skip -10s
                float skip10PrevX = ctrlCenterX - 56.0f;
                if (mouseX >= skip10PrevX - 2 && mouseX <= skip10PrevX + 19 && mouseY >= ctrlBtnY + 1 && mouseY <= ctrlBtnY + 16) {
                    media.seekOffset(-10);
                    skip10PrevBounce = 1.0f;
                    rewindWaveAnim = 1.0f;
                    playClick(1.15f);
                    return true;
                }

                // Previous button
                float prevSize = 20.0f;
                float prevX = ctrlCenterX - 36.0f;
                float prevY = ctrlBtnY - 1.0f;
                if (mouseX >= prevX && mouseX <= prevX + prevSize && mouseY >= prevY && mouseY <= prevY + prevSize) {
                    media.previous();
                    prevBounce = 1.0f;
                    rewindWaveAnim = 1.0f;
                    playClick(1.2f);
                    return true;
                }

                // Play / Pause button
                float playSize = 22.0f;
                float playX = ctrlCenterX - playSize / 2.0f;
                float playY = ctrlBtnY - 2.0f;
                if (mouseX >= playX && mouseX <= playX + playSize && mouseY >= playY && mouseY <= playY + playSize) {
                    media.playPause();
                    playBounce = 1.0f;
                    playClick(media.isPlaying() ? 1.3f : 0.9f);
                    return true;
                }

                // Next button
                float nextSize = 20.0f;
                float nextX = ctrlCenterX + 16.0f;
                float nextY = ctrlBtnY - 1.0f;
                if (mouseX >= nextX && mouseX <= nextX + nextSize && mouseY >= nextY && mouseY <= nextY + nextSize) {
                    media.next();
                    nextBounce = 1.0f;
                    forwardWaveAnim = 1.0f;
                    playClick(1.2f);
                    return true;
                }

                // Quick skip +10s
                float skip10NextX = ctrlCenterX + 39.0f;
                if (mouseX >= skip10NextX - 2 && mouseX <= skip10NextX + 19 && mouseY >= ctrlBtnY + 1 && mouseY <= ctrlBtnY + 16) {
                    media.seekOffset(10);
                    skip10NextBounce = 1.0f;
                    forwardWaveAnim = 1.0f;
                    playClick(1.15f);
                    return true;
                }

                return true;
            }
        }

        // 4. Module Cards Click Handling
        List<Module> modules;
        if (!searchQuery.trim().isEmpty()) {
            String q = searchQuery.trim().toLowerCase();
            modules = ModuleManager.getInstance().getModules().stream()
                    .filter(m -> m.getName().toLowerCase().contains(q) || m.getDescription().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        } else {
            modules = ModuleManager.getInstance().getModulesByCategory(currentCategory);
        }

        float contentX = mainX;
        float contentY = winY + 40.0f;
        float contentW = mainW;
        float colWidth = (contentW - 10.0f) / 2.0f;
        float[] colY = new float[]{contentY + scrollY, contentY + scrollY};

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % 2;
            float cardX = contentX + col * (colWidth + 10.0f);
            float cardY = colY[col];
            float baseCardH = 44.0f;
            float settingsH = calculateSettingsHeight(module);
            float cardH = baseCardH + settingsH * module.getEasedExpand();

            float switchW = 28.0f;
            float switchH = 14.0f;
            float switchX = cardX + colWidth - switchW - 10.0f;
            float switchY = cardY + (baseCardH - switchH) / 2.0f;
            float gearX = switchX - 16.0f;
            float gearY = cardY + (baseCardH - 12.0f) / 2.0f;

            // RIGHT CLICK (button == 1) on module card -> EXPAND / COLLAPSE SETTINGS DRAWER!
            if (button == 1 && mouseX >= cardX && mouseX <= cardX + colWidth && mouseY >= cardY && mouseY <= cardY + baseCardH) {
                module.toggleExpanded();
                playClick(module.isExpanded() ? 1.25f : 1.0f);
                return true;
            }

            // LEFT CLICK (button == 0)
            if (button == 0) {
                // Chevron click
                if (mouseX >= gearX - 4 && mouseX <= gearX + 14 && mouseY >= gearY - 4 && mouseY <= gearY + 16) {
                    module.toggleExpanded();
                    playClick(module.isExpanded() ? 1.25f : 1.0f);
                    return true;
                }

                // Module card / Switch click -> Toggle module ON / OFF
                if (mouseX >= cardX && mouseX <= cardX + colWidth && mouseY >= cardY && mouseY <= cardY + baseCardH) {
                    module.toggle();
                    ModuleManager.getInstance().saveConfig();
                    playClick(module.isEnabled() ? 1.35f : 0.85f);
                    return true;
                }

                // Settings drawer interaction
                if (module.isExpanded() && module.getEasedExpand() > 0.2f && mouseY > cardY + baseCardH && mouseY <= cardY + cardH) {
                    if (handleSettingsClick(module, cardX, cardY + baseCardH, colWidth, mouseX, mouseY)) {
                        return true;
                    }
                }
            }

            colY[col] += cardH + 8.0f;
        }

        // Click outside the window initiates smooth close
        boolean insideWindow = mouseX >= winX && mouseX <= winX + WIN_WIDTH && mouseY >= winY && mouseY <= winY + WIN_HEIGHT;
        float mediaY = winY + WIN_HEIGHT + 8.0f;
        if (mediaY + 48.0f > this.height - 4.0f) {
            mediaY = this.height - 48.0f - 4.0f;
        }
        boolean insideMedia = showMediaPlayer && mouseX >= winX && mouseX <= winX + WIN_WIDTH && mouseY >= mediaY && mouseY <= mediaY + 48.0f;
        if (!insideWindow && !insideMedia && button == 0) {
            closeSmoothly();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSettingsClick(Module module, float startX, float startY, float width, double mouseX, double mouseY) {
        float currentY = startY + 8.0f;

        // Keybind selector click
        String keyName = (bindingModule == module) ? "Press any key..." : getKeyName(module.getKeybind());
        float boxW = Math.max(48.0f, Fonts.medium().getWidth(keyName, 7.0f) + 16.0f);
        float boxH = 14.0f;
        float boxX = startX + width - boxW - 12.0f;
        float boxY = currentY;

        if (mouseY >= boxY - 2.0f && mouseY <= boxY + boxH + 2.0f) {
            bindingModule = (bindingModule == module) ? null : module;
            playClick(1.4f);
            return true;
        }
        currentY += 18.0f;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof NumberSetting num) {
                float sliderX = startX + 12.0f;
                float sliderW = width - 24.0f;
                currentY += 10.0f;
                if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= currentY - 6.0f && mouseY <= currentY + 10.0f) {
                    float prog = (float) ((mouseX - sliderX) / sliderW);
                    num.setFromProgress(prog);
                    draggingSlider = num;
                    ModuleManager.getInstance().saveConfig();
                    return true;
                }
                currentY += 16.0f;
            } else if (setting instanceof BooleanSetting bool) {
                if (mouseY >= currentY && mouseY <= currentY + 14.0f) {
                    bool.toggle();
                    ModuleManager.getInstance().saveConfig();
                    dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TOGGLE, bool.getValue() ? 1.1f : 0.9f, 0.75f);
                    return true;
                }
                currentY += 18.0f;
            } else if (setting instanceof ModeSetting mode) {
                if (mouseY >= currentY && mouseY <= currentY + 16.0f) {
                    mode.cycle();
                    modeBounceMap.put(mode, 1.0f);
                    ModuleManager.getInstance().saveConfig();
                    playClick(1.2f);
                    return true;
                }
                currentY += 20.0f;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDraggingScrubber) {
            float winX = (this.width - WIN_WIDTH) / 2.0f;
            float cardW = WIN_WIDTH;
            float ctrlCenterX = winX + cardW / 2.0f + 15.0f;
            float scrubW = 160.0f;
            float scrubX = ctrlCenterX - scrubW / 2.0f;
            float prog = (float) Math.max(0.0f, Math.min(1.0f, (mouseX - scrubX) / scrubW));
            float curProg = MediaManager.getInstance().getProgress();
            if (prog < curProg - 0.04f && rewindWaveAnim < 0.3f) {
                rewindWaveAnim = 0.8f;
            } else if (prog > curProg + 0.04f && forwardWaveAnim < 0.3f) {
                forwardWaveAnim = 0.8f;
            }
            MediaManager.getInstance().seekTo(prog);
            return true;
        }
        if (draggingSlider != null) {
            float winX = (this.width - WIN_WIDTH) / 2.0f;
            float colWidth = (WIN_WIDTH - 38.0f) / 2.0f;
            float sliderW = colWidth - 24.0f;
            float prog = (float) Math.max(0.0f, Math.min(1.0f, (mouseX - (winX + 26.0f)) / sliderW));
            draggingSlider.setFromProgress(prog);
            ModuleManager.getInstance().saveConfig();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = null;
        isDraggingScrubber = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScrollY += (float) (verticalAmount * 24.0f);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindingModule != null) {
            Module mod = bindingModule;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                mod.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
                dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.CRITICAL, 1.0f, 0.8f);
            } else {
                mod.setKeybind(keyCode);
                dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.APPLEPAY, 1.0f, 0.85f);
            }
            bindFlashMap.put(mod, 1.0f);
            bindingModule = null;
            ModuleManager.getInstance().saveConfig();
            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TYPING, 0.88f, 0.6f);
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
                dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.TYPING, 0.95f + (float) (Math.random() * 0.15), 0.6f);
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
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

    private static String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACK";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            default -> {
                String name = GLFW.glfwGetKeyName(keyCode, 0);
                if (name != null && !name.isEmpty()) {
                    yield name.toUpperCase();
                }
                yield "KEY " + keyCode;
            }
        };
    }

    private static int applyAlpha(int color, float alphaFactor) {
        alphaFactor = Math.max(0.0f, Math.min(1.0f, alphaFactor));
        int a = (int) (((color >> 24) & 0xFF) * alphaFactor);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
