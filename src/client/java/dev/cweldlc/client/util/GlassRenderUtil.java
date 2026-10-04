package dev.cweldlc.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.cweldlc.client.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class GlassRenderUtil {

    public static final ShaderProgram RECTANGLE_SHADER = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath("cweldlc", "core/rectangle/data"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            ShaderDefines.EMPTY
    );

    public static final ShaderProgram BORDER_SHADER = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath("cweldlc", "core/border/data"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            ShaderDefines.EMPTY
    );

    public static final ShaderProgram GRADIENT_RECTANGLE_SHADER = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath("cweldlc", "core/gradient_rectangle/data"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            ShaderDefines.EMPTY
    );

    public static final ShaderProgram ROUNDED_TEXTURE_SHADER = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath("cweldlc", "core/texture/data"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            ShaderDefines.EMPTY
    );

    /**
     * Draws an SDF-antialiased filled rounded rectangle with subpixel precision.
     */
    public static void fillRoundedRect(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
        if (width <= 0 || height <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        CompiledShaderProgram shader = RenderSystem.setShader(RECTANGLE_SHADER);
        if (shader == null) {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            graphics.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
            return;
        }

        shader.safeGetUniform("Size").set(width, height);
        shader.safeGetUniform("Radius").set(radius, radius, radius, radius);
        shader.safeGetUniform("Smoothness").set(0.6f);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        builder.addVertex(matrix, x, y, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        builder.addVertex(matrix, x, y + height, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        builder.addVertex(matrix, x + width, y + height, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        builder.addVertex(matrix, x + width, y, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a);

        MeshData meshData = builder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Draws an SDF-antialiased rounded outline (border) with subpixel precision and hairline control.
     */
    public static void drawRoundedOutline(GuiGraphics graphics, float x, float y, float width, float height, float radius, float thickness, int color) {
        // Borderless aesthetic - no borders rendered
    }

    /**
     * Draws an SDF-antialiased vertical gradient rounded rectangle.
     */
    public static void fillGradientRoundedRect(GuiGraphics graphics, float x, float y, float width, float height, float radius, int colorTop, int colorBottom) {
        if (width <= 0 || height <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        CompiledShaderProgram shader = RenderSystem.setShader(GRADIENT_RECTANGLE_SHADER);
        if (shader == null) {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            graphics.fillGradient((int) x, (int) y, (int) (x + width), (int) (y + height), colorTop, colorBottom);
            return;
        }

        shader.safeGetUniform("Size").set(width, height);
        shader.safeGetUniform("Radius").set(radius, radius, radius, radius);
        shader.safeGetUniform("Smoothness").set(0.6f);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        int aTop = (colorTop >> 24) & 0xFF, rTop = (colorTop >> 16) & 0xFF, gTop = (colorTop >> 8) & 0xFF, bTop = colorTop & 0xFF;
        int aBot = (colorBottom >> 24) & 0xFF, rBot = (colorBottom >> 16) & 0xFF, gBot = (colorBottom >> 8) & 0xFF, bBot = colorBottom & 0xFF;

        builder.addVertex(matrix, x, y, 0.0f).setUv(0.0f, 0.0f).setColor(rTop, gTop, bTop, aTop);
        builder.addVertex(matrix, x, y + height, 0.0f).setUv(0.0f, 1.0f).setColor(rBot, gBot, bBot, aBot);
        builder.addVertex(matrix, x + width, y + height, 0.0f).setUv(1.0f, 1.0f).setColor(rBot, gBot, bBot, aBot);
        builder.addVertex(matrix, x + width, y, 0.0f).setUv(1.0f, 0.0f).setColor(rTop, gTop, bTop, aTop);

        MeshData meshData = builder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Renders a clean Black / White themed panel matching RockReady's solid rounded aesthetic.
     */
    public static void drawGlassPanel(GuiGraphics graphics, int x, int y, int width, int height, int radius, boolean hovered, float hoverProgress) {
        drawGlassPanel(graphics, x, y, width, height, radius, hovered, hoverProgress, 1.0f);
    }

    public static void drawGlassPanel(GuiGraphics graphics, int x, int y, int width, int height, int radius, boolean hovered, float hoverProgress, float borderThickness) {
        // Solid antialiased rounded background (0x0C0C0C Dark mode) - borderless
        int baseColor = ThemeManager.getGlassBaseColor(hoverProgress);
        fillRoundedRect(graphics, x, y, width, height, radius, baseColor);
    }

    /**
     * Renders an antialiased translucent glass panel with custom alpha.
     */
    public static void drawTranslucentGlassPanel(GuiGraphics graphics, float x, float y, float width, float height, float radius, int alpha) {
        int clampedAlpha = Math.max(0, Math.min(255, alpha));
        int color = (clampedAlpha << 24) | 0x0C0C0C;
        fillRoundedRect(graphics, x, y, width, height, radius, color);
    }

    public static void drawTranslucentGlassPanel(GuiGraphics graphics, int x, int y, int width, int height, int radius, int alpha) {
        drawTranslucentGlassPanel(graphics, (float) x, (float) y, (float) width, (float) height, (float) radius, alpha);
    }

    public static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Draws an SDF-antialiased rounded texture (e.g. Album Art) with smooth rounded corners.
     */
    public static void drawRoundedTexture(GuiGraphics graphics, ResourceLocation texture, float x, float y, float width, float height, float radius) {
        drawRoundedTexture(graphics, texture, x, y, width, height, radius, 0xFFFFFFFF);
    }

    public static void drawRoundedTexture(GuiGraphics graphics, ResourceLocation texture, float x, float y, float width, float height, float radius, int color) {
        if (width <= 0 || height <= 0 || texture == null) return;

        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        try {
            net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(texture).setFilter(true, false);
        } catch (Exception ignored) {}

        RenderSystem.setShaderTexture(0, texture);

        CompiledShaderProgram shader = RenderSystem.setShader(ROUNDED_TEXTURE_SHADER);
        if (shader == null) {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, texture, (int) x, (int) y, 0.0f, 0.0f, (int) width, (int) height, (int) width, (int) height);
            graphics.flush();
            return;
        }

        shader.safeGetUniform("Size").set(width, height);
        shader.safeGetUniform("Radius").set(radius, radius, radius, radius);
        shader.safeGetUniform("Smoothness").set(0.6f);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        builder.addVertex(matrix, x, y, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        builder.addVertex(matrix, x, y + height, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        builder.addVertex(matrix, x + width, y + height, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        builder.addVertex(matrix, x + width, y, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a);

        MeshData meshData = builder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
