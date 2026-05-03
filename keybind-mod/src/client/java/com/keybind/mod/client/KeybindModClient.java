package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public class KeybindModClient implements ClientModInitializer {

    private KeybindManager keybindManager;

    @Override
    public void onInitializeClient() {
        keybindManager = new KeybindManager();
        keybindManager.registerAllKnownActions();

        // Register serverbound payload (client -> server: action triggers)
        PayloadTypeRegistry.serverboundPlay().register(
                KeybindActionPayload.TYPE,
                KeybindActionPayload.STREAM_CODEC
        );

        // Register clientbound payload (server -> client: action list sync)
        PayloadTypeRegistry.clientboundPlay().register(
                KeybindSyncPayload.TYPE,
                KeybindSyncPayload.STREAM_CODEC
        );

        // Listen for sync packets from the server
        ClientPlayNetworking.registerGlobalReceiver(KeybindSyncPayload.TYPE,
                (payload, context) -> {
                    String serverAddress = getServerAddress();
                    KeybindMod.LOGGER.info("Received sync from server: "
                            + payload.actions().size() + " actions");
                    context.client().execute(() -> {
                        keybindManager.onServerSync(serverAddress, payload.actions());
                    });
                }
        );

        // Clear keybinds on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            keybindManager.onDisconnect();
            KeybindMod.LOGGER.info("Disconnected — keybinds cleared.");
        });

        // Tick handler for key polling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            keybindManager.tick(client);
        });

        KeybindMod.LOGGER.info("Keybind Mod client initialized (server-synced mode).");
    }

    private String getServerAddress() {
        Minecraft client = Minecraft.getInstance();
        ServerData serverData = client.getCurrentServer();
        if (serverData != null) {
            return serverData.ip;
        }
        // Singleplayer or unknown
        return "singleplayer";
    }
}
