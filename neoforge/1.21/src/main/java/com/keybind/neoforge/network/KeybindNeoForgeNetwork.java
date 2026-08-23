package com.keybind.neoforge.network;

import com.keybind.mod.network.KeybindActionPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class KeybindNeoForgeNetwork {
    private KeybindNeoForgeNetwork() { }

    public static void sendAction(String action) {
        ClientPacketDistributor.sendToServer(new KeybindActionPayload(action));
    }
}
