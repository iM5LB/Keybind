package com.keybind.neoforge;

import com.keybind.mod.common.KeybindConstants;
import com.keybind.neoforge.client.KeybindNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = KeybindConstants.MOD_ID, dist = Dist.CLIENT)
public final class KeybindNeoForge {
    public static final Logger LOGGER = LoggerFactory.getLogger(KeybindConstants.MOD_ID);

    public KeybindNeoForge(IEventBus modBus) {
        LOGGER.info("Keybind NeoForge initialized.");
        KeybindNeoForgeClient.initialize(modBus);
    }
}
