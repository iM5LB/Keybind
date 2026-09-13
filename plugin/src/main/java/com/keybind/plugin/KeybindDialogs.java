package com.keybind.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Loads the Minecraft Dialog UI only when Paper 1.21.7+ (and Java 21) is present.
 * Older servers keep using the chat commands.
 */
public final class KeybindDialogs {

    private static final String DIALOG_CLASS = "io.papermc.paper.dialog.Dialog";
    private static final String UI_CLASS = "com.keybind.plugin.dialog.KeybindDialogUi";
    private static final String LISTENER_CLASS = "com.keybind.plugin.dialog.KeybindDialogListener";

    private KeybindDialogs() {
    }

    public static boolean isSupported() {
        try {
            Class.forName(DIALOG_CLASS);
            Class.forName(UI_CLASS);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static boolean openMenu(Player player, KeybindPlugin plugin) {
        if (!isSupported()) {
            return false;
        }
        try {
            Class<?> uiClass = Class.forName(UI_CLASS);
            Object ui = uiClass.getConstructor(KeybindPlugin.class).newInstance(plugin);
            uiClass.getMethod("openMenu", Player.class, int.class).invoke(ui, player, 0);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().warning("Failed to open keybind dialog: " + e.getMessage());
            return false;
        }
    }

    public static void resumeAfterCapture(Player player, KeybindPlugin plugin, KeyCaptureDraft draft, String key) {
        if (!isSupported() || draft == null) {
            return;
        }
        try {
            Class<?> uiClass = Class.forName(UI_CLASS);
            Object ui = uiClass.getConstructor(KeybindPlugin.class).newInstance(plugin);
            uiClass.getMethod("resumeAfterCapture", Player.class, KeyCaptureDraft.class, String.class)
                    .invoke(ui, player, draft, key);
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().warning("Failed to reopen keybind dialog after capture: " + e.getMessage());
        }
    }

    public static Listener createListener(KeybindPlugin plugin) {
        if (!isSupported()) {
            return null;
        }
        try {
            Class<?> listenerClass = Class.forName(LISTENER_CLASS);
            return (Listener) listenerClass.getConstructor(KeybindPlugin.class).newInstance(plugin);
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().warning("Failed to register keybind dialog listener: " + e.getMessage());
            return null;
        }
    }
}
