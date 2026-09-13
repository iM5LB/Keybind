package com.keybind.plugin.dialog;

import com.keybind.plugin.ConfigManager;
import com.keybind.plugin.KeyCaptureDraft;
import com.keybind.plugin.KeybindPlugin;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;

public final class KeybindDialogListener implements Listener {

    private final KeybindPlugin plugin;
    private final KeybindDialogUi ui;

    public KeybindDialogListener(KeybindPlugin plugin) {
        this.plugin = plugin;
        this.ui = new KeybindDialogUi(plugin);
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        Key id = event.getIdentifier();
        if (!KeybindDialogUi.NAMESPACE.equals(id.namespace()) || !id.value().startsWith("dialog/")) {
            return;
        }
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }

        Player player = connection.getPlayer();
        DialogResponseView view = event.getDialogResponseView();
        BinaryTagHolder tag = event.getTag();

        plugin.getServer().getScheduler().runTask(plugin, () -> handle(player, id, view, tag));
    }

    private void handle(Player player, Key id, DialogResponseView view, BinaryTagHolder tag) {
        if (!player.isOnline() || !player.hasPermission("keybind.admin")) {
            player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return;
        }

        int page = readPage(view, tag);
        String name = readName(view, tag);

        if (id.equals(KeybindDialogUi.MENU)) {
            ui.openMenu(player, page);
            return;
        }
        if (id.equals(KeybindDialogUi.ADD)) {
            ui.openAdd(player);
            return;
        }
        if (id.equals(KeybindDialogUi.VIEW) && name != null) {
            ui.openView(player, name, page);
            return;
        }
        if (id.equals(KeybindDialogUi.EDIT) && name != null) {
            ui.openEdit(player, name, page);
            return;
        }
        if (id.equals(KeybindDialogUi.DELETE) && name != null) {
            ui.openDelete(player, name, page);
            return;
        }
        if (id.equals(KeybindDialogUi.SETTINGS)) {
            ui.openSettings(player, page);
            return;
        }
        if (id.equals(KeybindDialogUi.RELOAD)) {
            plugin.getConfigManager().reload();
            plugin.getSyncSender().sendSyncToAll();
            player.sendMessage(Component.text("Keybind config reloaded and synced to all players!", NamedTextColor.GREEN));
            ui.openMenu(player, page);
            return;
        }
        if (id.equals(KeybindDialogUi.CAPTURE)) {
            startCapture(player, view, tag, page, name);
            return;
        }
        if (id.equals(KeybindDialogUi.CREATE)) {
            createAction(player, view);
            return;
        }
        if (id.equals(KeybindDialogUi.SAVE) && name != null) {
            saveAction(player, name, view, page);
            return;
        }
        if (id.equals(KeybindDialogUi.REMOVE) && name != null) {
            String error = plugin.getConfigManager().deleteAction(name);
            if (error != null) {
                ui.openNotice(player, error, KeybindDialogUi.MENU, KeybindDialogUi.tag(null, page));
                return;
            }
            plugin.getSyncSender().sendSyncToAll();
            player.sendMessage(Component.text("Removed keybind \"" + name + "\".", NamedTextColor.GREEN));
            ui.openMenu(player, page);
            return;
        }
        if (id.equals(KeybindDialogUi.SAVE_SETTINGS)) {
            saveSettings(player, view, page);
        }
    }

    private void startCapture(Player player, DialogResponseView view, BinaryTagHolder tag, int page, String name) {
        String screenName = extractQuoted(tag, "screen");
        KeyCaptureDraft.Screen screen = parseScreen(screenName, name);
        String draftName = name == null ? text(view, "name") : name;
        String display = text(view, "display");
        String command = text(view, "command").replaceFirst("^/", "");
        String defaultKey = text(view, "default_key").toUpperCase(Locale.ROOT);
        String permission = text(view, "permission");
        String cooldown = text(view, "cooldown");
        Boolean console = view == null ? null : view.getBoolean("console");

        if (screen == KeyCaptureDraft.Screen.VIEW && draftName != null) {
            ConfigManager.ActionConfig existing = plugin.getConfigManager().getAction(draftName);
            if (existing != null) {
                display = existing.getDisplayName();
                command = existing.getCommand();
                defaultKey = existing.getDefaultKey();
                permission = existing.getPermission();
                cooldown = String.valueOf(existing.getCooldown());
                console = existing.isConsole();
            }
        }

        try {
            player.closeDialog();
        } catch (Throwable ignored) {
            player.closeInventory();
        }

        plugin.getKeyCaptureService().request(player, new KeyCaptureDraft(
                screen,
                draftName,
                display,
                command,
                defaultKey,
                permission,
                cooldown.isEmpty() ? String.valueOf(plugin.getConfigManager().getGlobalCooldown()) : cooldown,
                console != null && console,
                page
        ));
    }

    private static KeyCaptureDraft.Screen parseScreen(String screen, String name) {
        if (screen != null) {
            try {
                return KeyCaptureDraft.Screen.valueOf(screen.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return name != null && !name.isEmpty() ? KeyCaptureDraft.Screen.EDIT : KeyCaptureDraft.Screen.ADD;
    }

    private void createAction(Player player, DialogResponseView view) {
        String name = text(view, "name");
        if (name.isEmpty()) {
            ui.openNotice(player, "Action name is required.", KeybindDialogUi.ADD, KeybindDialogUi.tag(null, 0));
            return;
        }
        name = name.toLowerCase(Locale.ROOT);
        if (!ConfigManager.isValidActionName(name) || ConfigManager.isReservedName(name)) {
            ui.openNotice(player, "Use letters, numbers, or underscores. Reserved names: manage, add, list, reload…", KeybindDialogUi.ADD, KeybindDialogUi.tag(null, 0));
            return;
        }
        if (plugin.getConfigManager().getAction(name) != null) {
            ui.openNotice(player, "An action named \"" + name + "\" already exists.", KeybindDialogUi.ADD, KeybindDialogUi.tag(null, 0));
            return;
        }

        ConfigManager.ActionConfig action = fromForm(name, view, null);
        String error = validateAndSave(action);
        if (error != null) {
            ui.openNotice(player, error, KeybindDialogUi.ADD, KeybindDialogUi.tag(null, 0));
            return;
        }
        plugin.getSyncSender().sendSyncToAll();
        player.sendMessage(Component.text("Added keybind \"" + action.getDisplayName() + "\".", NamedTextColor.GREEN));
        ui.openMenu(player, 0);
    }

    private void saveAction(Player player, String name, DialogResponseView view, int page) {
        ConfigManager.ActionConfig existing = plugin.getConfigManager().getAction(name);
        if (existing == null) {
            ui.openNotice(player, "Unknown action: " + name, KeybindDialogUi.MENU, KeybindDialogUi.tag(null, page));
            return;
        }

        ConfigManager.ActionConfig action = fromForm(existing.getName(), view, existing);
        String error = validateAndSave(action);
        if (error != null) {
            ui.openNotice(player, error, KeybindDialogUi.EDIT, KeybindDialogUi.tag(existing.getName(), page));
            return;
        }
        plugin.getSyncSender().sendSyncToAll();
        player.sendMessage(Component.text("Updated keybind \"" + action.getDisplayName() + "\".", NamedTextColor.GREEN));
        ui.openView(player, existing.getName(), page);
    }

    private void saveSettings(Player player, DialogResponseView view, int page) {
        Long cooldown = parseLong(text(view, "global_cooldown"));
        if (cooldown == null) {
            ui.openNotice(player, "Global cooldown must be a number (milliseconds).", KeybindDialogUi.SETTINGS, KeybindDialogUi.tag(null, page));
            return;
        }
        String error = plugin.getConfigManager().setGlobalCooldown(cooldown);
        if (error != null) {
            ui.openNotice(player, error, KeybindDialogUi.SETTINGS, KeybindDialogUi.tag(null, page));
            return;
        }
        plugin.getSyncSender().sendSyncToAll();
        player.sendMessage(Component.text("Global cooldown set to " + cooldown + " ms.", NamedTextColor.GREEN));
        ui.openMenu(player, page);
    }

    private String validateAndSave(ConfigManager.ActionConfig action) {
        return plugin.getConfigManager().saveAction(action);
    }

    private ConfigManager.ActionConfig fromForm(String name, DialogResponseView view, ConfigManager.ActionConfig existing) {
        ConfigManager config = plugin.getConfigManager();
        String display = text(view, "display");
        String command = text(view, "command").replaceFirst("^/", "");
        String defaultKey = text(view, "default_key").toUpperCase(Locale.ROOT);
        String permission = text(view, "permission");
        Long cooldown = parseLong(text(view, "cooldown"));
        Boolean console = view == null ? null : view.getBoolean("console");

        return new ConfigManager.ActionConfig(
                name,
                display.isEmpty() ? config.formatDefaultName(name) : display,
                command,
                defaultKey,
                permission,
                cooldown != null ? cooldown : (existing != null ? existing.getCooldown() : config.getGlobalCooldown()),
                console != null ? console : existing != null && existing.isConsole()
        );
    }

    private static String text(DialogResponseView view, String key) {
        if (view == null) {
            return "";
        }
        String value = view.getText(key);
        return value == null ? "" : value.trim();
    }

    private static Long parseLong(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String readName(DialogResponseView view, BinaryTagHolder tag) {
        String fromView = text(view, "name");
        if (ConfigManager.isValidActionName(fromView)) {
            return fromView;
        }
        return extractQuoted(tag, "name");
    }

    private static int readPage(DialogResponseView view, BinaryTagHolder tag) {
        if (view != null) {
            Float page = view.getFloat("page");
            if (page != null) {
                return Math.max(0, page.intValue());
            }
        }
        String raw = extractNumber(tag, "page");
        if (raw == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String extractQuoted(BinaryTagHolder tag, String key) {
        if (tag == null) {
            return null;
        }
        String snbt = tag.string();
        String needle = key + ":\"";
        int start = snbt.indexOf(needle);
        if (start < 0) {
            return null;
        }
        start += needle.length();
        int end = snbt.indexOf('"', start);
        return end < 0 ? null : snbt.substring(start, end);
    }

    private static String extractNumber(BinaryTagHolder tag, String key) {
        if (tag == null) {
            return null;
        }
        String snbt = tag.string();
        String needle = key + ":";
        int start = snbt.indexOf(needle);
        if (start < 0) {
            return null;
        }
        start += needle.length();
        int end = start;
        while (end < snbt.length() && (Character.isDigit(snbt.charAt(end)) || snbt.charAt(end) == '-')) {
            end++;
        }
        return end == start ? null : snbt.substring(start, end);
    }
}
