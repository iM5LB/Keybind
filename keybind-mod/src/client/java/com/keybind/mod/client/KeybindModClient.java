package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class KeybindModClient implements ClientModInitializer {

    private KeybindConfigManager configManager;
    private KeybindManager keybindManager;

    @Override
    public void onInitializeClient() {
        configManager = new KeybindConfigManager();
        configManager.load();

        keybindManager = new KeybindManager(configManager);
        keybindManager.registerKeybinds();

        // Register tick handler to detect key presses
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            keybindManager.tick(client);
        });

        // Register the plugin channel for packet communication
        // The channel is registered when the player joins a server
        // that has the Keybind plugin installed

        KeybindMod.LOGGER.info("Keybind Mod client initialized with "
                + configManager.getBindings().size() + " bindings.");
    }
}
