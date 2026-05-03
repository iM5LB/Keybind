package com.keybind.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionExecutor {

    private final KeybindPlugin plugin;
    private final ConfigManager configManager;

    // Cooldown tracking: player UUID -> (action name -> last use timestamp)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public ActionExecutor(KeybindPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Execute an action for a player.
     *
     * @param player the player triggering the action
     * @param actionName the action name from config
     * @return true if executed successfully
     */
    public boolean execute(Player player, String actionName) {
        ConfigManager.ActionConfig action = configManager.getAction(actionName);
        if (action == null) {
            player.sendMessage(Component.text("Unknown action: " + actionName, NamedTextColor.RED));
            return false;
        }

        // Check base permission
        if (!player.hasPermission("keybind.use")) {
            player.sendMessage(Component.text("You don't have permission to use keybinds.", NamedTextColor.RED));
            return false;
        }

        // Check action-specific permission
        String perm = action.getPermission();
        if (perm != null && !perm.isEmpty() && !player.hasPermission(perm)) {
            player.sendMessage(Component.text("You don't have permission for action: " + actionName, NamedTextColor.RED));
            return false;
        }

        // Check per-action permission node: keybind.action.<name>
        if (!player.hasPermission("keybind.action." + actionName.toLowerCase())) {
            // Only block if the permission is explicitly set to false
            // By default we allow if keybind.use is granted
            if (player.isPermissionSet("keybind.action." + actionName.toLowerCase())
                    && !player.hasPermission("keybind.action." + actionName.toLowerCase())) {
                player.sendMessage(Component.text("You don't have permission for action: " + actionName, NamedTextColor.RED));
                return false;
            }
        }

        // Check cooldown
        if (!player.hasPermission("keybind.bypass.cooldown") && isOnCooldown(player, action)) {
            long remaining = getRemainingCooldown(player, action);
            player.sendMessage(Component.text(
                    "Action on cooldown! Wait " + (remaining / 1000.0) + "s", NamedTextColor.YELLOW));
            return false;
        }

        // Execute the command
        String command = action.getCommand();
        boolean success;

        if (action.isConsole()) {
            // Run as console
            success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        } else {
            // Run as player
            success = player.performCommand(command);
        }

        if (success) {
            setCooldown(player, action);
        }

        return success;
    }

    private boolean isOnCooldown(Player player, ConfigManager.ActionConfig action) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long lastUse = playerCooldowns.get(action.getName().toLowerCase());
        if (lastUse == null) return false;

        return System.currentTimeMillis() - lastUse < action.getCooldown();
    }

    private long getRemainingCooldown(Player player, ConfigManager.ActionConfig action) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long lastUse = playerCooldowns.get(action.getName().toLowerCase());
        if (lastUse == null) return 0;

        return action.getCooldown() - (System.currentTimeMillis() - lastUse);
    }

    private void setCooldown(Player player, ConfigManager.ActionConfig action) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(action.getName().toLowerCase(), System.currentTimeMillis());
    }

    /**
     * Clear cooldowns for a player (called on disconnect).
     */
    public void clearCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
