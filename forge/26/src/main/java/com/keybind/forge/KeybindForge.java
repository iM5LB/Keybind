package com.keybind.forge;

import com.keybind.mod.common.KeybindConstants;
import com.keybind.forge.client.KeybindForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(KeybindConstants.MOD_ID)
public final class KeybindForge {
    public static final Logger LOGGER = LoggerFactory.getLogger(KeybindConstants.MOD_ID);

    public KeybindForge(FMLJavaModLoadingContext context) {
        LOGGER.info("Keybind Forge initialized.");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            KeybindForgeNetwork.initialize();
            KeybindForgeClient.initialize();
        }
    }
}
