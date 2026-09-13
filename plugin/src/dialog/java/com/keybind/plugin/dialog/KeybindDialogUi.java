package com.keybind.plugin.dialog;

import com.keybind.plugin.ConfigManager;
import com.keybind.plugin.KeyCaptureDraft;
import com.keybind.plugin.KeybindPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class KeybindDialogUi {

    static final String NAMESPACE = "keybind";
    static final Key MENU = Key.key(NAMESPACE, "dialog/menu");
    static final Key ADD = Key.key(NAMESPACE, "dialog/add");
    static final Key CREATE = Key.key(NAMESPACE, "dialog/create");
    static final Key VIEW = Key.key(NAMESPACE, "dialog/view");
    static final Key EDIT = Key.key(NAMESPACE, "dialog/edit");
    static final Key SAVE = Key.key(NAMESPACE, "dialog/save");
    static final Key DELETE = Key.key(NAMESPACE, "dialog/delete");
    static final Key REMOVE = Key.key(NAMESPACE, "dialog/remove");
    static final Key SETTINGS = Key.key(NAMESPACE, "dialog/settings");
    static final Key SAVE_SETTINGS = Key.key(NAMESPACE, "dialog/save_settings");
    static final Key RELOAD = Key.key(NAMESPACE, "dialog/reload");
    static final Key CAPTURE = Key.key(NAMESPACE, "dialog/capture");

    private static final int PAGE_SIZE = 8;
    private static final int BUTTON_WIDTH = 300;

    private final KeybindPlugin plugin;

    public KeybindDialogUi(KeybindPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMenu(Player player, int page) {
        ConfigManager config = plugin.getConfigManager();
        List<ConfigManager.ActionConfig> actions = config.getActions().values().stream()
                .sorted(Comparator.comparing(action -> action.getName().toLowerCase(Locale.ROOT)))
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil(actions.size() / (double) PAGE_SIZE));
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, actions.size());

        List<ActionButton> buttons = new ArrayList<>();
        for (ConfigManager.ActionConfig action : actions.subList(from, to)) {
            String keyLabel = action.getDefaultKey() == null || action.getDefaultKey().isBlank()
                    ? "unset"
                    : action.getDefaultKey();
            buttons.add(button(
                    Component.text(action.getDisplayName(), NamedTextColor.AQUA),
                    Component.text(action.getName() + "  ·  " + keyLabel + "  ·  /" + action.getCommand(), NamedTextColor.GRAY),
                    VIEW,
                    tag(action.getName(), currentPage)
            ));
        }

        buttons.add(button(Component.text("Add Keybind", NamedTextColor.GREEN), Component.text("Create a new action"), ADD, tag(null, currentPage)));
        buttons.add(button(Component.text("Settings", NamedTextColor.GOLD), Component.text("Global cooldown"), SETTINGS, tag(null, currentPage)));
        buttons.add(button(Component.text("Reload & Sync", NamedTextColor.YELLOW), Component.text("Reload config.yml and sync players"), RELOAD, tag(null, currentPage)));
        if (currentPage > 0) {
            buttons.add(button(Component.text("Previous page", NamedTextColor.GRAY), null, MENU, tag(null, currentPage - 1)));
        }
        if (currentPage < totalPages - 1) {
            buttons.add(button(Component.text("Next page", NamedTextColor.GRAY), null, MENU, tag(null, currentPage + 1)));
        }

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Keybind Manager", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                actions.size() + " keybind" + (actions.size() == 1 ? "" : "s")
                                        + "  ·  page " + (currentPage + 1) + "/" + totalPages
                                        + "  ·  click an action to edit",
                                NamedTextColor.GRAY
                        ))))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(
                        buttons,
                        ActionButton.builder(Component.text("Close", NamedTextColor.RED)).width(150).build(),
                        1
                ))
        ));
    }

    public void openAdd(Player player) {
        openAdd(player, new KeyCaptureDraft(
                KeyCaptureDraft.Screen.ADD,
                "",
                "",
                "",
                "",
                "",
                String.valueOf(plugin.getConfigManager().getGlobalCooldown()),
                false,
                0
        ));
    }

    public void openAdd(Player player, KeyCaptureDraft draft) {
        String keyHint = draft.defaultKey == null || draft.defaultKey.isBlank()
                ? "Click Press a key, then press it on your keyboard."
                : "Default key: " + draft.defaultKey + " — click Press a key to change it.";
        player.showDialog(formDialog(
                "Add Keybind",
                "Action names are letters, numbers, and underscores only. " + keyHint,
                draft.name == null ? "" : draft.name,
                draft.displayName == null ? "" : draft.displayName,
                draft.command == null ? "" : draft.command,
                draft.defaultKey == null ? "" : draft.defaultKey,
                draft.permission == null ? "" : draft.permission,
                draft.cooldown == null || draft.cooldown.isBlank()
                        ? String.valueOf(plugin.getConfigManager().getGlobalCooldown())
                        : draft.cooldown,
                draft.console,
                true,
                CREATE,
                tag(null, draft.page, "add")
        ));
    }

    public void openView(Player player, String name, int page) {
        ConfigManager.ActionConfig action = plugin.getConfigManager().getAction(name);
        if (action == null) {
            openNotice(player, "Unknown action: " + name, MENU, tag(null, page));
            return;
        }

        Component body = Component.text()
                .append(line("Name", action.getName()))
                .append(Component.newline())
                .append(line("Display", action.getDisplayName()))
                .append(Component.newline())
                .append(line("Command", "/" + action.getCommand()))
                .append(Component.newline())
                .append(line("Default key", blankToDash(action.getDefaultKey())))
                .append(Component.newline())
                .append(line("Permission", blankToDash(action.getPermission())))
                .append(Component.newline())
                .append(line("Cooldown", action.getCooldown() + " ms"))
                .append(Component.newline())
                .append(line("Console", String.valueOf(action.isConsole())))
                .build();

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(action.getDisplayName(), NamedTextColor.AQUA, TextDecoration.BOLD))
                        .body(List.of(DialogBody.plainMessage(body)))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(
                        List.of(
                                button(Component.text("Press a key…", NamedTextColor.YELLOW), Component.text("Click, then press the key on your keyboard"), CAPTURE, tag(action.getName(), page, "view")),
                                button(Component.text("Edit", NamedTextColor.GREEN), null, EDIT, tag(action.getName(), page)),
                                button(Component.text("Delete", NamedTextColor.RED), Component.text("Remove this keybind"), DELETE, tag(action.getName(), page))
                        ),
                        ActionButton.builder(Component.text("Back", NamedTextColor.GRAY))
                                .width(150)
                                .action(DialogAction.customClick(MENU, tag(null, page)))
                                .build(),
                        2
                ))
        ));
    }

    public void openEdit(Player player, String name, int page) {
        ConfigManager.ActionConfig action = plugin.getConfigManager().getAction(name);
        if (action == null) {
            openNotice(player, "Unknown action: " + name, MENU, tag(null, page));
            return;
        }
        openEdit(player, new KeyCaptureDraft(
                KeyCaptureDraft.Screen.EDIT,
                action.getName(),
                action.getDisplayName(),
                action.getCommand(),
                action.getDefaultKey() == null ? "" : action.getDefaultKey(),
                action.getPermission() == null ? "" : action.getPermission(),
                String.valueOf(action.getCooldown()),
                action.isConsole(),
                page
        ));
    }

    public void openEdit(Player player, KeyCaptureDraft draft) {
        String keyHint = draft.defaultKey == null || draft.defaultKey.isBlank()
                ? "Click Press a key, then press it on your keyboard."
                : "Default key: " + draft.defaultKey + " — click Press a key to change it.";
        player.showDialog(formDialog(
                "Edit " + (draft.displayName == null || draft.displayName.isBlank() ? draft.name : draft.displayName),
                "ID: " + draft.name + " (cannot be changed). " + keyHint,
                draft.name,
                draft.displayName == null ? "" : draft.displayName,
                draft.command == null ? "" : draft.command,
                draft.defaultKey == null ? "" : draft.defaultKey,
                draft.permission == null ? "" : draft.permission,
                draft.cooldown,
                draft.console,
                false,
                SAVE,
                tag(draft.name, draft.page, "edit")
        ));
    }

    public void resumeAfterCapture(Player player, KeyCaptureDraft draft, String key) {
        KeyCaptureDraft updated = draft.withKey(key == null ? "" : key);
        if (draft.screen == KeyCaptureDraft.Screen.ADD) {
            openAdd(player, updated);
            return;
        }
        if (draft.screen == KeyCaptureDraft.Screen.EDIT) {
            openEdit(player, updated);
            return;
        }
        ConfigManager.ActionConfig existing = plugin.getConfigManager().getAction(draft.name);
        if (existing == null) {
            openNotice(player, "Unknown action: " + draft.name, MENU, tag(null, draft.page));
            return;
        }
        if (key != null && !key.isBlank() && !key.equals(existing.getDefaultKey())) {
            ConfigManager.ActionConfig saved = new ConfigManager.ActionConfig(
                    existing.getName(),
                    existing.getDisplayName(),
                    existing.getCommand(),
                    key,
                    existing.getPermission(),
                    existing.getCooldown(),
                    existing.isConsole()
            );
            String error = plugin.getConfigManager().saveAction(saved);
            if (error != null) {
                openNotice(player, error, VIEW, tag(existing.getName(), draft.page));
                return;
            }
            plugin.getSyncSender().sendSyncToAll();
        }
        openView(player, existing.getName(), draft.page);
    }

    public void openDelete(Player player, String name, int page) {
        ConfigManager.ActionConfig action = plugin.getConfigManager().getAction(name);
        if (action == null) {
            openNotice(player, "Unknown action: " + name, MENU, tag(null, page));
            return;
        }

        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Delete Keybind", NamedTextColor.RED, TextDecoration.BOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "Remove \"" + action.getDisplayName() + "\" (" + action.getName() + ")? Players will lose this keybind on the next sync.",
                                NamedTextColor.GRAY
                        ))))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Delete", NamedTextColor.RED))
                                .tooltip(Component.text("This cannot be undone"))
                                .action(DialogAction.customClick(REMOVE, tag(action.getName(), page)))
                                .build(),
                        ActionButton.builder(Component.text("Cancel", NamedTextColor.GRAY))
                                .action(DialogAction.customClick(VIEW, tag(action.getName(), page)))
                                .build()
                ))
        ));
    }

    public void openSettings(Player player, int page) {
        long cooldown = plugin.getConfigManager().getGlobalCooldown();
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Keybind Settings", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "Minimum wait between any two keybind actions.",
                                NamedTextColor.GRAY
                        ))))
                        .inputs(List.of(
                                DialogInput.text("global_cooldown", Component.text("Global cooldown (ms)"))
                                        .width(BUTTON_WIDTH)
                                        .maxLength(8)
                                        .initial(String.valueOf(cooldown))
                                        .build()
                        ))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Save", NamedTextColor.GREEN))
                                .action(DialogAction.customClick(SAVE_SETTINGS, tag(null, page)))
                                .build(),
                        ActionButton.builder(Component.text("Back", NamedTextColor.GRAY))
                                .action(DialogAction.customClick(MENU, tag(null, page)))
                                .build()
                ))
        ));
    }

    public void openNotice(Player player, String message, Key back, BinaryTagHolder tag) {
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Keybind", NamedTextColor.YELLOW))
                        .body(List.of(DialogBody.plainMessage(Component.text(message, NamedTextColor.WHITE))))
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.notice(
                        ActionButton.builder(Component.text("Back", NamedTextColor.GRAY))
                                .action(DialogAction.customClick(back, tag))
                                .build()
                ))
        ));
    }

    private Dialog formDialog(
            String title,
            String description,
            String name,
            String displayName,
            String command,
            String defaultKey,
            String permission,
            String cooldown,
            boolean console,
            boolean includeName,
            Key submit,
            BinaryTagHolder tag
    ) {
        List<DialogInput> inputs = new ArrayList<>();
        if (includeName) {
            inputs.add(DialogInput.text("name", Component.text("Action name"))
                    .width(BUTTON_WIDTH)
                    .maxLength(32)
                    .initial(name)
                    .build());
        }
        inputs.add(DialogInput.text("display", Component.text("Display name"))
                .width(BUTTON_WIDTH)
                .maxLength(48)
                .initial(displayName)
                .build());
        inputs.add(DialogInput.text("command", Component.text("Command (without /)"))
                .width(BUTTON_WIDTH)
                .maxLength(256)
                .initial(command)
                .build());
        inputs.add(DialogInput.text("default_key", Component.text("Default key (e.g. LEFT_BRACKET)"))
                .width(BUTTON_WIDTH)
                .maxLength(32)
                .initial(defaultKey)
                .build());
        inputs.add(DialogInput.text("permission", Component.text("Extra permission (optional)"))
                .width(BUTTON_WIDTH)
                .maxLength(64)
                .initial(permission)
                .build());
        inputs.add(DialogInput.text("cooldown", Component.text("Cooldown (ms)"))
                .width(BUTTON_WIDTH)
                .maxLength(8)
                .initial(cooldown)
                .build());
        inputs.add(DialogInput.bool("console", Component.text("Run as console"))
                .initial(console)
                .build());

        BinaryTagHolder safeTag = tag == null ? tag(null, 0) : tag;
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                        .body(List.of(DialogBody.plainMessage(Component.text(description, NamedTextColor.GRAY))))
                        .inputs(inputs)
                        .canCloseWithEscape(true)
                        .build())
                .type(DialogType.multiAction(
                        List.of(
                                ActionButton.builder(Component.text("Press a key…", NamedTextColor.YELLOW))
                                        .tooltip(Component.text("Click this, then press the key on your keyboard"))
                                        .width(BUTTON_WIDTH)
                                        .action(DialogAction.customClick(CAPTURE, safeTag))
                                        .build(),
                                ActionButton.builder(Component.text("Save", NamedTextColor.GREEN))
                                        .width(BUTTON_WIDTH)
                                        .action(DialogAction.customClick(submit, safeTag))
                                        .build()
                        ),
                        ActionButton.builder(Component.text("Cancel", NamedTextColor.GRAY))
                                .width(150)
                                .action(DialogAction.customClick(MENU, safeTag))
                                .build(),
                        1
                ))
        );
    }

    private static ActionButton button(Component label, Component tooltip, Key action, BinaryTagHolder tag) {
        return ActionButton.builder(label)
                .tooltip(tooltip)
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(action, tag))
                .build();
    }

    static BinaryTagHolder tag(String name, int page) {
        return tag(name, page, null);
    }

    static BinaryTagHolder tag(String name, int page, String screen) {
        StringBuilder snbt = new StringBuilder("{page:").append(page);
        if (name != null && !name.isEmpty()) {
            snbt.append(",name:\"").append(name).append('"');
        }
        if (screen != null && !screen.isEmpty()) {
            snbt.append(",screen:\"").append(screen).append('"');
        }
        return BinaryTagHolder.binaryTagHolder(snbt.append('}').toString());
    }

    private static Component line(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
