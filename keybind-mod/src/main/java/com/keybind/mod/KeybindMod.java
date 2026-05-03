package com.keybind.mod;

import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeybindMod implements ModInitializer {

    public static final String MOD_ID = "keybind-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("keybind", "main");
    public static final Identifier SYNC_CHANNEL = Identifier.fromNamespaceAndPath("keybind", "sync");

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        
        PayloadTypeRegistry.serverboundPlay().register(
                KeybindActionPayload.TYPE,
                KeybindActionPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                KeybindSyncPayload.TYPE,
                KeybindSyncPayload.STREAM_CODEC
        );

        LOGGER.info("Keybind Mod initialized.");
    }
}
