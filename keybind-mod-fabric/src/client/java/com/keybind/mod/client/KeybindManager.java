package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.state.KeybindClientState;
import com.keybind.mod.common.sync.KeybindSyncPlan;
import com.keybind.mod.network.KeybindActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.*;

public class KeybindManager {

    private static final KeyMapping.Category KEYBIND_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(KeybindMod.MOD_ID, "actions"));
    private static final KeybindClientState CLIENT_STATE = new KeybindClientState();

    private final Map<String, KeyMapping> registeredMappings = new LinkedHashMap<>();

    public static String getDisplayName(String actionName) {
        return CLIENT_STATE.getDisplayName(actionName);
    }

    public void registerAllKnownActions() {
        Map<String, String> allActions = ServerKeybindStorage.loadAllActions();
        if (allActions.isEmpty()) {
            KeybindMod.LOGGER.info("No known actions to pre-register.");
            return;
        }
        for (Map.Entry<String, String> entry : allActions.entrySet()) {
            registerAction(entry.getKey(), entry.getValue());
        }
        KeybindMod.LOGGER.info("Pre-registered {} known actions.", allActions.size());
    }

    private void registerAction(String actionName, String keyName) {
        InputConstants.Key key = resolveKey(keyName);

        if (registeredMappings.containsKey(actionName)) {
            registeredMappings.get(actionName).setKey(key);
        } else {
            KeyMapping mapping = new KeyMapping(
                    "key.keybind." + actionName,
                    key.getType(),
                    key.getValue(),
                    KEYBIND_CATEGORY
            );
            
            Minecraft client = Minecraft.getInstance();
            if (client.options == null) {
                KeyMappingHelper.registerKeyMapping(mapping);
            } else {
                injectKeyMapping(client, mapping);
            }
            
            registeredMappings.put(actionName, mapping);
        }
    }

    private void injectKeyMapping(Minecraft client, KeyMapping mapping) {
        try {
            Field field = null;
            try {
                field = Options.class.getDeclaredField("keyMappings");
            } catch (NoSuchFieldException e) {
                for (Field f : Options.class.getDeclaredFields()) {
                    if (f.getType() == KeyMapping[].class) {
                        field = f;
                        break;
                    }
                }
            }

            if (field != null) {
                field.setAccessible(true);
                KeyMapping[] original = (KeyMapping[]) field.get(client.options);
                if (!ArrayUtils.contains(original, mapping)) {
                    KeyMapping[] updated = ArrayUtils.add(original, mapping);
                    field.set(client.options, updated);
                    KeyMapping.resetMapping();
                    KeybindMod.LOGGER.info("Dynamically registered action: {}", mapping.getName());
                }
            }
        } catch (Exception e) {
            KeybindMod.LOGGER.error("Failed to dynamically register keybind via reflection", e);
        }
    }

    private void removeKeyMapping(Minecraft client, KeyMapping mapping) {
        try {
            Field field = null;
            try {
                field = Options.class.getDeclaredField("keyMappings");
            } catch (NoSuchFieldException e) {
                for (Field f : Options.class.getDeclaredFields()) {
                    if (f.getType() == KeyMapping[].class) {
                        field = f;
                        break;
                    }
                }
            }

            if (field != null) {
                field.setAccessible(true);
                KeyMapping[] original = (KeyMapping[]) field.get(client.options);
                if (ArrayUtils.contains(original, mapping)) {
                    KeyMapping[] updated = ArrayUtils.removeElement(original, mapping);
                    field.set(client.options, updated);
                    KeyMapping.resetMapping();
                    KeybindMod.LOGGER.info("Dynamically removed action: {}", mapping.getName());
                }
            }
        } catch (Exception e) {
            KeybindMod.LOGGER.error("Failed to dynamically remove keybind via reflection", e);
        }
    }

    public void onServerSync(String serverAddress, String serverVersion, List<KeybindActionDefinition> actions) {
        KeybindMod.LOGGER.info("Starting sync for: {} (Version: {}, Actions: {})", serverAddress, serverVersion, actions.size());

        Map<String, String> savedBindings = ServerKeybindStorage.load(serverAddress);
        KeybindSyncPlan syncPlan = CLIENT_STATE.startSync(serverAddress, savedBindings, registeredMappings.keySet(), actions);

        for (KeybindActionDefinition action : actions) {
            String keyName = syncPlan.bindings().get(action.name());
            if (syncPlan.newActions().contains(action.name())) {
                KeybindMod.LOGGER.info("New action found: {} (default: {})", action.name(), keyName);
            }
            if (keyName != null && !keyName.isEmpty()) {
                registerAction(action.name(), keyName);
            }
        }

        Minecraft client = Minecraft.getInstance();
        for (String actionName : syncPlan.removedActions()) {
            KeyMapping mapping = registeredMappings.remove(actionName);
            KeybindMod.LOGGER.info("Removing obsolete action from client: {}", actionName);
            if (mapping != null && client.options != null) {
                removeKeyMapping(client, mapping);
            }
            CLIENT_STATE.removeAction(actionName);
            KeybindMod.LOGGER.info("Removed obsolete action from saved config: {}", actionName);
        }

        if (syncPlan.configChanged()) {
            ServerKeybindStorage.save(serverAddress, syncPlan.bindings());
        }
        
        KeyMapping.resetMapping();
        
        if (client.player != null) {
            String msg = "§a[Keybind] Synced " + actions.size() + " actions.";
            if (!syncPlan.newActions().isEmpty()) msg += " §7(" + syncPlan.newActions().size() + " new)";
            client.player.sendSystemMessage(Component.literal(msg));
        }
        
        KeybindMod.LOGGER.info("Successfully synced {} actions for: {}", actions.size(), serverAddress);
    }

    public void onDisconnect() {
        CLIENT_STATE.clearSync();
        KeybindMod.LOGGER.info("Disconnected — keybind sync cleared.");
    }

    public void tick(Minecraft client) {
        if (!CLIENT_STATE.isSynced() || client.player == null || client.screen != null) return;

        boolean changed = false;
        for (Map.Entry<String, KeyMapping> entry : registeredMappings.entrySet()) {
            String action = entry.getKey();
            KeyMapping mapping = entry.getValue();

            if (CLIENT_STATE.getCurrentServer() != null) {
                String currentKey = mapping.saveString();
                changed |= CLIENT_STATE.updateBinding(action, currentKey);
            }

            while (mapping.consumeClick()) {
                sendAction(client, action);
            }
        }
        if (changed) {
            ServerKeybindStorage.save(CLIENT_STATE.getCurrentServer(), CLIENT_STATE.snapshotPersistedBindings());
            KeyMapping.resetMapping();
        }
    }

    public boolean isSynced() {
        return CLIENT_STATE.isSynced();
    }

    private void sendAction(Minecraft client, String action) {
        if (client.player == null) return;

        try {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new KeybindActionPayload(action));
            KeybindMod.LOGGER.debug("Sent action: {}", action);
        } catch (Exception e) {
            client.player.connection.sendCommand("kbind " + action);
            KeybindMod.LOGGER.debug("Sent command: /kbind {}", action);
        }
    }

    private InputConstants.Key resolveKey(String keyName) {
        if (keyName == null || keyName.isEmpty()) return InputConstants.UNKNOWN;
        String name = keyName.toUpperCase();

        // Check for internal Minecraft key strings first
        if (name.contains(".")) {
            try {
                return InputConstants.getKey(keyName);
            } catch (Exception ignored) {}
        }

        // Mouse Buttons
        return switch (name) {
            case "MOUSE_LEFT", "MOUSE_1" -> InputConstants.Type.MOUSE.getOrCreate(0);
            case "MOUSE_RIGHT", "MOUSE_2" -> InputConstants.Type.MOUSE.getOrCreate(1);
            case "MOUSE_MIDDLE", "MOUSE_3" -> InputConstants.Type.MOUSE.getOrCreate(2);
            case "MOUSE_4" -> InputConstants.Type.MOUSE.getOrCreate(3);
            case "MOUSE_5" -> InputConstants.Type.MOUSE.getOrCreate(4);
            case "MOUSE_6" -> InputConstants.Type.MOUSE.getOrCreate(5);
            case "MOUSE_7" -> InputConstants.Type.MOUSE.getOrCreate(6);
            case "MOUSE_8" -> InputConstants.Type.MOUSE.getOrCreate(7);

            // Function Keys
            case "F1" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F1);
            case "F2" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F2);
            case "F3" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F3);
            case "F4" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F4);
            case "F5" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F5);
            case "F6" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F6);
            case "F7" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F7);
            case "F8" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8);
            case "F9" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F9);
            case "F10" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10);
            case "F11" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F11);
            case "F12" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F12);
            case "F13" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F13);
            case "F14" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F14);
            case "F15" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F15);
            case "F16" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F16);
            case "F17" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F17);
            case "F18" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F18);
            case "F19" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F19);
            case "F20" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F20);
            case "F21" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F21);
            case "F22" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F22);
            case "F23" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F23);
            case "F24" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F24);
            case "F25" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F25);

            // Alphanumeric
            case "A" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_A);
            case "B" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B);
            case "C" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C);
            case "D" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_D);
            case "E" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_E);
            case "F" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F);
            case "G" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G);
            case "H" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H);
            case "I" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_I);
            case "J" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J);
            case "K" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K);
            case "L" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_L);
            case "M" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_M);
            case "N" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_N);
            case "O" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_O);
            case "P" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_P);
            case "Q" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Q);
            case "R" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R);
            case "S" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_S);
            case "T" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_T);
            case "U" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_U);
            case "V" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_V);
            case "W" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_W);
            case "X" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X);
            case "Y" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Y);
            case "Z" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z);
            case "0" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_0);
            case "1" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_1);
            case "2" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_2);
            case "3" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_3);
            case "4" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_4);
            case "5" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_5);
            case "6" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_6);
            case "7" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_7);
            case "8" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_8);
            case "9" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_9);

            // Symbols
            case "[", "LEFT_BRACKET" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET);
            case "]", "RIGHT_BRACKET" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET);
            case "\\", "BACKSLASH" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_BACKSLASH);
            case ";", "SEMICOLON" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SEMICOLON);
            case "'", "APOSTROPHE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_APOSTROPHE);
            case ",", "COMMA" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_COMMA);
            case ".", "PERIOD" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PERIOD);
            case "/", "SLASH" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SLASH);
            case "`", "GRAVE_ACCENT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_GRAVE_ACCENT);
            case "-", "MINUS" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_MINUS);
            case "=", "EQUAL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_EQUAL);

            // Navigation & Special
            case "SPACE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE);
            case "ENTER" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_ENTER);
            case "TAB" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_TAB);
            case "BACKSPACE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_BACKSPACE);
            case "INSERT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_INSERT);
            case "DELETE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_DELETE);
            case "RIGHT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT);
            case "LEFT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT);
            case "DOWN" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_DOWN);
            case "UP" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_UP);
            case "PAGE_UP" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PAGE_UP);
            case "PAGE_DOWN" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PAGE_DOWN);
            case "HOME" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_HOME);
            case "END" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_END);
            case "ESCAPE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_ESCAPE);
            case "CAPS_LOCK" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_CAPS_LOCK);
            case "SCROLL_LOCK" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SCROLL_LOCK);
            case "NUM_LOCK" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_NUM_LOCK);
            case "PRINT_SCREEN" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PRINT_SCREEN);
            case "PAUSE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PAUSE);

            // Numpad
            case "KP_0" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_0);
            case "KP_1" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_1);
            case "KP_2" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_2);
            case "KP_3" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_3);
            case "KP_4" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_4);
            case "KP_5" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_5);
            case "KP_6" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_6);
            case "KP_7" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_7);
            case "KP_8" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_8);
            case "KP_9" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_9);
            case "KP_DECIMAL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_DECIMAL);
            case "KP_DIVIDE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_DIVIDE);
            case "KP_MULTIPLY" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_MULTIPLY);
            case "KP_SUBTRACT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_SUBTRACT);
            case "KP_ADD" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_ADD);
            case "KP_ENTER" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_ENTER);
            case "KP_EQUAL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_EQUAL);

            // Modifiers
            case "LEFT_SHIFT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT);
            case "LEFT_CONTROL", "LEFT_CTRL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL);
            case "LEFT_ALT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT);
            case "LEFT_SUPER" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SUPER);
            case "RIGHT_SHIFT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SHIFT);
            case "RIGHT_CONTROL", "RIGHT_CTRL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_CONTROL);
            case "RIGHT_ALT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_ALT);
            case "RIGHT_SUPER" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SUPER);
            case "MENU" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_MENU);

            // World Keys
            case "WORLD_1" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_WORLD_1);
            case "WORLD_2" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_WORLD_2);

            default -> InputConstants.UNKNOWN;
        };
    }
}
