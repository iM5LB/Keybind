package com.keybind.mod.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Fabric API transport for client->server payloads. The Quilt build provides
 * its own implementation of this class using vanilla packets + mixins, so
 * {@link com.keybind.mod.client.KeybindManager} stays loader-agnostic.
 */
public final class KeybindNet {

    private KeybindNet() {
    }

    public static void sendAction(String action) {
        ClientPlayNetworking.send(new KeybindActionPayload(action));
    }

    public static void sendCapture(String key) {
        ClientPlayNetworking.send(new KeybindCapturePayload(key));
    }
}
