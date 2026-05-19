package com.keybind.plugin;

import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.KeybindSyncData;
import com.keybind.mod.common.network.KeybindPacketCodec;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SyncSender implements Listener {

    private final KeybindPlugin plugin;

    public SyncSender(KeybindPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                sendSync(player);
            }
        }, 20L);
    }

    public void sendSync(Player player) {
        byte[] data = buildSyncPacket();
        if (data == null) return;

        try {
            player.sendPluginMessage(plugin, plugin.getConfigManager().getSyncChannel(), data);
            plugin.getLogger().info("Sent sync to " + player.getName() + " (" + plugin.getConfigManager().getActions().size() + " actions)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send sync to " + player.getName() + ": " + e.getMessage());
        }
    }

    public void sendSyncToAll() {
        byte[] data = buildSyncPacket();
        if (data == null) return;

        String syncChannel = plugin.getConfigManager().getSyncChannel();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                player.sendPluginMessage(plugin, syncChannel, data);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send sync to " + player.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Sent sync to all online players.");
    }

    private byte[] buildSyncPacket() {
        Map<String, ConfigManager.ActionConfig> actions = plugin.getConfigManager().getActions();
        String version = plugin.getDescription().getVersion();

        try {
            List<KeybindActionDefinition> actionDefinitions = actions.values().stream()
                    .map(action -> new KeybindActionDefinition(
                            action.getName(),
                            action.getDisplayName(),
                            action.getDefaultKey()
                    ))
                    .toList();
            return KeybindPacketCodec.encodeSync(new KeybindSyncData(version, actionDefinitions));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to build sync packet: " + e.getMessage());
            return null;
        }
    }
}
