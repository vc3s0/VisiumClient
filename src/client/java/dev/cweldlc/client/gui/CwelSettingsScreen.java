package dev.cweldlc.client.gui;

import dev.cweldlc.CwelDLC;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.gui.components.GlassButton;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CwelSettingsScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CwelDLC.MOD_ID, "textures/gui/title/background.png"
    );

    private static final int TEX_WIDTH = 1920;
    private static final int TEX_HEIGHT = 1080;

    private final Screen parent;

    public static boolean liquidGlassBlur = true;
    public static boolean showFpsWidget = true;
    public static boolean showClockWidget = true;
    public static boolean customAnimations = true;

    public CwelSettingsScreen(Screen parent) {
        super(Component.literal("VisiumClient Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int btnW = 280;
        int btnH = 28;
        int spacing = 8;
        int startX = (this.width - btnW) / 2;
        int startY = (int) (this.height * 0.29f);

        // Theme Toggle
        this.addRenderableWidget(new GlassButton(
                startX, startY, btnW, btnH,
                Component.literal("Motyw UI: " + (ThemeManager.isDark() ? "§8CZARNY (OLED)" : "§fBIAŁY (CRYSTAL)")),
                btn -> {
                    ThemeManager.startRipple(btn.getX() + btn.getWidth() / 2.0f, btn.getY() + btn.getHeight() / 2.0f);
                    ThemeManager.toggleTheme();
                    btn.setMessage(Component.literal("Motyw UI: " + (ThemeManager.isDark() ? "§8CZARNY (OLED)" : "§fBIAŁY (CRYSTAL)")));
                    btn.withAccent(ThemeManager.isDark() ? 0xFF60A5FA : 0xFFF59E0B);
                }
        ).withAccent(ThemeManager.isDark() ? 0xFF60A5FA : 0xFFF59E0B).withRadius(10));

        // LiquidGlass Shaders
        this.addRenderableWidget(new GlassButton(
                startX, startY + (btnH + spacing), btnW, btnH,
                Component.literal("LiquidGlass Shaders: " + (liquidGlassBlur ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")),
                btn -> {
                    liquidGlassBlur = !liquidGlassBlur;
                    btn.setMessage(Component.literal("LiquidGlass Shaders: " + (liquidGlassBlur ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")));
                }
        ).withAccent(0xFFE2E8F0).withRadius(10));

        // Apple Clock Widget
        this.addRenderableWidget(new GlassButton(
                startX, startY + (btnH + spacing) * 2, btnW, btnH,
                Component.literal("Apple Clock Widget: " + (showClockWidget ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")),
                btn -> {
                    showClockWidget = !showClockWidget;
                    btn.setMessage(Component.literal("Apple Clock Widget: " + (showClockWidget ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")));
                }
        ).withAccent(0xFFD1D5DB).withRadius(10));

        // Fluid Glass Animations
        this.addRenderableWidget(new GlassButton(
                startX, startY + (btnH + spacing) * 3, btnW, btnH,
                Component.literal("Fluid Glass Animations: " + (customAnimations ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")),
                btn -> {
                    customAnimations = !customAnimations;
                    btn.setMessage(Component.literal("Fluid Glass Animations: " + (customAnimations ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")));
                }
        ).withAccent(0xFF6B7280).withRadius(10));

        // Back
        this.addRenderableWidget(new GlassButton(
                startX, startY + (btnH + spacing) * 4 + 10, btnW, btnH,
                Component.literal("Powrót"),
                btn -> this.minecraft.setScreen(this.parent)
        ).withAccent(0xFFEF4444).withRadius(10));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Intentionally empty.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        float scale = Math.max((float) this.width / TEX_WIDTH, (float) this.height / TEX_HEIGHT);
        int destW = (int) Math.ceil(TEX_WIDTH * scale);
        int destH = (int) Math.ceil(TEX_HEIGHT * scale);
        int destX = (this.width - destW) / 2;
        int destY = (this.height - destH) / 2;

        graphics.blit(
                RenderType::guiTextured,
                BACKGROUND_TEXTURE,
                destX, destY,
                0.0f, 0.0f,
                destW, destH,
                TEX_WIDTH, TEX_HEIGHT,
                TEX_WIDTH, TEX_HEIGHT
        );

        graphics.fill(0, 0, this.width, this.height, ThemeManager.getOverlayColor());
        ThemeManager.renderThemeRipple(graphics, this.width, this.height);

        int panelW = 340;
        int panelH = 275;
        int panelX = (this.width - panelW) / 2;
        int panelY = (int) (this.height * 0.20f);
        GlassRenderUtil.drawGlassPanel(graphics, panelX, panelY, panelW, panelH, 18, false, 0.0f);

        String title = "VisiumClient Settings";
        MsdfRenderer.renderCenteredText(
                Fonts.bold(),
                title,
                16.0f,
                ThemeManager.getTitleColor(),
                graphics.pose().last().pose(),
                this.width / 2.0f,
                panelY + 16,
                0.0f
        );

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
