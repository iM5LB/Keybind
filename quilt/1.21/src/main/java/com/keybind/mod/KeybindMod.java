package com.keybind.mod;

import com.keybind.mod.common.KeybindConstants;
import net.fabricmc.api.ModInitializer;
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
    public static final ResourceLocation CAPTURE_CHANNEL = ResourceLocation.fromNamespaceAndPath(
            MOD_ID,
            KeybindConstants.CAPTURE_CHANNEL_PATH
    );

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();
        // Payload codec registration happens via mixins (see
        // ClientboundPayloadCodecMixin / ServerboundPayloadCodecMixin) so no
        // Fabric API is required.
        LOGGER.info("Keybind initialized.");
    }
}
