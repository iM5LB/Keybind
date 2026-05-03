package com.keybind.mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = "keybind-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("keybind", "main");
    public static final Identifier SYNC_CHANNEL = Identifier.fromNamespaceAndPath("keybind", "sync");

    @Override
    public void onInitialize() {
        LOGGER.info("Keybind Mod initialized.");
    }
}
