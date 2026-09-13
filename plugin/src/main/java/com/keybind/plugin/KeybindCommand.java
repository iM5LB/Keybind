package com.keybind.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class KeybindCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SET_FIELDS = List.of("command", "display", "key", "permission", "cooldown", "console");

    private final KeybindPlugin plugin;
    private final ActionExecutor actionExecutor;
    private final ConfigManager configManager;

    public KeybindCommand(KeybindPlugin plugin, ActionExecutor actionExecutor, ConfigManager configManager) {
        this.plugin = plugin;
        this.actionExecutor = actionExecutor;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        if (subcommand.equals("help")) {
            sendUsage(sender, label);
            return true;
        }

        if (subcommand.equals("list")) {
            sender.sendMessage(Component.text("Available actions: " +
                    String.join(", ", configManager.getActionNames()), NamedTextColor.AQUA));
            return true;
        }

        if (subcommand.equals("reload")
                || subcommand.equals("manage") || subcommand.equals("gui") || subcommand.equals("menu")
                || subcommand.equals("add") || subcommand.equals("remove") || subcommand.equals("set")) {
            return handleAdmin(sender, label, subcommand, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute keybind actions.", NamedTextColor.RED));
            return true;
        }

        actionExecutor.execute(player, subcommand);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String label, String subcommand, String[] args) {
        if (!sender.hasPermission("keybind.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        switch (subcommand) {
            case "reload" -> {
                configManager.reload();
                plugin.getSyncSender().sendSyncToAll();
                sender.sendMessage(Component.text("Keybind config reloaded and synced to all players!", NamedTextColor.GREEN));
            }
            case "manage", "gui", "menu" -> openManager(sender);
            case "add" -> addAction(sender, label, args);
            case "remove" -> removeAction(sender, label, args);
            case "set" -> setAction(sender, label, args);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void openManager(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Open the manager in-game, or use /kbind add|set|remove from the console.", NamedTextColor.YELLOW));
            sendAdminCommands(sender, "kbind");
            return;
        }
        if (KeybindDialogs.openMenu(player, plugin)) {
            return;
        }
        player.sendMessage(Component.text("Minecraft dialogs need Paper 1.21.7+. Use commands instead:", NamedTextColor.YELLOW));
        sendAdminCommands(player, "kbind");
    }

    private void addAction(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /" + label + " add <name> <command...>", NamedTextColor.YELLOW));
            return;
        }

        String name = args[1].toLowerCase(Locale.ROOT);
        if (!ConfigManager.isValidActionName(name) || ConfigManager.isReservedName(name)) {
            sender.sendMessage(Component.text("Action names must be letters, numbers, or underscores, and cannot be a reserved command.", NamedTextColor.RED));
            return;
        }
        if (configManager.getAction(name) != null) {
            sender.sendMessage(Component.text("An action named \"" + name + "\" already exists.", NamedTextColor.RED));
            return;
        }

        String commandLine = join(args, 2).replaceFirst("^/", "");
        ConfigManager.ActionConfig action = new ConfigManager.ActionConfig(
                name,
                configManager.formatDefaultName(name),
                commandLine,
                "",
                "",
                configManager.getGlobalCooldown(),
                false
        );
        applyChange(sender, configManager.saveAction(action), "Added keybind \"" + action.getDisplayName() + "\".");
    }

    private void removeAction(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " remove <name>", NamedTextColor.YELLOW));
            return;
        }
        applyChange(sender, configManager.deleteAction(args[1]), "Removed keybind \"" + args[1] + "\".");
    }

    private void setAction(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /" + label + " set <name> <command|display|key|permission|cooldown|console> <value>", NamedTextColor.YELLOW));
            return;
        }

        ConfigManager.ActionConfig existing = configManager.getAction(args[1]);
        if (existing == null) {
            sender.sendMessage(Component.text("Unknown action: " + args[1], NamedTextColor.RED));
            return;
        }

        String field = args[2].toLowerCase(Locale.ROOT);
        String value = join(args, 3);
        String command = existing.getCommand();
        String displayName = existing.getDisplayName();
        String defaultKey = existing.getDefaultKey();
        String permission = existing.getPermission();
        long cooldown = existing.getCooldown();
        boolean console = existing.isConsole();

        switch (field) {
            case "command" -> command = value.replaceFirst("^/", "");
            case "display", "display-name", "displayname" -> displayName = value;
            case "key", "default-key", "defaultkey" -> defaultKey = value.toUpperCase(Locale.ROOT);
            case "permission", "perm" -> permission = value.equalsIgnoreCase("none") ? "" : value;
            case "cooldown" -> {
                try {
                    cooldown = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Cooldown must be a number (milliseconds).", NamedTextColor.RED));
                    return;
                }
            }
            case "console" -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(Component.text("Console must be true or false.", NamedTextColor.RED));
                    return;
                }
                console = Boolean.parseBoolean(value);
            }
            default -> {
                sender.sendMessage(Component.text("Unknown field. Use command, display, key, permission, cooldown, or console.", NamedTextColor.RED));
                return;
            }
        }

        ConfigManager.ActionConfig updated = new ConfigManager.ActionConfig(
                existing.getName(), displayName, command, defaultKey, permission, cooldown, console
        );
        applyChange(sender, configManager.saveAction(updated), "Updated " + existing.getName() + " " + field + ".");
    }

    private void applyChange(CommandSender sender, String error, String success) {
        if (error != null) {
            sender.sendMessage(Component.text(error, NamedTextColor.RED));
            return;
        }
        plugin.getSyncSender().sendSyncToAll();
        sender.sendMessage(Component.text(success, NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("Usage: /" + label + " <action|list|help>", NamedTextColor.YELLOW));
        if (sender.hasPermission("keybind.admin")) {
            sendAdminCommands(sender, label);
        }
    }

    private void sendAdminCommands(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " manage", NamedTextColor.GOLD)
                .append(Component.text(" — open the keybind dialog", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " add <name> <command...>", NamedTextColor.GOLD)
                .append(Component.text(" — create a keybind", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " set <name> <field> <value>", NamedTextColor.GOLD)
                .append(Component.text(" — edit a field", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " remove <name>", NamedTextColor.GOLD)
                .append(Component.text(" — delete a keybind", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.GOLD)
                .append(Component.text(" — reload config.yml", NamedTextColor.GRAY)));
    }

    private static String join(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(configManager.getActionNames());
            completions.add("list");
            completions.add("help");
            if (sender.hasPermission("keybind.admin")) {
                completions.addAll(List.of("manage", "gui", "add", "remove", "set", "reload"));
            }
            return prefix(completions, args[0]);
        }

        if (!sender.hasPermission("keybind.admin")) {
            return List.of();
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("remove") || sub.equals("set"))) {
            return prefix(new ArrayList<>(configManager.getActionNames()), args[1]);
        }
        if (args.length == 3 && sub.equals("set")) {
            return prefix(SET_FIELDS, args[2]);
        }
        if (args.length == 4 && sub.equals("set") && args[2].equalsIgnoreCase("console")) {
            return prefix(List.of("true", "false"), args[3]);
        }
        return List.of();
    }

    private static List<String> prefix(List<String> values, String token) {
        String prefix = token.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
    }
}
