package com.keybind.neoforge.network;

import com.keybind.mod.KeybindMod;

import com.keybind.mod.network.KeybindHelloPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class KeybindNeoForgeNetwork {
    private static Object lastHelloConnection;

    private KeybindNeoForgeNetwork() { }

    public static void sendAction(String action) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null) {
            client.player.connection.sendCommand("kbind " + action);
        }
    }

    public static void sendCapture(String key) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null) {
            // Use capture command - server plugin now supports this
            client.player.connection.sendCommand("kbind capture " + key);
        }
    }

    /**
     * Announces the mod to the server once per connection so the plugin only
     * sends sync/capture packets to clients that actually have it.
     */
    public static void maybeSendHello() {
        Minecraft client = Minecraft.getInstance();
        Object connection = client.getConnection();
        if (connection == null || client.player == null) {
            lastHelloConnection = null;
            return;
        }
        if (connection == lastHelloConnection) {
            return;
        }
        try {
            ClientPacketDistributor.sendToServer(new KeybindHelloPayload());
            lastHelloConnection = connection;
        } catch (Throwable t) {
            KeybindMod.LOGGER.warn("Failed to send keybind hello, will retry: {}", t.toString());
        }
    }
}
