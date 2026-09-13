package com.keybind.forge.client;

import com.keybind.mod.client.KeybindManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

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
        com.keybind.forge.KeybindForgeNetwork.setCaptureReceiver(KEYBINDS::beginKeyCapture);
        KEYBINDS.registerAllKnownActions();
        registerForgeListeners();
    }

    private static void registerForgeListeners() {
        try {
            registerModernListener(TickEvent.ClientTickEvent.Post.class, event -> KEYBINDS.tick(Minecraft.getInstance()));
            registerModernListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> KEYBINDS.onDisconnect());
        } catch (NoSuchFieldException exception) {
            registerLegacyListeners();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register Forge event listener", exception);
        }
    }

    private static void registerModernListener(Class<?> eventType, Consumer<Object> listener) throws ReflectiveOperationException {
        Field busField = eventType.getField("BUS");
        Object eventBus = busField.get(null);
        Class<?> eventBusType = Class.forName("net.minecraftforge.eventbus.api.bus.EventBus");
        Method addListener = eventBusType.getMethod("addListener", Consumer.class);
        addListener.invoke(eventBus, listener);
    }

    private static void registerLegacyListeners() {
        try {
            Field eventBusField = MinecraftForge.class.getField("EVENT_BUS");
            Object eventBus = eventBusField.get(null);
            Method register = eventBus.getClass().getMethod("register", Object.class);
            register.invoke(eventBus, new ForgeListeners());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to register Forge event listener", exception);
        }
    }

    private static final class ForgeListeners {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent.Post event) {
            KEYBINDS.tick(Minecraft.getInstance());
        }

        @SubscribeEvent
        public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            KEYBINDS.onDisconnect();
        }
    }

    private static String serverAddress() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server == null ? "singleplayer" : server.ip;
    }
}
