package dev.cweldlc;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CwelDLC implements ModInitializer {
    public static final String MOD_ID = "cweldlc";
    public static final String CLIENT_NAME = "VisiumClient";
    public static final String CLIENT_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Initializing {} Client v{} for Minecraft 1.21.4", MOD_ID, CLIENT_NAME, CLIENT_VERSION);
    }
}
