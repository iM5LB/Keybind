package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.network.KeybindCapturePayload;
import com.keybind.mod.network.KeybindNet;
import com.keybind.mod.network.KeybindSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public class KeybindModClient implements ClientModInitializer {

    private static KeybindModClient instance;

    private KeybindManager keybindManager;
    private boolean helloSent;
    private Object lastConnection;
    private boolean wasInGame;

    @Override
    public void onInitializeClient() {
        instance = this;
        KeybindMod.LOGGER.info("Initializing Keybind Mod client...");
        KeybindManager.getKeybindCategory();
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

        keybindManager = new KeybindManager();
        keybindManager.registerAllKnownActions();
        KeybindMod.LOGGER.info("Keybind Mod client initialized.");
    }

    /** Called every client tick from MinecraftTickMixin. */
    public static void onClientTick(Minecraft client) {
        KeybindModClient self = instance;
        if (self == null || self.keybindManager == null) {
            return;
        }

        boolean inGame = client.player != null;
        if (!inGame && self.wasInGame) {
            self.keybindManager.onDisconnect();
            self.helloSent = false;
            self.lastConnection = null;
            KeybindMod.LOGGER.info("Disconnected — keybinds cleared.");
        }
        self.wasInGame = inGame;

        Object connection = client.getConnection();
        if (connection != self.lastConnection) {
            self.lastConnection = connection;
            self.helloSent = false;
        }
        if (!self.helloSent && inGame && connection != null) {
            KeybindNet.sendHello();
            self.helloSent = true;
            KeybindMod.LOGGER.info("Sent keybind hello to server.");
        }

        self.keybindManager.tick(client);
    }

    /** Called from ClientPacketListenerPayloadMixin on the netty thread. */
    public static void onSyncPayload(KeybindSyncPayload payload) {
        Minecraft.getInstance().execute(() -> {
            KeybindModClient self = instance;
            if (self != null && self.keybindManager != null) {
                KeybindMod.LOGGER.info("Received keybind sync ({} actions, v{}).",
                        payload.actions() == null ? 0 : payload.actions().size(), payload.version());
                self.keybindManager.onServerSync(getServerAddress(), payload.version(), payload.actions());
            }
        });
    }

    /** Called from ClientPacketListenerPayloadMixin on the netty thread. */
    public static void onCapturePayload(KeybindCapturePayload payload) {
        Minecraft.getInstance().execute(() -> {
            KeybindModClient self = instance;
            if (self != null && self.keybindManager != null) {
                KeybindMod.LOGGER.info("Received keybind capture request.");
                self.keybindManager.beginKeyCapture();
            }
        });
    }

    private static String getServerAddress() {
        Minecraft client = Minecraft.getInstance();
        ServerData serverData = client.getCurrentServer();
        if (serverData != null) {
            return serverData.ip;
        }
        return "singleplayer";
    }
}
