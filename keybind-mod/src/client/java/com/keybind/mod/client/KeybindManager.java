package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.network.KeybindActionPayload;
import com.keybind.mod.network.KeybindSyncPayload;
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

    private final Map<String, KeyMapping> registeredMappings = new LinkedHashMap<>();
    private static final Map<String, String> displayNames = new HashMap<>();

    private String currentServer = null;
    private boolean synced = false;

    public static String getDisplayName(String actionName) {
        if (actionName == null) return null;
        return displayNames.get(actionName.toLowerCase());
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
        InputConstants.Key key;
        try {
            if (keyName.contains(".")) {
                key = InputConstants.getKey(keyName);
            } else {
                int glfwKey = resolveGlfwKey(keyName.toUpperCase());
                key = (glfwKey == GLFW.GLFW_KEY_UNKNOWN) 
                    ? InputConstants.UNKNOWN 
                    : InputConstants.Type.KEYSYM.getOrCreate(glfwKey);
            }
        } catch (Exception e) {
            KeybindMod.LOGGER.error("Failed to parse key '{}' for action '{}'", keyName, actionName);
            key = InputConstants.UNKNOWN;
        }

        if (registeredMappings.containsKey(actionName)) {
            registeredMappings.get(actionName).setKey(key);
        } else {
            KeyMapping mapping = new KeyMapping(
                    "key.keybind." + actionName,
                    InputConstants.Type.KEYSYM,
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

    public void onServerSync(String serverAddress, String serverVersion, List<KeybindSyncPayload.ActionEntry> actions) {
        KeybindMod.LOGGER.info("Starting sync for: {} (Version: {}, Actions: {})", serverAddress, serverVersion, actions.size());
        this.currentServer = serverAddress;
        this.synced = true;

        Map<String, String> savedBindings = ServerKeybindStorage.load(serverAddress);
        if (savedBindings == null) savedBindings = new LinkedHashMap<>();

        Set<String> incomingActions = new HashSet<>();
        List<String> newActions = new ArrayList<>();
        boolean configChanged = false;

        // Register/Update incoming actions
        for (KeybindSyncPayload.ActionEntry action : actions) {
            String name = action.name();
            incomingActions.add(name);
            displayNames.put(name.toLowerCase(), action.displayName());
            
            String keyName;
            if (savedBindings.containsKey(name)) {
                keyName = savedBindings.get(name);
            } else {
                keyName = action.defaultKey();
                savedBindings.put(name, keyName);
                newActions.add(name);
                configChanged = true;
                KeybindMod.LOGGER.info("New action found: {} (default: {})", name, keyName);
            }

            if (keyName != null && !keyName.isEmpty()) {
                registerAction(name, keyName);
            }
        }

        // Remove old actions that are no longer on the server
        Minecraft client = Minecraft.getInstance();
        Iterator<Map.Entry<String, KeyMapping>> it = registeredMappings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, KeyMapping> entry = it.next();
            String actionName = entry.getKey();
            if (!incomingActions.contains(actionName)) {
                KeybindMod.LOGGER.info("Removing obsolete action from client: {}", actionName);
                if (client.options != null) {
                    removeKeyMapping(client, entry.getValue());
                }
                it.remove();
                displayNames.remove(actionName.toLowerCase());
                if (savedBindings.containsKey(actionName)) {
                    savedBindings.remove(actionName);
                    configChanged = true;
                    KeybindMod.LOGGER.info("Removed obsolete action from saved config: {}", actionName);
                }
            }
        }

        if (configChanged) {
            ServerKeybindStorage.save(serverAddress, savedBindings);
        }
        
        KeyMapping.resetMapping();
        
        if (client.player != null) {
            String msg = "§a[Keybind] Synced " + actions.size() + " actions.";
            if (!newActions.isEmpty()) msg += " §7(" + newActions.size() + " new)";
            client.player.sendSystemMessage(Component.literal(msg));
        }
        
        KeybindMod.LOGGER.info("Successfully synced {} actions for: {}", actions.size(), serverAddress);
    }

    public void onDisconnect() {
        currentServer = null;
        synced = false;
        // We keep registeredMappings so they stay in the Controls menu,
        // but we clear displayNames to avoid confusion if rejoining a different server.
        // The mappings will get updated/removed on the next sync.
        KeybindMod.LOGGER.info("Disconnected — keybind sync cleared.");
    }

    public void tick(Minecraft client) {
        if (!synced || client.player == null || client.screen != null) return;

        boolean changed = false;
        for (Map.Entry<String, KeyMapping> entry : registeredMappings.entrySet()) {
            String action = entry.getKey();
            KeyMapping mapping = entry.getValue();

            if (currentServer != null) {
                String currentKey = mapping.saveString();
                Map<String, String> saved = ServerKeybindStorage.load(currentServer);
                if (saved != null && !currentKey.equals(saved.get(action))) {
                    saved.put(action, currentKey);
                    ServerKeybindStorage.save(currentServer, saved);
                    changed = true;
                }
            }

            while (mapping.consumeClick()) {
                sendAction(client, action);
            }
        }
        if (changed) KeyMapping.resetMapping();
    }

    public boolean isSynced() {
        return synced;
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

    static int resolveGlfwKey(String keyName) {
        return switch (keyName.toUpperCase()) {
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
            case "LEFT_BRACKET" -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case "RIGHT_BRACKET" -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "BACKSLASH" -> GLFW.GLFW_KEY_BACKSLASH;
            case "SEMICOLON" -> GLFW.GLFW_KEY_SEMICOLON;
            case "APOSTROPHE" -> GLFW.GLFW_KEY_APOSTROPHE;
            case "COMMA" -> GLFW.GLFW_KEY_COMMA;
            case "PERIOD" -> GLFW.GLFW_KEY_PERIOD;
            case "SLASH" -> GLFW.GLFW_KEY_SLASH;
            case "GRAVE_ACCENT" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case "MINUS" -> GLFW.GLFW_KEY_MINUS;
            case "EQUAL" -> GLFW.GLFW_KEY_EQUAL;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "INSERT" -> GLFW.GLFW_KEY_INSERT;
            case "DELETE" -> GLFW.GLFW_KEY_DELETE;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "PAGE_UP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGE_DOWN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "CAPS_LOCK" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "SCROLL_LOCK" -> GLFW.GLFW_KEY_SCROLL_LOCK;
            case "NUM_LOCK" -> GLFW.GLFW_KEY_NUM_LOCK;
            case "PRINT_SCREEN" -> GLFW.GLFW_KEY_PRINT_SCREEN;
            case "PAUSE" -> GLFW.GLFW_KEY_PAUSE;
            case "KP_0" -> GLFW.GLFW_KEY_KP_0;
            case "KP_1" -> GLFW.GLFW_KEY_KP_1;
            case "KP_2" -> GLFW.GLFW_KEY_KP_2;
            case "KP_3" -> GLFW.GLFW_KEY_KP_3;
            case "KP_4" -> GLFW.GLFW_KEY_KP_4;
            case "KP_5" -> GLFW.GLFW_KEY_KP_5;
            case "KP_6" -> GLFW.GLFW_KEY_KP_6;
            case "KP_7" -> GLFW.GLFW_KEY_KP_7;
            case "KP_8" -> GLFW.GLFW_KEY_KP_8;
            case "KP_9" -> GLFW.GLFW_KEY_KP_9;
            case "KP_DECIMAL" -> GLFW.GLFW_KEY_KP_DECIMAL;
            case "KP_DIVIDE" -> GLFW.GLFW_KEY_KP_DIVIDE;
            case "KP_MULTIPLY" -> GLFW.GLFW_KEY_KP_MULTIPLY;
            case "KP_SUBTRACT" -> GLFW.GLFW_KEY_KP_SUBTRACT;
            case "KP_ADD" -> GLFW.GLFW_KEY_KP_ADD;
            case "KP_ENTER" -> GLFW.GLFW_KEY_KP_ENTER;
            case "KP_EQUAL" -> GLFW.GLFW_KEY_KP_EQUAL;
            case "LEFT_SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "LEFT_CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "LEFT_ALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "LEFT_SUPER" -> GLFW.GLFW_KEY_LEFT_SUPER;
            case "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "RIGHT_CONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "RIGHT_SUPER" -> GLFW.GLFW_KEY_RIGHT_SUPER;
            case "MENU" -> GLFW.GLFW_KEY_MENU;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }
}
