package com.keybind.mod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Vanilla transport for client->server payloads. Works without Fabric API:
 * custom payload types are registered through codec mixins, so the vanilla
 * packet codec can serialize them.
 */
public final class KeybindNet {

    private KeybindNet() {
    }

    public static void sendAction(String action) {
        send(new KeybindActionPayload(action));
    }

    public static void sendCapture(String key) {
        send(new KeybindCapturePayload(key));
    }

    public static void sendHello() {
        send(new KeybindHelloPayload());
        // Announce plugin channels so servers that filter outgoing plugin
        // messages by client registration (e.g. Paper) will forward syncs.
        send(new RegisterChannelsPayload(CHANNELS));
    }

    private static final String CHANNELS =
            "keybind:sync" + '\0' + "keybind:capture" + '\0'
            + "keybind:main" + '\0' + "keybind:hello";

    private static void send(CustomPacketPayload payload) {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }
}
