package com.keybind.mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = "keybind-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Plugin messaging channel for client → server action triggers.
     * MUST match the "channel" value in the server plugin's config.yml (default: "keybind:main").
     */
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("keybind", "main");

    /**
     * Plugin messaging channel for server → client action list sync on join.
     * MUST match the "sync-channel" value in the server plugin's config.yml (default: "keybind:sync").
     */
    public static final Identifier SYNC_CHANNEL = Identifier.fromNamespaceAndPath("keybind", "sync");

    @Override
    public void onInitialize() {
        LOGGER.info("Keybind Mod initialized.");
    }
}
