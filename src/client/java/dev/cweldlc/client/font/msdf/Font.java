package dev.cweldlc.client.font.msdf;

public class Font {
    private final MsdfFont font;
    private final float size;

    public Font(MsdfFont font, float size) {
        this.font = font;
        this.size = size;
    }

    public float height() {
        return this.size * 0.7f;
    }

    public float width(String text) {
        return this.font.getWidth(text, this.size);
    }

    public MsdfFont getFont() {
        return this.font;
    }

    public float getSize() {
        return this.size;
    }
}
