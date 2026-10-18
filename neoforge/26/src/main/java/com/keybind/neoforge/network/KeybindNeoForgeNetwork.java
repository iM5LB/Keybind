package com.keybind.neoforge.network;

import net.minecraft.client.Minecraft;

public final class KeybindNeoForgeNetwork {
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
            client.player.connection.sendCommand("kbind capture " + key);
        }
    }
}
