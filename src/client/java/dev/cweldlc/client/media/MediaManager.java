package dev.cweldlc.client.media;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaManager {

    private static MediaManager instance;

    public static synchronized MediaManager getInstance() {
        if (instance == null) {
            instance = new MediaManager();
        }
        return instance;
    }

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "Visium-MediaWorker");
        t.setDaemon(true);
        return t;
    });

    private static final ResourceLocation DYNAMIC_COVER_LOC = ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/album_art_current");
    private static final ResourceLocation[] FALLBACK_COVERS = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/covers/cover_0.png"),
            ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/covers/cover_1.png"),
            ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/covers/cover_2.png"),
            ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/covers/cover_3.png"),
            ResourceLocation.fromNamespaceAndPath("cweldlc", "textures/gui/covers/cover_4.png")
    };

    private final File cacheDir = new File(System.getProperty("user.home") + "/.cache/cweldlc/covers");

    private String activePlayer = null;
    private String title = "zaklamana prawda";
    private String artist = "whyukanti";
    private String album = "zaklamana prawda";
    private String artUrl = "";
    private String trackId = "";
    private boolean isPlaying = false;
    private boolean hasExternalPlayer = false;
    private long durationMs = 89620;

    // Smooth Monotonic Wall-Clock Position Tracker
    private long basePositionMs = 0;
    private long syncTimestamp = System.currentTimeMillis();

    // Dynamic Album Cover Texture state
    private volatile boolean hasDynamicCover = false;
    private volatile String loadedTrackKey = "";
    private final AtomicBoolean isDownloadingCover = new AtomicBoolean(false);

    // Fallback playlist when no external player is active
    private static final String[][] FALLBACK_TRACKS = {
            {"zaklamana prawda", "whyukanti", "zaklamana prawda", "89620"},
            {"Resonance", "HOME", "Odyssey", "212000"},
            {"Memory Reboot", "VØJ & Narvent", "Memory Reboot", "102000"},
            {"After Dark", "Mr.Kitty", "Time", "259000"},
            {"Midnight City", "M83", "Hurry Up, We're Dreaming", "244000"}
    };
    private int fallbackIndex = 0;
    private boolean fallbackPlaying = true;

    // Visualizer Bars (5 channels)
    private final float[] spectrumBars = new float[]{0.2f, 0.4f, 0.3f, 0.5f, 0.2f};
    private long lastPollTime = 0;

    private static final String PYTHON_QUERY =
            "import subprocess, json, re\n" +
            "try:\n" +
            "  names = subprocess.check_output(['dbus-send', '--session', '--dest=org.freedesktop.DBus', '--type=method_call', '--print-reply', '/org/freedesktop/DBus', 'org.freedesktop.DBus.ListNames'], stderr=subprocess.DEVNULL, timeout=0.8).decode()\n" +
            "  players = [l.split('\"')[1] for l in names.splitlines() if 'org.mpris.MediaPlayer2' in l]\n" +
            "  if not players:\n" +
            "    print(json.dumps({'active': False}))\n" +
            "    exit(0)\n" +
            "  best = None\n" +
            "  for p in players:\n" +
            "    try:\n" +
            "      status_out = subprocess.check_output(['dbus-send', '--session', '--print-reply', f'--dest={p}', '/org/mpris/MediaPlayer2', 'org.freedesktop.DBus.Properties.Get', 'string:org.mpris.MediaPlayer2.Player', 'string:PlaybackStatus'], stderr=subprocess.DEVNULL, timeout=0.5).decode()\n" +
            "      status = 'Playing' if '\"Playing\"' in status_out else ('Paused' if '\"Paused\"' in status_out else 'Stopped')\n" +
            "      meta = subprocess.check_output(['dbus-send', '--session', '--print-reply', f'--dest={p}', '/org/mpris/MediaPlayer2', 'org.freedesktop.DBus.Properties.Get', 'string:org.mpris.MediaPlayer2.Player', 'string:Metadata'], stderr=subprocess.DEVNULL, timeout=0.5).decode()\n" +
            "      pos_out = subprocess.check_output(['dbus-send', '--session', '--print-reply', f'--dest={p}', '/org/mpris/MediaPlayer2', 'org.freedesktop.DBus.Properties.Get', 'string:org.mpris.MediaPlayer2.Player', 'string:Position'], stderr=subprocess.DEVNULL, timeout=0.5).decode()\n" +
            "      pos_m = re.search(r'int64\\s+(\\d+)', pos_out)\n" +
            "      pos_ms = int(pos_m.group(1)) // 1000 if pos_m else 0\n" +
            "      title_m = re.search(r'string \"xesam:title\"\\s+variant\\s+string \"([^\"]+)\"', meta)\n" +
            "      artist_m = re.search(r'string \"xesam:artist\"\\s+variant\\s+array \\[\\s+string \"([^\"]+)\"', meta)\n" +
            "      album_m = re.search(r'string \"xesam:album\"\\s+variant\\s+string \"([^\"]+)\"', meta)\n" +
            "      art_m = re.search(r'string \"mpris:artUrl\"\\s+variant\\s+string \"([^\"]+)\"', meta)\n" +
            "      trackid_m = re.search(r'string \"mpris:trackid\"\\s+variant\\s+(?:string|object path)\\s+\"([^\"]+)\"', meta)\n" +
            "      len_m = re.search(r'string \"mpris:length\"\\s+variant\\s+uint64\\s+(\\d+)', meta)\n" +
            "      title = title_m.group(1) if title_m else 'Unknown'\n" +
            "      artist = artist_m.group(1) if artist_m else 'Unknown'\n" +
            "      album = album_m.group(1) if album_m else ''\n" +
            "      art_url = art_m.group(1) if art_m else ''\n" +
            "      track_id = trackid_m.group(1) if trackid_m else ''\n" +
            "      dur_ms = int(len_m.group(1)) // 1000 if len_m else 180000\n" +
            "      data = {'active': True, 'player': p, 'title': title, 'artist': artist, 'album': album, 'artUrl': art_url, 'trackId': track_id, 'status': status, 'pos': pos_ms, 'len': dur_ms}\n" +
            "      if status == 'Playing' or best is None:\n" +
            "        best = data\n" +
            "        if status == 'Playing': break\n" +
            "    except Exception: continue\n" +
            "  if best:\n" +
            "    print(json.dumps(best))\n" +
            "  else:\n" +
            "    print(json.dumps({'active': False}))\n" +
            "except Exception as e:\n" +
            "  print(json.dumps({'active': False, 'error': str(e)}))\n";

    private MediaManager() {
        try {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        } catch (Exception ignored) {}
        pollMedia();
    }

    public void update(float dt) {
        long now = System.currentTimeMillis();
        // Poll every 850ms
        if (now - lastPollTime > 850) {
            lastPollTime = now;
            pollMedia();
        }

        // Handle fallback track looping
        if (!hasExternalPlayer && fallbackPlaying) {
            long curDur = Long.parseLong(FALLBACK_TRACKS[fallbackIndex][3]);
            if (now - syncTimestamp >= curDur) {
                fallbackIndex = (fallbackIndex + 1) % FALLBACK_TRACKS.length;
                basePositionMs = 0;
                syncTimestamp = now;
            }
            title = FALLBACK_TRACKS[fallbackIndex][0];
            artist = FALLBACK_TRACKS[fallbackIndex][1];
            album = FALLBACK_TRACKS[fallbackIndex][2];
            durationMs = curDur;
            isPlaying = true;

            String fbKey = (artist + " - " + title).trim();
            if (!fbKey.equals(this.loadedTrackKey) && !isDownloadingCover.get()) {
                triggerCoverDownload("", artist, title, fbKey);
            }
        } else if (!hasExternalPlayer) {
            isPlaying = false;
        }

        // Animate visualizer spectrum bars
        for (int i = 0; i < spectrumBars.length; i++) {
            float target;
            if (isPlaying) {
                double wave = Math.sin((now / 130.0) * (i * 0.7 + 1.2) + i * 1.4);
                double wave2 = Math.cos((now / 90.0) * (i * 0.5 + 0.8));
                target = (float) (Math.abs(wave * 0.6 + wave2 * 0.4) * 0.75 + 0.25);
            } else {
                target = 0.12f;
            }
            spectrumBars[i] += (target - spectrumBars[i]) * (1.0f - (float) Math.exp(-dt * 18.0f));
        }
    }

    private void pollMedia() {
        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("python3", "-c", PYTHON_QUERY);
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    process.waitFor();
                    String jsonStr = sb.toString().trim();
                    if (!jsonStr.isEmpty() && jsonStr.startsWith("{")) {
                        JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                        boolean active = obj.has("active") && obj.get("active").getAsBoolean();
                        if (active) {
                            String newTitle = obj.get("title").getAsString();
                            String newArtist = obj.get("artist").getAsString();
                            String newTrackId = obj.has("trackId") ? obj.get("trackId").getAsString() : "";
                            boolean trackChanged = !newTitle.equals(this.title) || (!newTrackId.isEmpty() && !newTrackId.equals(this.trackId));

                            this.hasExternalPlayer = true;
                            this.activePlayer = obj.get("player").getAsString();
                            this.title = newTitle;
                            this.artist = newArtist;
                            this.album = obj.has("album") ? obj.get("album").getAsString() : "";
                            this.trackId = newTrackId;
                            String status = obj.get("status").getAsString();
                            this.isPlaying = "Playing".equalsIgnoreCase(status);
                            this.durationMs = obj.get("len").getAsLong();

                            long polledPos = obj.get("pos").getAsLong();
                            long currentCalc = getPositionMs();
                            long delta = Math.abs(polledPos - currentCalc);

                            // Smooth synchronization: NEVER jump backward by ~1s on periodic poll
                            if (!this.isPlaying || trackChanged || delta > 2500 || polledPos < currentCalc - 2500) {
                                this.basePositionMs = polledPos;
                                this.syncTimestamp = System.currentTimeMillis();
                            } else {
                                this.basePositionMs = Math.max(currentCalc, polledPos);
                                this.syncTimestamp = System.currentTimeMillis();
                            }

                            String newArtUrl = obj.has("artUrl") ? obj.get("artUrl").getAsString() : "";
                            this.artUrl = newArtUrl;

                            String trackKey = (newArtist + " - " + newTitle).trim();
                            if (!trackKey.isEmpty() && (!this.hasDynamicCover || !trackKey.equals(this.loadedTrackKey))) {
                                triggerCoverDownload(newArtUrl, newArtist, newTitle, trackKey);
                            }
                            return;
                        }
                    }
                }
                // No external player active - keep playing fallback without wiping downloaded covers prematurely
                this.hasExternalPlayer = false;
            } catch (Exception e) {
                this.hasExternalPlayer = false;
            }
        });
    }

    private void scheduleFastPoll() {
        executor.schedule(this::pollMedia, 150, TimeUnit.MILLISECONDS);
        executor.schedule(this::pollMedia, 450, TimeUnit.MILLISECONDS);
    }

    private void triggerCoverDownload(String rawUrl, String artist, String title, String trackKey) {
        if (isDownloadingCover.getAndSet(true)) return;
        executor.submit(() -> {
            try {
                String resolvedUrl = resolveCoverUrl(rawUrl, artist, title);
                if (resolvedUrl != null && !resolvedUrl.isEmpty()) {
                    downloadAndRegisterCover(resolvedUrl, trackKey);
                }
            } catch (Exception ignored) {
            } finally {
                isDownloadingCover.set(false);
            }
        });
    }

    private String resolveCoverUrl(String rawUrl, String artist, String title) {
        if (rawUrl != null && !rawUrl.isEmpty()) {
            if (rawUrl.startsWith("spotify:image:")) {
                return "https://i.scdn.co/image/" + rawUrl.substring("spotify:image:".length());
            }
            if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://") || rawUrl.startsWith("file://")) {
                return rawUrl;
            }
        }

        // Automatic fallback web search for covers (Deezer / iTunes API)
        if (title != null && !title.isEmpty()) {
            try {
                String cleanArtist = (artist != null && !artist.isEmpty() && !artist.equalsIgnoreCase("Unknown")) ? artist.trim() : "";
                String cleanTitle = title.trim();

                // 1. Search Deezer API with Artist + Title
                if (!cleanArtist.isEmpty()) {
                    String q1 = URLEncoder.encode(cleanArtist + " " + cleanTitle, StandardCharsets.UTF_8);
                    String cover = fetchDeezerCover("https://api.deezer.com/search?q=" + q1 + "&limit=1");
                    if (cover != null && !cover.isEmpty()) return cover;
                }

                // 2. Search Deezer API with Title only
                String q2 = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8);
                String cover = fetchDeezerCover("https://api.deezer.com/search?q=" + q2 + "&limit=1");
                if (cover != null && !cover.isEmpty()) return cover;

                // 3. Search iTunes Search API
                String itunesUrl = "https://itunes.apple.com/search?term=" + q2 + "&entity=song&limit=1";
                cover = fetchItunesCover(itunesUrl);
                if (cover != null && !cover.isEmpty()) return cover;
            } catch (Exception ignored) {}
        }
        return "";
    }

    private String fetchDeezerCover(String apiUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("data") && json.getAsJsonArray("data").size() > 0) {
                    JsonObject first = json.getAsJsonArray("data").get(0).getAsJsonObject();
                    if (first.has("album")) {
                        JsonObject alb = first.getAsJsonObject("album");
                        if (alb.has("cover_big")) return alb.get("cover_big").getAsString();
                        if (alb.has("cover_medium")) return alb.get("cover_medium").getAsString();
                        if (alb.has("cover")) return alb.get("cover").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String fetchItunesCover(String apiUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("results") && json.getAsJsonArray("results").size() > 0) {
                    JsonObject first = json.getAsJsonArray("results").get(0).getAsJsonObject();
                    if (first.has("artworkUrl100")) {
                        String art = first.get("artworkUrl100").getAsString();
                        return art.replace("100x100bb.jpg", "600x600bb.jpg");
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void downloadAndRegisterCover(String urlStr, String trackKey) {
        try {
            BufferedImage src = null;
            String safeHash = Integer.toHexString(trackKey.hashCode());
            File cachedFile = new File(cacheDir, safeHash + ".png");

            if (cachedFile.exists() && cachedFile.length() > 0) {
                try {
                    src = ImageIO.read(cachedFile);
                } catch (Exception ignored) {
                    cachedFile.delete();
                }
            }

            if (src == null) {
                byte[] data = null;
                if (urlStr.startsWith("file://")) {
                    File file = new File(URI.create(urlStr));
                    if (file.exists()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            data = fis.readAllBytes();
                        }
                    }
                } else if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    try (InputStream in = conn.getInputStream()) {
                        data = in.readAllBytes();
                    }
                }

                if (data != null && data.length > 0) {
                    src = ImageIO.read(new ByteArrayInputStream(data));
                }
            }

            if (src != null) {
                // High-performance bilinear downscale to 128x128 with smoothed rounded corners
                int targetSize = 128;
                BufferedImage dest = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = dest.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // Apple Music styled rounded corner clipping (radius ~16px)
                g2.setClip(new RoundRectangle2D.Float(0, 0, targetSize, targetSize, 22.0f, 22.0f));
                g2.drawImage(src, 0, 0, targetSize, targetSize, null);
                g2.dispose();

                // Save to local cache
                try {
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    ImageIO.write(dest, "PNG", cachedFile);
                } catch (Exception ignored) {}

                // Convert to NativeImage for Minecraft GPU upload
                NativeImage nativeImg = new NativeImage(NativeImage.Format.RGBA, targetSize, targetSize, false);
                int[] pixels = dest.getRGB(0, 0, targetSize, targetSize, null, 0, targetSize);
                for (int y = 0; y < targetSize; y++) {
                    for (int x = 0; x < targetSize; x++) {
                        nativeImg.setPixel(x, y, pixels[y * targetSize + x]);
                    }
                }

                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.execute(() -> {
                        try {
                            if (mc.getTextureManager() != null) {
                                DynamicTexture dynTex = new DynamicTexture(nativeImg);
                                dynTex.upload();
                                mc.getTextureManager().register(DYNAMIC_COVER_LOC, dynTex);
                                this.hasDynamicCover = true;
                                this.loadedTrackKey = trackKey;
                            } else {
                                nativeImg.close();
                            }
                        } catch (Exception e) {
                            nativeImg.close();
                        }
                    });
                } else {
                    nativeImg.close();
                }
            }
        } catch (Exception ignored) {}
    }

    public void playPause() {
        this.basePositionMs = getPositionMs();
        this.syncTimestamp = System.currentTimeMillis();

        if (hasExternalPlayer && activePlayer != null) {
            sendDbusCommand("PlayPause");
            this.isPlaying = !this.isPlaying;
        } else {
            this.fallbackPlaying = !this.fallbackPlaying;
            this.isPlaying = this.fallbackPlaying;
        }
        scheduleFastPoll();
    }

    public void next() {
        this.basePositionMs = 0;
        this.syncTimestamp = System.currentTimeMillis();

        if (hasExternalPlayer && activePlayer != null) {
            sendDbusCommand("Next");
        } else {
            fallbackIndex = (fallbackIndex + 1) % FALLBACK_TRACKS.length;
            title = FALLBACK_TRACKS[fallbackIndex][0];
            artist = FALLBACK_TRACKS[fallbackIndex][1];
            album = FALLBACK_TRACKS[fallbackIndex][2];
            durationMs = Long.parseLong(FALLBACK_TRACKS[fallbackIndex][3]);
        }
        scheduleFastPoll();
    }

    public void previous() {
        this.basePositionMs = 0;
        this.syncTimestamp = System.currentTimeMillis();

        if (hasExternalPlayer && activePlayer != null) {
            sendDbusCommand("Previous");
        } else {
            fallbackIndex = (fallbackIndex - 1 + FALLBACK_TRACKS.length) % FALLBACK_TRACKS.length;
            title = FALLBACK_TRACKS[fallbackIndex][0];
            artist = FALLBACK_TRACKS[fallbackIndex][1];
            album = FALLBACK_TRACKS[fallbackIndex][2];
            durationMs = Long.parseLong(FALLBACK_TRACKS[fallbackIndex][3]);
        }
        scheduleFastPoll();
    }

    public void seekTo(float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        long prevMs = getPositionMs();
        long targetMs = (long) (durationMs * progress);
        this.basePositionMs = targetMs;
        this.syncTimestamp = System.currentTimeMillis();

        if (hasExternalPlayer && activePlayer != null) {
            final long targetMicroseconds = targetMs * 1000L;
            final long relativeMicroseconds = (targetMs - prevMs) * 1000L;
            executor.submit(() -> {
                try {
                    if (trackId != null && !trackId.isEmpty()) {
                        new ProcessBuilder(
                                "dbus-send",
                                "--session",
                                "--type=method_call",
                                "--dest=" + activePlayer,
                                "/org/mpris/MediaPlayer2",
                                "org.mpris.MediaPlayer2.Player.SetPosition",
                                "objpath:" + trackId,
                                "int64:" + targetMicroseconds
                        ).start().waitFor();
                    } else {
                        // Relative seek fallback
                        new ProcessBuilder(
                                "dbus-send",
                                "--session",
                                "--type=method_call",
                                "--dest=" + activePlayer,
                                "/org/mpris/MediaPlayer2",
                                "org.mpris.MediaPlayer2.Player.Seek",
                                "int64:" + relativeMicroseconds
                        ).start().waitFor();
                    }
                } catch (Exception ignored) {}
            });
        }
        scheduleFastPoll();
    }

    public void seekOffset(long offsetSeconds) {
        long current = getPositionMs();
        long targetMs = Math.max(0, Math.min(durationMs, current + (offsetSeconds * 1000L)));
        seekTo((float) targetMs / (float) Math.max(1, durationMs));
    }

    public long getPositionMs() {
        if (hasExternalPlayer) {
            if (!isPlaying || durationMs <= 0) return basePositionMs;
            long elapsed = System.currentTimeMillis() - syncTimestamp;
            return Math.min(durationMs, Math.max(0, basePositionMs + elapsed));
        } else {
            if (!fallbackPlaying || durationMs <= 0) return basePositionMs;
            long elapsed = System.currentTimeMillis() - syncTimestamp;
            return Math.min(durationMs, Math.max(0, (basePositionMs + elapsed) % durationMs));
        }
    }

    public float getProgress() {
        if (durationMs <= 0) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) getPositionMs() / (float) durationMs));
    }

    public String getFormattedPosition() {
        long cur = getPositionMs();
        long sec = (cur / 1000) % 60;
        long min = (cur / 60000);
        return String.format("%d:%02d", min, sec);
    }

    public String getFormattedDuration() {
        long sec = (durationMs / 1000) % 60;
        long min = (durationMs / 60000);
        return String.format("%d:%02d", min, sec);
    }

    public float[] getSpectrumBars() {
        return spectrumBars;
    }

    public boolean hasExternalPlayer() {
        return hasExternalPlayer;
    }

    public String getPlayerName() {
        if (!hasExternalPlayer || activePlayer == null) return "Lo-Fi Beats";
        if (activePlayer.contains("spotify")) return "Spotify";
        if (activePlayer.contains("chromium") || activePlayer.contains("chrome")) return "Chrome";
        if (activePlayer.contains("firefox")) return "Firefox";
        if (activePlayer.contains("vlc")) return "VLC";
        return "Media";
    }

    private void sendDbusCommand(String method) {
        if (activePlayer == null || activePlayer.isEmpty()) return;
        executor.submit(() -> {
            try {
                new ProcessBuilder(
                        "dbus-send",
                        "--session",
                        "--type=method_call",
                        "--dest=" + activePlayer,
                        "/org/mpris/MediaPlayer2",
                        "org.mpris.MediaPlayer2.Player." + method
                ).start().waitFor();
            } catch (Exception ignored) {}
        });
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public boolean isPlaying() {
        return hasExternalPlayer ? isPlaying : fallbackPlaying;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public ResourceLocation getAlbumArtLocation() {
        if (hasDynamicCover) {
            return DYNAMIC_COVER_LOC;
        }
        if (fallbackPlaying || hasExternalPlayer) {
            int idx = Math.abs(fallbackIndex) % FALLBACK_COVERS.length;
            return FALLBACK_COVERS[idx];
        }
        return null;
    }
}
