package com.keybind.mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = "keybind-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    public static Identifier CHANNEL;
    public static Identifier SYNC_CHANNEL;

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        CHANNEL = Identifier.parse(CONFIG.channel);
        SYNC_CHANNEL = Identifier.parse(CONFIG.syncChannel);
        LOGGER.info("Keybind Mod initialized (Channel: {}, Sync: {})", CONFIG.channel, CONFIG.syncChannel);
    }
}
