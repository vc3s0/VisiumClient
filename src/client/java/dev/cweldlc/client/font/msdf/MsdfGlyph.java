package dev.cweldlc.client.font.msdf;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public final class MsdfGlyph {
    private final int code;
    private final float minU;
    private final float maxU;
    private final float minV;
    private final float maxV;
    private final float advance;
    private final float topPosition;
    private final float width;
    private final float height;

    public MsdfGlyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
        this.code = data.unicode();
        this.advance = data.advance();
        FontData.BoundsData atlasBounds = data.atlasBounds();
        if (atlasBounds != null) {
            this.minU = atlasBounds.left() / atlasWidth;
            this.maxU = atlasBounds.right() / atlasWidth;
            this.minV = 1.0f - atlasBounds.top() / atlasHeight;
            this.maxV = 1.0f - atlasBounds.bottom() / atlasHeight;
        } else {
            this.minU = 0.0f;
            this.maxU = 0.0f;
            this.minV = 0.0f;
            this.maxV = 0.0f;
        }
        FontData.BoundsData planeBounds = data.planeBounds();
        if (planeBounds != null) {
            this.width = planeBounds.right() - planeBounds.left();
            this.height = planeBounds.top() - planeBounds.bottom();
            this.topPosition = planeBounds.top();
        } else {
            this.topPosition = 0.0f;
            this.height = 0.0f;
            this.width = 0.0f;
        }
    }

    public float apply(Matrix4f matrix, VertexConsumer consumer, float size, float x, float y, float z, int color) {
        float w = this.width * size;
        float h = this.height * size;
        float adjustedY = y - this.topPosition * size;

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        consumer.addVertex(matrix, x, adjustedY, z).setUv(this.minU, this.minV).setColor(r, g, b, a);
        consumer.addVertex(matrix, x, adjustedY + h, z).setUv(this.minU, this.maxV).setColor(r, g, b, a);
        consumer.addVertex(matrix, x + w, adjustedY + h, z).setUv(this.maxU, this.maxV).setColor(r, g, b, a);
        consumer.addVertex(matrix, x + w, adjustedY, z).setUv(this.maxU, this.minV).setColor(r, g, b, a);

        return this.advance * size;
    }

    public float getWidth(float size) {
        return this.advance * size;
    }

    public int getCharCode() {
        return this.code;
    }
}
