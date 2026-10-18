package com.keybind.neoforge;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.client.KeybindManager;
import com.keybind.mod.common.KeybindConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = KeybindConstants.MOD_ID, dist = Dist.CLIENT)
public final class KeybindNeoForge {
    public static final Logger LOGGER = LoggerFactory.getLogger(KeybindConstants.MOD_ID);

    public KeybindNeoForge(IEventBus modBus) {
        LOGGER.info("Keybind NeoForge initialized.");
        initializeClient(modBus);
        modBus.addListener(KeybindMod::registerPayloads);
    }

    private void initializeClient(IEventBus modBus) {
        KeybindManager keybinds = new KeybindManager();

        KeybindManager.setRegistrationHandler(new KeybindManager.RegistrationHandler() {
            @Override
            public void register(Minecraft client, KeyMapping mapping) {
                KeybindManager.injectKeyMapping(client, mapping);
            }

            @Override
            public void unregister(Minecraft client, KeyMapping mapping) {
                KeybindManager.removeKeyMapping(client, mapping);
            }
        });

        keybinds.registerAllKnownActions();

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            Object category = KeybindManager.getKeybindCategory();
            if (category instanceof KeyMapping.Category keybindCategory) {
                event.registerCategory(keybindCategory);
            }
            for (KeyMapping mapping : keybinds.getRegisteredMappings()) {
                event.register(mapping);
            }
        });

        KeybindMod.setSyncReceiver(payload -> keybinds.onServerSync(serverAddress(), payload.version(), payload.actions()));
        KeybindMod.setCaptureReceiver(keybinds::beginKeyCapture);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> keybinds.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> keybinds.onDisconnect());
    }

    private String serverAddress() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server == null ? "singleplayer" : server.ip;
    }
}
