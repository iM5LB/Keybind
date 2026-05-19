package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.state.KeybindClientState;
import com.keybind.mod.common.sync.KeybindSyncPlan;
import com.keybind.mod.network.KeybindActionPayload;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.*;

public class KeybindManager {

    private static final String KEYBIND_CATEGORY = "key.categories." + KeybindMod.MOD_ID;
    private static final KeybindClientState CLIENT_STATE = new KeybindClientState();

    private final Map<String, KeyBinding> registeredMappings = new LinkedHashMap<>();

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
        InputUtil.Key key = resolveKey(keyName);

        if (registeredMappings.containsKey(actionName)) {
            registeredMappings.get(actionName).setBoundKey(key);
        } else {
            KeyBinding mapping = new KeyBinding(
                    "key.keybind." + actionName,
                    key.getCategory(),
                    key.getCode(),
                    KEYBIND_CATEGORY
            );
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options == null) {
                KeyBindingHelper.registerKeyBinding(mapping);
            } else {
                injectKeyMapping(client, mapping);
            }
            
            registeredMappings.put(actionName, mapping);
        }
    }

    private void injectKeyMapping(MinecraftClient client, KeyBinding mapping) {
        try {
            Field field = null;
            try {
                field = GameOptions.class.getDeclaredField("allKeys");
            } catch (NoSuchFieldException e) {
                for (Field f : GameOptions.class.getDeclaredFields()) {
                    if (f.getType() == KeyBinding[].class) {
                        field = f;
                        break;
                    }
                }
            }

            if (field != null) {
                field.setAccessible(true);
                KeyBinding[] original = (KeyBinding[]) field.get(client.options);
                if (!ArrayUtils.contains(original, mapping)) {
                    KeyBinding[] updated = ArrayUtils.add(original, mapping);
                    field.set(client.options, updated);
                    KeyBinding.updateKeysByCode();
                    KeybindMod.LOGGER.info("Dynamically registered action: {}", mapping.getTranslationKey());
                }
            }
        } catch (Exception e) {
            KeybindMod.LOGGER.error("Failed to dynamically register keybind via reflection", e);
        }
    }

    private void removeKeyMapping(MinecraftClient client, KeyBinding mapping) {
        try {
            Field field = null;
            try {
                field = GameOptions.class.getDeclaredField("allKeys");
            } catch (NoSuchFieldException e) {
                for (Field f : GameOptions.class.getDeclaredFields()) {
                    if (f.getType() == KeyBinding[].class) {
                        field = f;
                        break;
                    }
                }
            }

            if (field != null) {
                field.setAccessible(true);
                KeyBinding[] original = (KeyBinding[]) field.get(client.options);
                if (ArrayUtils.contains(original, mapping)) {
                    KeyBinding[] updated = ArrayUtils.removeElement(original, mapping);
                    field.set(client.options, updated);
                    KeyBinding.updateKeysByCode();
                    KeybindMod.LOGGER.info("Dynamically removed action: {}", mapping.getTranslationKey());
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

        MinecraftClient client = MinecraftClient.getInstance();
        for (String actionName : syncPlan.removedActions()) {
            KeyBinding mapping = registeredMappings.remove(actionName);
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
        
        KeyBinding.updateKeysByCode();
        
        if (client.player != null) {
            String msg = "§a[Keybind] Synced " + actions.size() + " actions.";
            if (!syncPlan.newActions().isEmpty()) msg += " §7(" + syncPlan.newActions().size() + " new)";
            client.player.sendMessage(Text.literal(msg), false);
        }
        
        KeybindMod.LOGGER.info("Successfully synced {} actions for: {}", actions.size(), serverAddress);
    }

    public void onDisconnect() {
        CLIENT_STATE.clearSync();
        KeybindMod.LOGGER.info("Disconnected — keybind sync cleared.");
    }

    public void tick(MinecraftClient client) {
        if (!CLIENT_STATE.isSynced() || client.player == null || client.currentScreen != null) return;

        boolean changed = false;
        for (Map.Entry<String, KeyBinding> entry : registeredMappings.entrySet()) {
            String action = entry.getKey();
            KeyBinding mapping = entry.getValue();

            if (CLIENT_STATE.getCurrentServer() != null) {
                String currentKey = mapping.getBoundKeyTranslationKey();
                changed |= CLIENT_STATE.updateBinding(action, currentKey);
            }

            while (mapping.wasPressed()) {
                sendAction(client, action);
            }
        }
        if (changed) {
            ServerKeybindStorage.save(CLIENT_STATE.getCurrentServer(), CLIENT_STATE.snapshotPersistedBindings());
            KeyBinding.updateKeysByCode();
        }
    }

    public boolean isSynced() {
        return CLIENT_STATE.isSynced();
    }

    private void sendAction(MinecraftClient client, String action) {
        if (client.player == null) return;

        try {
            ClientPlayNetworking.send(new KeybindActionPayload(action));
            KeybindMod.LOGGER.debug("Sent action: {}", action);
        } catch (Exception e) {
            client.player.networkHandler.sendCommand("kbind " + action);
            KeybindMod.LOGGER.debug("Sent command: /kbind {}", action);
        }
    }

    private InputUtil.Key resolveKey(String keyName) {
        if (keyName == null || keyName.isEmpty()) return InputUtil.UNKNOWN_KEY;
        String name = keyName.toUpperCase();

        // Check for internal Minecraft key strings first
        if (name.contains(".")) {
            try {
                return InputUtil.fromTranslationKey(keyName);
            } catch (Exception ignored) {}
        }

        // Mouse Buttons
        return switch (name) {
            case "MOUSE_LEFT", "MOUSE_1" -> InputUtil.Type.MOUSE.createFromCode(0);
            case "MOUSE_RIGHT", "MOUSE_2" -> InputUtil.Type.MOUSE.createFromCode(1);
            case "MOUSE_MIDDLE", "MOUSE_3" -> InputUtil.Type.MOUSE.createFromCode(2);
            case "MOUSE_4" -> InputUtil.Type.MOUSE.createFromCode(3);
            case "MOUSE_5" -> InputUtil.Type.MOUSE.createFromCode(4);
            case "MOUSE_6" -> InputUtil.Type.MOUSE.createFromCode(5);
            case "MOUSE_7" -> InputUtil.Type.MOUSE.createFromCode(6);
            case "MOUSE_8" -> InputUtil.Type.MOUSE.createFromCode(7);

            // Function Keys
            case "F1" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F1);
            case "F2" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F2);
            case "F3" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F3);
            case "F4" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F4);
            case "F5" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F5);
            case "F6" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F6);
            case "F7" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F7);
            case "F8" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F8);
            case "F9" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F9);
            case "F10" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F10);
            case "F11" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F11);
            case "F12" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F12);
            case "F13" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F13);
            case "F14" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F14);
            case "F15" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F15);
            case "F16" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F16);
            case "F17" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F17);
            case "F18" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F18);
            case "F19" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F19);
            case "F20" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F20);
            case "F21" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F21);
            case "F22" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F22);
            case "F23" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F23);
            case "F24" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F24);
            case "F25" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F25);

            // Alphanumeric
            case "A" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_A);
            case "B" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_B);
            case "C" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_C);
            case "D" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_D);
            case "E" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_E);
            case "F" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F);
            case "G" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_G);
            case "H" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_H);
            case "I" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_I);
            case "J" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_J);
            case "K" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_K);
            case "L" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_L);
            case "M" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_M);
            case "N" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_N);
            case "O" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_O);
            case "P" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_P);
            case "Q" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_Q);
            case "R" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_R);
            case "S" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_S);
            case "T" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_T);
            case "U" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_U);
            case "V" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_V);
            case "W" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_W);
            case "X" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_X);
            case "Y" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_Y);
            case "Z" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_Z);
            case "0" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_0);
            case "1" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_1);
            case "2" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_2);
            case "3" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_3);
            case "4" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_4);
            case "5" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_5);
            case "6" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_6);
            case "7" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_7);
            case "8" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_8);
            case "9" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_9);

            // Symbols
            case "[", "LEFT_BRACKET" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_LEFT_BRACKET);
            case "]", "RIGHT_BRACKET" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_RIGHT_BRACKET);
            case "\\", "BACKSLASH" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_BACKSLASH);
            case ";", "SEMICOLON" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_SEMICOLON);
            case "'", "APOSTROPHE" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_APOSTROPHE);
            case ",", "COMMA" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_COMMA);
            case ".", "PERIOD" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_PERIOD);
            case "/", "SLASH" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_SLASH);
            case "`", "GRAVE_ACCENT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_GRAVE_ACCENT);
            case "-", "MINUS" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_MINUS);
            case "=", "EQUAL" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_EQUAL);

            // Navigation & Special
            case "SPACE" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_SPACE);
            case "ENTER" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_ENTER);
            case "TAB" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_TAB);
            case "BACKSPACE" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_BACKSPACE);
            case "INSERT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_INSERT);
            case "DELETE" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_DELETE);
            case "RIGHT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_RIGHT);
            case "LEFT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_LEFT);
            case "DOWN" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_DOWN);
            case "UP" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_UP);
            case "PAGE_UP" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_PAGE_UP);
            case "PAGE_DOWN" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_PAGE_DOWN);
            case "HOME" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_HOME);
            case "END" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_END);
            case "ESCAPE" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_ESCAPE);
            case "CAPS_LOCK" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_CAPS_LOCK);
            case "SCROLL_LOCK" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_SCROLL_LOCK);
            case "NUM_LOCK" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_NUM_LOCK);
            case "PRINT_SCREEN" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_PRINT_SCREEN);
            case "PAUSE" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_PAUSE);
            case "SCROLL_LOCK_EXTRA" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_SCROLL_LOCK);

            // Numpad
            case "KP_0", "NUMPAD_0" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_0);
            case "KP_1", "NUMPAD_1" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_1);
            case "KP_2", "NUMPAD_2" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_2);
            case "KP_3", "NUMPAD_3" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_3);
            case "KP_4", "NUMPAD_4" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_4);
            case "KP_5", "NUMPAD_5" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_5);
            case "KP_6", "NUMPAD_6" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_6);
            case "KP_7", "NUMPAD_7" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_7);
            case "KP_8", "NUMPAD_8" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_8);
            case "KP_9", "NUMPAD_9" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_9);
            case "KP_ADD", "NUMPAD_ADD" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_ADD);
            case "KP_DECIMAL", "NUMPAD_DECIMAL" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_DECIMAL);
            case "KP_DIVIDE", "NUMPAD_DIVIDE" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_DIVIDE);
            case "KP_ENTER", "NUMPAD_ENTER" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_ENTER);
            case "KP_EQUAL", "NUMPAD_EQUAL" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_EQUAL);
            case "KP_MULTIPLY", "NUMPAD_MULTIPLY" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_MULTIPLY);
            case "KP_SUBTRACT", "NUMPAD_SUBTRACT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_KP_SUBTRACT);

            // Modifiers
            case "LEFT_SHIFT", "SHIFT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_LEFT_SHIFT);
            case "RIGHT_SHIFT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
            case "LEFT_CONTROL", "LEFT_CTRL", "CONTROL", "CTRL" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_LEFT_CONTROL);
            case "RIGHT_CONTROL" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_RIGHT_CONTROL);
            case "LEFT_ALT", "ALT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_LEFT_ALT);
            case "RIGHT_ALT" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_RIGHT_ALT);
            case "LEFT_SUPER", "SUPER", "WINDOWS", "CMD" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_LEFT_SUPER);
            case "RIGHT_SUPER" -> InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_RIGHT_SUPER);

            default -> {
                // Try literal key name (single char)
                if (name.length() == 1) {
                    int code = name.charAt(0);
                    try {
                        yield InputUtil.Type.KEYSYM.createFromCode(code);
                    } catch (Exception ignored) {}
                }
                yield InputUtil.UNKNOWN_KEY;
            }
        };
    }
}
