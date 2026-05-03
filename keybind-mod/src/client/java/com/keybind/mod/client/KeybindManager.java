package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.network.KeybindActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeybindManager {

    private static final KeyMapping.Category KEYBIND_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("keybind", "actions"));

    private final KeybindConfigManager configManager;
    private final List<KeybindEntry> entries = new ArrayList<>();

    public KeybindManager(KeybindConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Register all keybinds from the config with Fabric's keybinding system.
     */
    public void registerKeybinds() {
        for (Map.Entry<String, String> entry : configManager.getBindings().entrySet()) {
            String keyName = entry.getKey().toUpperCase();
            String action = entry.getValue();

            int glfwKey = resolveGlfwKey(keyName);
            if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
                KeybindMod.LOGGER.warn("Unknown key: " + keyName + " — skipping binding for action: " + action);
                continue;
            }

            KeyMapping keyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.keybind." + action,
                    InputConstants.Type.KEYSYM,
                    glfwKey,
                    KEYBIND_CATEGORY
            ));

            entries.add(new KeybindEntry(keyMapping, action));
            KeybindMod.LOGGER.info("Registered keybind: " + keyName + " -> " + action);
        }
    }

    /**
     * Called every client tick. Checks if any registered keybind was pressed
     * and sends the corresponding action.
     */
    public void tick(Minecraft client) {
        for (KeybindEntry entry : entries) {
            while (entry.keyMapping.consumeClick()) {
                sendAction(client, entry.action);
            }
        }
    }

    /**
     * Send the action to the server, either via packet or chat command.
     */
    private void sendAction(Minecraft client, String action) {
        if (client.player == null) return;

        if (configManager.usePackets() && canSendPacket()) {
            ClientPlayNetworking.send(new KeybindActionPayload(action));
            KeybindMod.LOGGER.debug("Sent packet action: " + action);
        } else {
            client.player.connection.sendCommand("kbind " + action);
            KeybindMod.LOGGER.debug("Sent chat command: /kbind " + action);
        }
    }

    /**
     * Check if the client can send packets on the keybind channel.
     */
    private boolean canSendPacket() {
        try {
            return ClientPlayNetworking.canSend(KeybindActionPayload.TYPE);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve a key name string (e.g. "K", "F5", "LEFT_SHIFT") to a GLFW key code.
     */
    private int resolveGlfwKey(String keyName) {
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

    private record KeybindEntry(KeyMapping keyMapping, String action) {}
}
