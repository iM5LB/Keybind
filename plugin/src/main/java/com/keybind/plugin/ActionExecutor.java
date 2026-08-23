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

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> globalCooldowns = new ConcurrentHashMap<>();

    public ActionExecutor(KeybindPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean execute(Player player, String actionName) {
        ConfigManager.ActionConfig action = configManager.getAction(actionName);
        if (action == null) {
            player.sendMessage(Component.text("Unknown action: " + actionName, NamedTextColor.RED));
            return false;
        }

        if (!player.hasPermission("keybind.use")) {
            player.sendMessage(Component.text("You don't have permission to use keybinds.", NamedTextColor.RED));
            return false;
        }

        String perm = action.getPermission();
        if (perm != null && !perm.isEmpty() && !player.hasPermission(perm)) {
            player.sendMessage(Component.text("You don't have permission for action: " + action.getDisplayName(), NamedTextColor.RED));
            return false;
        }

        if (!player.hasPermission("keybind.bypass.cooldown")) {
            long now = System.currentTimeMillis();
            
            // Global cooldown check
            Long lastGlobal = globalCooldowns.get(player.getUniqueId());
            if (lastGlobal != null && now - lastGlobal < configManager.getGlobalCooldown()) {
                double remaining = (configManager.getGlobalCooldown() - (now - lastGlobal)) / 1000.0;
                player.sendMessage(Component.text("Please wait " + String.format("%.1f", remaining) + "s before next action.", NamedTextColor.YELLOW));
                return false;
            }

            // Per-action cooldown check
            Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
            if (playerCooldowns != null) {
                Long lastAction = playerCooldowns.get(action.getName().toLowerCase());
                if (lastAction != null && now - lastAction < action.getCooldown()) {
                    double remaining = (action.getCooldown() - (now - lastAction)) / 1000.0;
                    player.sendMessage(Component.text(action.getDisplayName() + " is on cooldown! Wait " + String.format("%.1f", remaining) + "s", NamedTextColor.YELLOW));
                    return false;
                }
            }
        }

        String command = action.getCommand().replace("{player}", player.getName());
        boolean success;

        if (action.isConsole()) {
            success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            success = player.performCommand(command);
        }

        if (success) {
            long now = System.currentTimeMillis();
            globalCooldowns.put(player.getUniqueId(), now);
            cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                     .put(action.getName().toLowerCase(), now);
        }

        return success;
    }

    public void clearCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
        globalCooldowns.remove(uuid);
    }
}
