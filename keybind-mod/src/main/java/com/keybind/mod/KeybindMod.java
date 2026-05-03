package com.keybind.mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = "keybind-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Plugin messaging channel — must match the server plugin's channel. */
    public static final Identifier CHANNEL = new Identifier("keybind", "main");

    @Override
    public void onInitialize() {
        LOGGER.info("Keybind Mod initialized.");
    }
}
