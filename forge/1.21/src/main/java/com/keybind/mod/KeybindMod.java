package com.keybind.mod;

import com.keybind.mod.common.KeybindConstants;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KeybindMod {
    public static final String MOD_ID = KeybindConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, KeybindConstants.ACTION_CHANNEL_PATH);
    public static final ResourceLocation SYNC_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, KeybindConstants.SYNC_CHANNEL_PATH);
    public static final ResourceLocation CAPTURE_CHANNEL = ResourceLocation.fromNamespaceAndPath(MOD_ID, KeybindConstants.CAPTURE_CHANNEL_PATH);

    private KeybindMod() { }
}
