package com.keybind.neoforge.network;

import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindCapturePayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class KeybindNeoForgeNetwork {
    private KeybindNeoForgeNetwork() { }

    public static void sendAction(String action) {
        ClientPacketDistributor.sendToServer(new KeybindActionPayload(action));
    }

    public static void sendCapture(String key) {
        ClientPacketDistributor.sendToServer(new KeybindCapturePayload(key));
    }
}
