package com.keybind.forge.client;

import com.keybind.mod.client.KeybindManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

public final class KeybindForgeClient {
    private static final KeybindManager KEYBINDS = new KeybindManager();

    private KeybindForgeClient() { }

    public static void initialize() {
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

        com.keybind.forge.KeybindForgeNetwork.setSyncReceiver(data -> KEYBINDS.onServerSync(serverAddress(), data.version(), data.actions()));
        KEYBINDS.registerAllKnownActions();
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> KEYBINDS.tick(Minecraft.getInstance()));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> KEYBINDS.onDisconnect());
    }

    private static String serverAddress() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server == null ? "singleplayer" : server.ip;
    }
}
