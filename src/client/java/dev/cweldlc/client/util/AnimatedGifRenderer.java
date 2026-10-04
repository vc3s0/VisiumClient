package dev.cweldlc.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnimatedGifRenderer {

    public static final AnimatedGifRenderer VISIUM_LOGO = new AnimatedGifRenderer(
            "visium_logo",
            "/assets/cweldlc/textures/gui/visium_final.gif",
            new File("/home/vc3s/Desktop/ALL/Visium final.gif"),
            128
    );

    private final String id;
    private final String resourcePath;
    private final File fallbackFile;
    private final int targetFrameSize;

    private final List<ResourceLocation> frameLocations = new ArrayList<>();
    private final List<Integer> frameDelays = new ArrayList<>();
    private final List<Integer> frameCumulativeTime = new ArrayList<>();
    private int totalDurationMs = 0;
    private volatile boolean isLoaded = false;
    private boolean isLoading = false;

    public AnimatedGifRenderer(String id, String resourcePath, File fallbackFile, int targetFrameSize) {
        this.id = id;
        this.resourcePath = resourcePath;
        this.fallbackFile = fallbackFile;
        this.targetFrameSize = targetFrameSize;
    }

    public synchronized void load() {
        if (isLoaded || isLoading) return;
        isLoading = true;

        CompletableFuture.runAsync(() -> {
            try {
                InputStream is = null;
                if (resourcePath != null) {
                    is = AnimatedGifRenderer.class.getResourceAsStream(resourcePath);
                }
                if (is == null && fallbackFile != null && fallbackFile.exists()) {
                    is = new FileInputStream(fallbackFile);
                }
                if (is == null) {
                    isLoading = false;
                    return;
                }

                ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
                ImageInputStream iis = ImageIO.createImageInputStream(is);
                reader.setInput(iis);

                int count = reader.getNumImages(true);
                List<NativeImage> nativeFrames = new ArrayList<>(count);
                List<Integer> delays = new ArrayList<>(count);

                for (int i = 0; i < count; i++) {
                    BufferedImage raw = reader.read(i);
                    int delayMs = 60; // default ~16fps

                    try {
                        IIOMetadata meta = reader.getImageMetadata(i);
                        String format = meta.getNativeMetadataFormatName();
                        org.w3c.dom.Node root = meta.getAsTree(format);
                        for (int j = 0; j < root.getChildNodes().getLength(); j++) {
                            org.w3c.dom.Node node = root.getChildNodes().item(j);
                            if (node.getNodeName().equalsIgnoreCase("GraphicControlExtension")) {
                                String d = ((IIOMetadataNode) node).getAttribute("delayTime");
                                if (d != null && !d.isEmpty()) {
                                    int val = Integer.parseInt(d) * 10;
                                    if (val > 0) delayMs = val;
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    delays.add(delayMs);

                    BufferedImage frameToUse;
                    if (targetFrameSize > 0 && (raw.getWidth() != targetFrameSize || raw.getHeight() != targetFrameSize)) {
                        frameToUse = new BufferedImage(targetFrameSize, targetFrameSize, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2d = frameToUse.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2d.drawImage(raw, 0, 0, targetFrameSize, targetFrameSize, null);
                        g2d.dispose();
                    } else {
                        frameToUse = raw;
                    }

                    int w = frameToUse.getWidth();
                    int h = frameToUse.getHeight();
                    NativeImage nativeImg = new NativeImage(w, h, false);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            nativeImg.setPixel(x, y, frameToUse.getRGB(x, y));
                        }
                    }
                    nativeFrames.add(nativeImg);
                }

                iis.close();
                is.close();

                // Register dynamic textures on Minecraft render thread
                Minecraft.getInstance().execute(() -> {
                    try {
                        int cumTime = 0;
                        for (int i = 0; i < nativeFrames.size(); i++) {
                            NativeImage img = nativeFrames.get(i);
                            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/anim_" + id + "/frame_" + i);
                            DynamicTexture tex = new DynamicTexture(img);
                            Minecraft.getInstance().getTextureManager().register(loc, tex);
                            frameLocations.add(loc);

                            int delay = delays.get(i);
                            frameDelays.add(delay);
                            frameCumulativeTime.add(cumTime);
                            cumTime += delay;
                        }
                        totalDurationMs = Math.max(cumTime, 1);
                        isLoaded = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                isLoading = false;
            }
        });
    }

    public ResourceLocation getCurrentFrame() {
        if (!isLoaded || frameLocations.isEmpty()) return null;
        if (frameLocations.size() == 1 || totalDurationMs <= 0) return frameLocations.get(0);

        long time = System.currentTimeMillis() % totalDurationMs;
        for (int i = frameCumulativeTime.size() - 1; i >= 0; i--) {
            if (time >= frameCumulativeTime.get(i)) {
                return frameLocations.get(i);
            }
        }
        return frameLocations.get(0);
    }

    public void render(GuiGraphics graphics, float x, float y, float width, float height, float radius, int color) {
        if (!isLoaded) {
            load();
            GlassRenderUtil.fillRoundedRect(graphics, x, y, width, height, radius, color & 0x25FFFFFF);
            return;
        }

        ResourceLocation current = getCurrentFrame();
        if (current != null) {
            GlassRenderUtil.drawRoundedTexture(graphics, current, x, y, width, height, radius, color);
        }
    }
}
