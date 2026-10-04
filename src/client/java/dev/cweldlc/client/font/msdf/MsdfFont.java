package dev.cweldlc.client.font.msdf;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class MsdfFont {
    private static final Gson GSON = new Gson();

    private final String name;
    private final AbstractTexture texture;
    private final ResourceLocation atlasLocation;
    private final FontData.AtlasData atlas;
    private final FontData.MetricsData metrics;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Integer, Map<Integer, Float>> kernings;
    private final ConcurrentHashMap<Long, Float> widthCache = new ConcurrentHashMap<>();

    private MsdfFont(String name, AbstractTexture texture, ResourceLocation atlasLocation, FontData.AtlasData atlas, FontData.MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
        this.name = name;
        this.texture = texture;
        this.atlasLocation = atlasLocation;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
    }

    public int getTextureId() {
        return this.texture.getId();
    }

    public ResourceLocation getAtlasLocation() {
        return this.atlasLocation;
    }

    public Font getFont(float size) {
        return new Font(this, size);
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int initialColor) {
        int currentColor = initialColor;
        int prevChar = -1;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                currentColor = getFormattingColor(code, initialColor);
                i++;
                continue;
            }
            MsdfGlyph glyph = this.glyphs.get((int) c);
            if (glyph == null) continue;
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault((int) c, 0.0f) * size;
            }
            x += glyph.apply(matrix, consumer, size, x, y, z, currentColor) + thickness + spacing;
            prevChar = c;
        }
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, net.minecraft.util.FormattedCharSequence sequence, float size, float thickness, float spacing, float startX, float y, float z, int defaultColor) {
        float[] currentX = new float[]{startX};
        int[] prevChar = new int[]{-1};
        int alpha = (defaultColor >> 24) & 0xFF;
        if (alpha == 0) alpha = 0xFF;
        int finalAlpha = alpha;

        sequence.accept((index, style, codePoint) -> {
            MsdfGlyph glyph = this.glyphs.get(codePoint);
            if (glyph == null) return true;

            int rgb = 0xFFFFFF;
            if (style.getColor() != null) {
                rgb = style.getColor().getValue();
            }
            int color = (finalAlpha << 24) | (rgb & 0x00FFFFFF);

            Map<Integer, Float> kerning = this.kernings.get(prevChar[0]);
            if (kerning != null) {
                currentX[0] += kerning.getOrDefault(codePoint, 0.0f) * size;
            }
            currentX[0] += glyph.apply(matrix, consumer, size, currentX[0], y, z, color) + thickness + spacing;
            prevChar[0] = codePoint;
            return true;
        });
    }

    public float getWidth(net.minecraft.util.FormattedCharSequence sequence, float size) {
        if (sequence == null) return 0.0f;
        float[] width = new float[]{0.0f};
        int[] prevChar = new int[]{-1};
        sequence.accept((index, style, codePoint) -> {
            MsdfGlyph glyph = this.glyphs.get(codePoint);
            if (glyph == null) return true;
            Map<Integer, Float> kerning = this.kernings.get(prevChar[0]);
            if (kerning != null) {
                width[0] += kerning.getOrDefault(codePoint, 0.0f) * size;
            }
            width[0] += glyph.getWidth(size) + 0.25f;
            prevChar[0] = codePoint;
            return true;
        });
        return width[0];
    }

    private static int getFormattingColor(char code, int defaultColor) {
        int alpha = (defaultColor >> 24) & 0xFF;
        int rgb = switch (code) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> defaultColor & 0x00FFFFFF;
        };
        return (alpha << 24) | rgb;
    }

    public float getWidth(String text, float size) {
        if (text == null || text.isEmpty()) return 0.0f;
        long key = ((long) text.hashCode() & 0xFFFFFFFFL) ^ ((long) Float.floatToIntBits(size) << 32);
        Float cached = this.widthCache.get(key);
        if (cached != null) {
            return cached;
        }
        float w = computeWidth(text, size);
        this.widthCache.put(key, w);
        return w;
    }

    private float computeWidth(String text, float size) {
        int prevChar = -1;
        float width = 0.0f;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            MsdfGlyph glyph = this.glyphs.get((int) c);
            if (glyph == null) continue;
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault((int) c, 0.0f) * size;
            }
            width += glyph.getWidth(size) + 0.25f;
            prevChar = c;
        }
        return width;
    }

    public String getName() {
        return this.name;
    }

    public FontData.AtlasData getAtlas() {
        return this.atlas;
    }

    public FontData.MetricsData getMetrics() {
        return this.metrics;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "?";
        private ResourceLocation dataIdentifier;
        private ResourceLocation atlasIdentifier;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder data(String dataFileName) {
            this.dataIdentifier = ResourceLocation.fromNamespaceAndPath("cweldlc", "fonts/msdf/" + dataFileName + ".json");
            return this;
        }

        public Builder atlas(String atlasFileName) {
            this.atlasIdentifier = ResourceLocation.fromNamespaceAndPath("cweldlc", "fonts/msdf/" + atlasFileName + ".png");
            return this;
        }

        public MsdfFont build() {
            FontData data;
            try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(this.dataIdentifier)) {
                data = GSON.fromJson(reader, FontData.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read font data file: " + this.dataIdentifier, e);
            }

            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(this.atlasIdentifier);
            RenderSystem.recordRenderCall(() -> texture.setFilter(true, false));

            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
                    .collect(Collectors.toMap(FontData.GlyphData::unicode, g -> new MsdfGlyph(g, aWidth, aHeight)));

            Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
            if (data.kernings() != null) {
                data.kernings().forEach(k -> {
                    kernings.computeIfAbsent(k.leftChar(), x -> new HashMap<>()).put(k.rightChar(), k.advance());
                });
            }

            return new MsdfFont(this.name, texture, this.atlasIdentifier, data.atlas(), data.metrics(), glyphs, kernings);
        }
    }
}
