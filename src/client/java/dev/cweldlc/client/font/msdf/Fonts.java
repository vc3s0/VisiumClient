package dev.cweldlc.client.font.msdf;

public final class Fonts {
    private static MsdfFont regular;
    private static MsdfFont medium;
    private static MsdfFont semiBold;
    private static MsdfFont bold;
    private static MsdfFont roundBold;

    private Fonts() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static MsdfFont regular() {
        if (regular == null) {
            regular = MsdfFont.builder().name("regular").atlas("regular").data("regular").build();
        }
        return regular;
    }

    public static MsdfFont medium() {
        if (medium == null) {
            medium = MsdfFont.builder().name("medium").atlas("medium").data("medium").build();
        }
        return medium;
    }

    public static MsdfFont semiBold() {
        if (semiBold == null) {
            semiBold = MsdfFont.builder().name("semibold").atlas("semibold").data("semibold").build();
        }
        return semiBold;
    }

    public static MsdfFont bold() {
        if (bold == null) {
            bold = MsdfFont.builder().name("bold").atlas("bold").data("bold").build();
        }
        return bold;
    }

    public static MsdfFont roundBold() {
        if (roundBold == null) {
            roundBold = MsdfFont.builder().name("roundbold").atlas("roundbold").data("roundbold").build();
        }
        return roundBold;
    }
}
