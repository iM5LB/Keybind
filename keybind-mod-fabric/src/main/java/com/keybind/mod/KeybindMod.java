package com.keybind.mod;

import com.keybind.mod.common.KeybindConstants;
import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = KeybindConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(
            MOD_ID,
            KeybindConstants.ACTION_CHANNEL_PATH
    );
    public static final ResourceLocation SYNC_CHANNEL = ResourceLocation.fromNamespaceAndPath(
            MOD_ID,
            KeybindConstants.SYNC_CHANNEL_PATH
    );

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        
        PayloadTypeRegistry.playC2S().register(
                KeybindActionPayload.TYPE,
                KeybindActionPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                KeybindSyncPayload.TYPE,
                KeybindSyncPayload.STREAM_CODEC
        );

        LOGGER.info("Keybind initialized.");
    }
}
