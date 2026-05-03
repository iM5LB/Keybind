package com.keybind.mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = "keybind-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    /**
     * Plugin messaging channel for client → server action triggers.
     * MUST match the "channel" value in the server plugin's config.yml (default: "keybind:main").
     */
    public static Identifier CHANNEL;

    /**
     * Plugin messaging channel for server → client action list sync on join.
     * MUST match the "sync-channel" value in the server plugin's config.yml (default: "keybind:sync").
     */
    public static Identifier SYNC_CHANNEL;

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        CHANNEL = Identifier.parse(CONFIG.channel);
        SYNC_CHANNEL = Identifier.parse(CONFIG.syncChannel);
        LOGGER.info("Keybind Mod initialized with channel: {} and sync-channel: {}", CONFIG.channel, CONFIG.syncChannel);
    }
}
