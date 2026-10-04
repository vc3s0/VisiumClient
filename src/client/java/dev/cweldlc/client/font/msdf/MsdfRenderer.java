package dev.cweldlc.client.font.msdf;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class MsdfRenderer {
    public static final ShaderProgram MSDF_FONT_SHADER = new ShaderProgram(
            ResourceLocation.fromNamespaceAndPath("cweldlc", "core/msdf_font/data"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            ShaderDefines.EMPTY
    );

    private MsdfRenderer() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void renderText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float x, float y, float z) {
        renderText(font, text, size, color, matrix, x, y, z, false, 0.0f, 1.0f, 0.0f);
    }

    public static void renderText(MsdfFont font, net.minecraft.util.FormattedCharSequence sequence, float size, int color, Matrix4f matrix, float x, float y, float z) {
        if (sequence == null) return;

        float thickness = 0.05f;
        float smoothness = 0.5f;
        float spacing = 0.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(0, font.getTextureId());
        CompiledShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER);
        if (shader == null) {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            return;
        }

        AbstractUniform uRange = shader.safeGetUniform("Range");
        uRange.set(font.getAtlas().range());
        AbstractUniform uThickness = shader.safeGetUniform("Thickness");
        uThickness.set(thickness);
        AbstractUniform uSmoothness = shader.safeGetUniform("Smoothness");
        uSmoothness.set(smoothness);
        AbstractUniform uEnableFadeout = shader.safeGetUniform("EnableFadeout");
        uEnableFadeout.set(0);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        font.applyGlyphs(matrix, builder, sequence, size, thickness * 0.5f * size, spacing, x - 0.75f, y + size * 0.7f, z, color);
        MeshData meshData = builder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void renderCenteredText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float centerX, float y, float z) {
        float width = font.getWidth(text, size);
        renderText(font, text, size, color, matrix, centerX - width / 2.0f, y, z);
    }

    public static void renderText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float x, float y, float z,
                                  boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
        if (text == null || text.isEmpty()) return;

        float thickness = 0.05f;
        float smoothness = 0.5f;
        float spacing = 0.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(0, font.getTextureId());
        CompiledShaderProgram shader = RenderSystem.setShader(MSDF_FONT_SHADER);
        if (shader == null) {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            return;
        }

        AbstractUniform uRange = shader.safeGetUniform("Range");
        uRange.set(font.getAtlas().range());

        AbstractUniform uThickness = shader.safeGetUniform("Thickness");
        uThickness.set(thickness);

        AbstractUniform uSmoothness = shader.safeGetUniform("Smoothness");
        uSmoothness.set(smoothness);

        AbstractUniform uEnableFadeout = shader.safeGetUniform("EnableFadeout");
        uEnableFadeout.set(enableFadeout ? 1 : 0);

        AbstractUniform uFadeoutStart = shader.safeGetUniform("FadeoutStart");
        uFadeoutStart.set(fadeoutStart);

        AbstractUniform uFadeoutEnd = shader.safeGetUniform("FadeoutEnd");
        uFadeoutEnd.set(fadeoutEnd);

        AbstractUniform uMaxWidth = shader.safeGetUniform("MaxWidth");
        uMaxWidth.set(maxWidth);

        AbstractUniform uTextPosX = shader.safeGetUniform("TextPosX");
        uTextPosX.set(x);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        font.applyGlyphs(matrix, builder, text, size, thickness * 0.5f * size, spacing, x - 0.75f, y + size * 0.7f, z, color);
        MeshData meshData = builder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
