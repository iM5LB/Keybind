package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public class KeybindModClient implements ClientModInitializer {

    private KeybindManager keybindManager;

    @Override
    public void onInitializeClient() {
        KeybindMod.LOGGER.info("Initializing Keybind Mod client...");
        keybindManager = new KeybindManager();
        keybindManager.registerAllKnownActions();

        ClientPlayNetworking.registerGlobalReceiver(KeybindSyncPayload.ID,
                (payload, context) -> {
                    String serverAddress = getServerAddress();
                    context.client().execute(() -> {
                        keybindManager.onServerSync(serverAddress, payload.version(), payload.actions());
                    });
                }
        );

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            keybindManager.onDisconnect();
            KeybindMod.LOGGER.info("Disconnected — keybinds cleared.");
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                keybindManager.tick(client);
            }
        });

        KeybindMod.LOGGER.info("Keybind Mod client initialized.");
    }

    private String getServerAddress() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            return serverInfo.address;
        }
        // Singleplayer or unknown
        return "singleplayer";
    }
}
