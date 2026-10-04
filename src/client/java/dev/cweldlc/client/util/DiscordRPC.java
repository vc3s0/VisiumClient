package dev.cweldlc.client.util;

import net.minecraft.client.Minecraft;

import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordRPC {

    // Discord Application ID — uses Ekspresowy Bot's application
    private static final String CLIENT_ID = "1556299557791469598";
    private static final long START_TIME = System.currentTimeMillis();

    private static SocketChannel socket;
    private static final AtomicBoolean connected = new AtomicBoolean(false);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DiscordRPC");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> heartbeatTask;
    private static int heartbeatInterval = 30000;
    private static long lastHeartbeat = 0;
    private static int nonce = 1;

    // Current state
    private static volatile String currentDetails = "Playing Minecraft";
    private static volatile String currentState = "In Menu";

    public static void start() {
        if (running.getAndSet(true)) return;
        scheduler.submit(DiscordRPC::connectLoop);
    }

    public static void stop() {
        running.set(false);
        if (heartbeatTask != null) heartbeatTask.cancel(true);
        disconnect();
    }

    public static void updatePresence(String details, String state) {
        currentDetails = details;
        currentState = state;
        if (connected.get()) {
            scheduler.submit(DiscordRPC::sendPresence);
        }
    }

    private static void connectLoop() {
        while (running.get()) {
            if (!connected.get()) {
                try {
                    connect();
                } catch (Exception e) {
                    connected.set(false);
                    closeSocket();
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void connect() throws Exception {
        String runtimeDir = System.getenv("XDG_RUNTIME_DIR");
        if (runtimeDir == null) runtimeDir = System.getProperty("java.io.tmpdir");

        SocketChannel ch = null;
        for (int i = 0; i < 10; i++) {
            try {
                ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                ch.connect(UnixDomainSocketAddress.of(runtimeDir + "/discord-ipc-" + i));
                break;
            } catch (Exception ignored) {
                if (ch != null) { try { ch.close(); } catch (Exception ignored2) {} }
                ch = null;
            }
        }
        if (ch == null) return;
        socket = ch;

        // Handshake
        sendFrame(socket, 0, "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}");

        // Read READY
        String resp = readFrame(socket);
        if (resp == null || !resp.contains("READY")) {
            closeSocket();
            return;
        }

        // Parse heartbeat_interval if present
        if (resp.contains("heartbeat_interval")) {
            try {
                int idx = resp.indexOf("heartbeat_interval");
                int start = resp.indexOf(':', idx) + 1;
                int end = start;
                while (end < resp.length() && (Character.isDigit(resp.charAt(end)) || resp.charAt(end) == ' ')) end++;
                heartbeatInterval = Integer.parseInt(resp.substring(start, end).trim());
            } catch (Exception ignored) {}
        }

        connected.set(true);
        sendPresence();

        // Schedule periodic presence updates and heartbeat
        if (heartbeatTask != null) heartbeatTask.cancel(false);
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (!connected.get()) return;
            try {
                // Drain any incoming frames
                drainIncoming();
                // Update presence with current game state
                updateCurrentState();
                sendPresence();
            } catch (Exception e) {
                connected.set(false);
                closeSocket();
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    private static void updateCurrentState() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            if (mc.level != null && mc.player != null) {
                String serverAddr = "Singleplayer";
                if (mc.getCurrentServer() != null) {
                    serverAddr = mc.getCurrentServer().ip;
                }
                currentDetails = "Playing VisiumClient";
                currentState = "On: " + serverAddr;
            } else {
                currentDetails = "In Main Menu";
                currentState = "VisiumClient";
            }
        } catch (Exception ignored) {}
    }

    private static void sendPresence() {
        if (!connected.get() || socket == null) return;
        try {
            long elapsed = (System.currentTimeMillis() - START_TIME) / 1000L;
            String json = "{"
                + "\"cmd\":\"SET_ACTIVITY\","
                + "\"args\":{"
                    + "\"pid\":" + ProcessHandle.current().pid() + ","
                    + "\"activity\":{"
                        + "\"details\":\"" + escape(currentDetails) + "\","
                        + "\"state\":\"" + escape(currentState) + "\","
                        + "\"timestamps\":{\"start\":" + (START_TIME / 1000L) + "},"
                        + "\"assets\":{"
                            + "\"large_image\":\"visiumclient\","
                            + "\"large_text\":\"VisiumClient\""
                        + "},"
                        + "\"type\":0"
                    + "}"
                + "},"
                + "\"nonce\":\"" + (nonce++) + "\""
            + "}";
            sendFrame(socket, 1, json);
        } catch (Exception e) {
            connected.set(false);
            closeSocket();
        }
    }

    private static void drainIncoming() {
        if (socket == null || !socket.isConnected()) return;
        try {
            socket.configureBlocking(false);
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            while (socket.read(header) > 0) {
                if (header.remaining() == 0) {
                    header.flip();
                    int len = header.getInt(4);
                    ByteBuffer body = ByteBuffer.allocate(len);
                    socket.read(body);
                    header.clear();
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { socket.configureBlocking(true); } catch (Exception ignored) {}
        }
    }

    private static void sendFrame(SocketChannel ch, int opcode, String json) throws Exception {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(data.length);
        buf.put(data);
        buf.flip();
        ch.write(buf);
    }

    private static String readFrame(SocketChannel ch) {
        try {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            int read = 0;
            while (read < 8) {
                int r = ch.read(header);
                if (r < 0) return null;
                read += r;
            }
            header.flip();
            header.getInt(); // opcode
            int len = header.getInt();
            ByteBuffer body = ByteBuffer.allocate(len);
            read = 0;
            while (read < len) {
                int r = ch.read(body);
                if (r < 0) return null;
                read += r;
            }
            body.flip();
            return new String(body.array(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static void disconnect() {
        connected.set(false);
        closeSocket();
    }

    private static void closeSocket() {
        if (socket != null) {
            try { socket.close(); } catch (Exception ignored) {}
            socket = null;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
