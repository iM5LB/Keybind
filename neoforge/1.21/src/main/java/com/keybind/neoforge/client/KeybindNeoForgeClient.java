package com.keybind.neoforge.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.client.KeybindManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class KeybindNeoForgeClient {
    private static final KeybindManager KEYBINDS = new KeybindManager();

    private KeybindNeoForgeClient() { }

    public static void initialize(IEventBus modBus) {
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

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            Object category = KeybindManager.getKeybindCategory();
            if (category instanceof KeyMapping.Category keybindCategory) {
                event.registerCategory(keybindCategory);
            }
        });

        modBus.addListener(KeybindMod::registerPayloads);
        KeybindMod.setSyncReceiver(payload -> KEYBINDS.onServerSync(serverAddress(), payload.version(), payload.actions()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> KEYBINDS.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> KEYBINDS.onDisconnect());
        KEYBINDS.registerAllKnownActions();
    }

    private static String serverAddress() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server == null ? "singleplayer" : server.ip;
    }
}
