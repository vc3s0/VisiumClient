package dev.cweldlc.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cweldlc.CwelDLC;
import dev.cweldlc.client.gui.clickgui.ClickGuiScreen;
import dev.cweldlc.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class CwelDLCClient implements ClientModInitializer {

    public static KeyMapping clickGuiKey;

    @Override
    public void onInitializeClient() {
        CwelDLC.LOGGER.info("[VisiumClient] Initializing client subsystems, LiquidGlass render engine & Apple widgets.");

        // Show Welcome Dialog before Minecraft completes launching
        dev.cweldlc.client.gui.welcome.WelcomeDialog.showWelcome();

        // Initialize Client Sounds
        dev.cweldlc.client.util.ClientSounds.init();

        // Preload Visium Animated GIF Logo
        dev.cweldlc.client.util.AnimatedGifRenderer.VISIUM_LOGO.load();

        // Register ClickGUI Keybind (Right Shift)
        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cweldlc.clickgui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.cweldlc.general"
        ));

        // Initialize Module Manager
        ModuleManager.getInstance();

        // Start Discord Rich Presence
        dev.cweldlc.client.util.DiscordRPC.start();

        // Tick counter for RPC updates (every ~5s @ 20tps)
        int[] rpcTickCounter = {0};

        // Client Tick Handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clickGuiKey.consumeClick()) {
                client.setScreen(new ClickGuiScreen());
            }
            ModuleManager.getInstance().onTick();

            // Update Discord RPC state every 5 seconds
            rpcTickCounter[0]++;
            if (rpcTickCounter[0] >= 100) {
                rpcTickCounter[0] = 0;
                String details, state;
                if (client.level != null && client.player != null) {
                    details = "Playing VisiumClient";
                    if (client.getCurrentServer() != null) {
                        state = "On: " + client.getCurrentServer().ip;
                    } else {
                        state = "Singleplayer";
                    }
                } else {
                    details = "In Main Menu";
                    state = "VisiumClient";
                }
                dev.cweldlc.client.util.DiscordRPC.updatePresence(details, state);
            }
        });

        // 2D HUD Rendering Handler
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            ModuleManager.getInstance().onRender2D(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        });

        // Shutdown hook: stop Discord RPC
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dev.cweldlc.client.util.DiscordRPC.stop();
        }, "DiscordRPC-Shutdown"));
    }
}
