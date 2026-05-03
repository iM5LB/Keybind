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
import java.util.List;
import java.util.stream.Collectors;

public class KeybindCommand implements CommandExecutor, TabCompleter {

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
            sender.sendMessage(Component.text("Usage: /kbind <action|reload|list>", NamedTextColor.YELLOW));
            return true;
        }

        String subcommand = args[0].toLowerCase();

        // Admin subcommands
        if (subcommand.equals("reload")) {
            if (!sender.hasPermission("keybind.admin")) {
                sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return true;
            }
            configManager.reload();
            sender.sendMessage(Component.text("Keybind config reloaded!", NamedTextColor.GREEN));
            return true;
        }

        if (subcommand.equals("list")) {
            sender.sendMessage(Component.text("Available actions: " +
                    String.join(", ", configManager.getActionNames()), NamedTextColor.AQUA));
            return true;
        }

        // Action execution — must be a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute keybind actions.", NamedTextColor.RED));
            return true;
        }

        actionExecutor.execute(player, subcommand);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(configManager.getActionNames());
            if (sender.hasPermission("keybind.admin")) {
                completions.add("reload");
            }
            completions.add("list");
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
