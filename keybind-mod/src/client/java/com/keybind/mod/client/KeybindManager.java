package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindSyncPayload;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeybindManager {

    private final Map<String, Integer> activeBindings = new LinkedHashMap<>();
    private final Map<String, Boolean> keyStates = new LinkedHashMap<>();
    private String currentServer = null;
    private boolean synced = false;

    /**
     * Called when the server sends an action sync packet.
     * Sets up keybinds based on server actions + per-server saved config.
     */
    public void onServerSync(String serverAddress, List<KeybindSyncPayload.ActionEntry> actions) {
        activeBindings.clear();
        keyStates.clear();
        currentServer = serverAddress;
        synced = true;

        // Load saved per-server bindings
        Map<String, String> savedBindings = ServerKeybindStorage.load(serverAddress);

        // Build active bindings: use saved keys if available, otherwise server defaults
        Map<String, String> currentBindings = new LinkedHashMap<>();
        for (KeybindSyncPayload.ActionEntry action : actions) {
            String key;
            if (savedBindings != null && savedBindings.containsKey(action.name())) {
                key = savedBindings.get(action.name());
            } else {
                key = action.defaultKey();
            }

            if (key == null || key.isEmpty()) continue;

            int glfwKey = resolveGlfwKey(key.toUpperCase());
            if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
                KeybindMod.LOGGER.warn("Unknown key '" + key + "' for action '" + action.name() + "'");
                continue;
            }

            activeBindings.put(action.name(), glfwKey);
            keyStates.put(action.name(), false);
            currentBindings.put(action.name(), key.toUpperCase());
            KeybindMod.LOGGER.info("Bound: " + key.toUpperCase() + " -> " + action.name());
        }

        // Save current bindings for this server
        ServerKeybindStorage.save(serverAddress, currentBindings);

        KeybindMod.LOGGER.info("Synced " + activeBindings.size() + " keybinds for server: " + serverAddress);
    }

    /**
     * Called when disconnecting from a server.
     */
    public void onDisconnect() {
        activeBindings.clear();
        keyStates.clear();
        currentServer = null;
        synced = false;
    }

    /**
     * Called every client tick. Polls key states and sends actions on key press.
     */
    public void tick(Minecraft client) {
        if (!synced || client.player == null) return;

        // Don't process keybinds while a screen is open (chat, inventory, etc.)
        if (client.screen != null) return;

        Window window = client.getWindow();

        for (Map.Entry<String, Integer> entry : activeBindings.entrySet()) {
            String action = entry.getKey();
            int glfwKey = entry.getValue();

            boolean pressed = InputConstants.isKeyDown(window, glfwKey);
            boolean wasPressed = keyStates.getOrDefault(action, false);

            // Detect key-down edge (pressed this tick, wasn't pressed last tick)
            if (pressed && !wasPressed) {
                sendAction(client, action);
            }

            keyStates.put(action, pressed);
        }
    }

    public boolean isSynced() {
        return synced;
    }

    private void sendAction(Minecraft client, String action) {
        if (client.player == null) return;

        if (canSendPacket()) {
            ClientPlayNetworking.send(new KeybindActionPayload(action));
            KeybindMod.LOGGER.debug("Sent packet action: " + action);
        } else {
            client.player.connection.sendCommand("kbind " + action);
            KeybindMod.LOGGER.debug("Sent chat command: /kbind " + action);
        }
    }

    private boolean canSendPacket() {
        try {
            return ClientPlayNetworking.canSend(KeybindActionPayload.TYPE);
        } catch (Exception e) {
            return false;
        }
    }

    static int resolveGlfwKey(String keyName) {
        return switch (keyName) {
            case "A" -> GLFW.GLFW_KEY_A;
            case "B" -> GLFW.GLFW_KEY_B;
            case "C" -> GLFW.GLFW_KEY_C;
            case "D" -> GLFW.GLFW_KEY_D;
            case "E" -> GLFW.GLFW_KEY_E;
            case "F" -> GLFW.GLFW_KEY_F;
            case "G" -> GLFW.GLFW_KEY_G;
            case "H" -> GLFW.GLFW_KEY_H;
            case "I" -> GLFW.GLFW_KEY_I;
            case "J" -> GLFW.GLFW_KEY_J;
            case "K" -> GLFW.GLFW_KEY_K;
            case "L" -> GLFW.GLFW_KEY_L;
            case "M" -> GLFW.GLFW_KEY_M;
            case "N" -> GLFW.GLFW_KEY_N;
            case "O" -> GLFW.GLFW_KEY_O;
            case "P" -> GLFW.GLFW_KEY_P;
            case "Q" -> GLFW.GLFW_KEY_Q;
            case "R" -> GLFW.GLFW_KEY_R;
            case "S" -> GLFW.GLFW_KEY_S;
            case "T" -> GLFW.GLFW_KEY_T;
            case "U" -> GLFW.GLFW_KEY_U;
            case "V" -> GLFW.GLFW_KEY_V;
            case "W" -> GLFW.GLFW_KEY_W;
            case "X" -> GLFW.GLFW_KEY_X;
            case "Y" -> GLFW.GLFW_KEY_Y;
            case "Z" -> GLFW.GLFW_KEY_Z;
            case "0" -> GLFW.GLFW_KEY_0;
            case "1" -> GLFW.GLFW_KEY_1;
            case "2" -> GLFW.GLFW_KEY_2;
            case "3" -> GLFW.GLFW_KEY_3;
            case "4" -> GLFW.GLFW_KEY_4;
            case "5" -> GLFW.GLFW_KEY_5;
            case "6" -> GLFW.GLFW_KEY_6;
            case "7" -> GLFW.GLFW_KEY_7;
            case "8" -> GLFW.GLFW_KEY_8;
            case "9" -> GLFW.GLFW_KEY_9;
            case "F1" -> GLFW.GLFW_KEY_F1;
            case "F2" -> GLFW.GLFW_KEY_F2;
            case "F3" -> GLFW.GLFW_KEY_F3;
            case "F4" -> GLFW.GLFW_KEY_F4;
            case "F5" -> GLFW.GLFW_KEY_F5;
            case "F6" -> GLFW.GLFW_KEY_F6;
            case "F7" -> GLFW.GLFW_KEY_F7;
            case "F8" -> GLFW.GLFW_KEY_F8;
            case "F9" -> GLFW.GLFW_KEY_F9;
            case "F10" -> GLFW.GLFW_KEY_F10;
            case "F11" -> GLFW.GLFW_KEY_F11;
            case "F12" -> GLFW.GLFW_KEY_F12;
            case "LEFT_SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LEFT_CONTROL", "LEFT_CTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RIGHT_CONTROL", "RIGHT_CTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LEFT_ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "ESCAPE", "ESC" -> GLFW.GLFW_KEY_ESCAPE;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "DELETE" -> GLFW.GLFW_KEY_DELETE;
            case "INSERT" -> GLFW.GLFW_KEY_INSERT;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGE_UP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "COMMA" -> GLFW.GLFW_KEY_COMMA;
            case "PERIOD" -> GLFW.GLFW_KEY_PERIOD;
            case "SEMICOLON" -> GLFW.GLFW_KEY_SEMICOLON;
            case "APOSTROPHE" -> GLFW.GLFW_KEY_APOSTROPHE;
            case "MINUS" -> GLFW.GLFW_KEY_MINUS;
            case "EQUAL" -> GLFW.GLFW_KEY_EQUAL;
            case "LEFT_BRACKET" -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case "RIGHT_BRACKET" -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "BACKSLASH" -> GLFW.GLFW_KEY_BACKSLASH;
            case "SLASH" -> GLFW.GLFW_KEY_SLASH;
            case "GRAVE", "GRAVE_ACCENT" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }
}
