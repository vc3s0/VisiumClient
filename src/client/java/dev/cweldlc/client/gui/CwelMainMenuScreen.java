package dev.cweldlc.client.gui;

import dev.cweldlc.CwelDLC;
import dev.cweldlc.client.font.msdf.Fonts;
import dev.cweldlc.client.font.msdf.MsdfRenderer;
import dev.cweldlc.client.gui.components.GlassButton;
import dev.cweldlc.client.gui.widgets.AppleClockWidget;
import dev.cweldlc.client.theme.ThemeManager;
import dev.cweldlc.client.util.GlassRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CwelMainMenuScreen extends Screen {

    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CwelDLC.MOD_ID, "textures/gui/title/background.png"
    );

    private static final int TEX_WIDTH = 1920;
    private static final int TEX_HEIGHT = 1080;

    private static boolean welcomePlayed = false;

    public CwelMainMenuScreen() {
        super(Component.literal("VisiumClient Main Menu"));
    }

    @Override
    protected void init() {
        super.init();

        if (!welcomePlayed) {
            welcomePlayed = true;
            dev.cweldlc.client.util.ClientSounds.play(dev.cweldlc.client.util.ClientSounds.WELCOME, 1.0f, 0.8f);
        }

        // 1. Apple-style Top Glass Widgets
        int widgetMargin = 16;
        int widgetWidth = 148;
        int widgetHeight = 44;

        // Apple Clock & Date Widget (Top-Right)
        this.addRenderableWidget(new AppleClockWidget(this.width - widgetWidth - widgetMargin, widgetMargin, widgetWidth, widgetHeight));

        // 2. LiquidGlass Center Menu Column (compact 200px width, lowered to 0.46f)
        int btnWidth = 200;
        int btnHeight = 28;
        int btnSpacing = 6;
        int startX = (this.width - btnWidth) / 2;
        int startY = (int) (this.height * 0.46f);

        // Singleplayer
        this.addRenderableWidget(new GlassButton(
                startX, startY, btnWidth, btnHeight,
                Component.literal("Singleplayer"),
                btn -> this.minecraft.setScreen(new SelectWorldScreen(this))
        ).withAccent(0xFFE2E8F0).withRadius(9));

        // Multiplayer
        this.addRenderableWidget(new GlassButton(
                startX, startY + (btnHeight + btnSpacing), btnWidth, btnHeight,
                Component.literal("Multiplayer"),
                btn -> this.minecraft.setScreen(new JoinMultiplayerScreen(this))
        ).withAccent(0xFFD1D5DB).withRadius(9));

        // Minecraft Options
        this.addRenderableWidget(new GlassButton(
                startX, startY + (btnHeight + btnSpacing) * 2, btnWidth, btnHeight,
                Component.literal("Options"),
                btn -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options))
        ).withAccent(0xFF6B7280).withRadius(9));

        // Quit Game
        this.addRenderableWidget(new GlassButton(
                startX, startY + (btnHeight + btnSpacing) * 3, btnWidth, btnHeight,
                Component.literal("Quit Game"),
                btn -> this.minecraft.setScreen(new ConfirmScreen(
                        confirmed -> {
                            if (confirmed) {
                                this.minecraft.stop();
                            } else {
                                this.minecraft.setScreen(this);
                            }
                        },
                        Component.literal("Quit VisiumClient"),
                        Component.literal("Czy na pewno chcesz wyjść z gry?"),
                        CommonComponents.GUI_PROCEED,
                        CommonComponents.GUI_CANCEL
                ))
        ).withAccent(0xFFEF4444).withRadius(9));
    }

    /**
     * Prevents Screen.render from rendering the default blurred dirt/panorama.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Intentionally empty. Custom background wallpaper is handled in render().
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 1. Draw custom background wallpaper covering full viewport
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

        // 2. Uniform clean background glass overlay (no dirty vertical gradient)
        graphics.fill(0, 0, this.width, this.height, ThemeManager.getOverlayColor());

        // 3. Fluid theme shockwave ripple
        ThemeManager.renderThemeRipple(graphics, this.width, this.height);

        // 4. Central frosted glass card container (compact 224px, smooth 18px radius)
        int btnWidth = 200;
        int cardPadding = 12;
        int cardWidth = btnWidth + cardPadding * 2;
        int cardHeight = (28 + 6) * 4 + cardPadding * 2 - 6;
        int cardX = (this.width - cardWidth) / 2;
        int cardY = (int) (this.height * 0.46f) - cardPadding;

        GlassRenderUtil.drawTranslucentGlassPanel(graphics, cardX, cardY, cardWidth, cardHeight, 18, 0x60);

        // 4. Client Branding above the central card
        renderBranding(graphics);

        // 5. Render child widgets (buttons, Apple widgets)
        super.render(graphics, mouseX, mouseY, delta);

        // 6. Footer metadata bar
        renderFooter(graphics);
    }

    private void renderBranding(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int titleY = (int) (this.height * 0.33f);

        // Compact Apple capsule pill for title (radius 19 = full pill curvature)
        int badgeWidth = 180;
        int badgeHeight = 38;
        int badgeX = centerX - badgeWidth / 2;
        GlassRenderUtil.drawTranslucentGlassPanel(graphics, badgeX, titleY, badgeWidth, badgeHeight, 19, 0x60);

        // Glowing "VisiumClient" Title via MSDF vector font (centered in capsule)
        MsdfRenderer.renderCenteredText(
                Fonts.roundBold(),
                "VisiumClient",
                20.0f,
                ThemeManager.getTitleColor(),
                graphics.pose().last().pose(),
                centerX,
                titleY + 11.5f,
                0.0f
        );
    }

    private void renderFooter(GuiGraphics graphics) {
        String watermark = "VisiumClient " + CwelDLC.CLIENT_VERSION + " (Fabric 1.21.4)";
        int watermarkColor = ThemeManager.isDark() ? 0x709CA3AF : 0x90475569;
        MsdfRenderer.renderText(
                Fonts.regular(),
                watermark,
                8.0f,
                watermarkColor,
                graphics.pose().last().pose(),
                18,
                this.height - 18,
                0.0f
        );

        String status = "Right Shift: ClickGUI";
        float statusW = Fonts.regular().getWidth(status, 8.0f);
        MsdfRenderer.renderText(
                Fonts.regular(),
                status,
                8.0f,
                watermarkColor,
                graphics.pose().last().pose(),
                this.width - statusW - 18,
                this.height - 18,
                0.0f
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.minecraft.setScreen(new dev.cweldlc.client.gui.clickgui.ClickGuiScreen(this));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
