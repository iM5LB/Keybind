package com.keybind.mod;

import com.keybind.mod.common.KeybindConstants;
import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = KeybindConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    public static final Identifier CHANNEL = Identifier.of(
            MOD_ID,
            KeybindConstants.ACTION_CHANNEL_PATH
    );
    public static final Identifier SYNC_CHANNEL = Identifier.of(
            MOD_ID,
            KeybindConstants.SYNC_CHANNEL_PATH
    );

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        
        PayloadTypeRegistry.playC2S().register(
                KeybindActionPayload.ID,
                KeybindActionPayload.CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                KeybindSyncPayload.ID,
                KeybindSyncPayload.CODEC
        );

        LOGGER.info("Keybind initialized.");
    }
}
