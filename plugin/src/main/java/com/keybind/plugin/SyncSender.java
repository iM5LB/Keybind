package com.keybind.plugin;

import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.KeybindConstants;
import com.keybind.mod.common.KeybindSyncData;
import com.keybind.mod.common.network.KeybindPacketCodec;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SyncSender implements Listener, PluginMessageListener {

    private final KeybindPlugin plugin;
    private final Set<UUID> moddedPlayers = ConcurrentHashMap.newKeySet();

    public SyncSender(KeybindPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a client announces the Keybind mod via the hello channel
     * (plugin message or /kbind hello). Only these players ever receive
     * sync/capture packets, so vanilla clients are never spammed.
     */
    public void onHello(Player player) {
        markModded(player);
        // Plugin message listeners can run off the entity's region thread on
        // Folia; always send through the entity scheduler.
        SchedulerCompat.run(player, plugin, () -> sendSync(player));
        // Hello can arrive while the client is still finishing the join
        // transition; resend once shortly after so an early sync is not lost.
        SchedulerCompat.runLater(player, plugin, 20L, () -> {
            if (player.isOnline()) {
                sendSync(player);
            }
        });
    }

    public void markModded(Player player) {
        moddedPlayers.add(player.getUniqueId());
    }

    public boolean isModded(UUID playerId) {
        return moddedPlayers.contains(playerId);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!KeybindConstants.HELLO_CHANNEL.equals(channel)) {
            return;
        }
        if (message.length > 64) {
            plugin.getLogger().warning("Invalid hello packet from " + player.getName() + ": bad length");
            return;
        }
        plugin.getLogger().info("Keybind hello received from " + player.getName() + " — sending sync.");
        onHello(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        moddedPlayers.remove(id);
        plugin.getActionExecutor().clearCooldowns(id);
    }

    public void sendSync(Player player) {
        if (!isModded(player.getUniqueId())) {
            return;
        }
        byte[] data = buildSyncPacket();
        if (data == null) return;

        try {
            player.sendPluginMessage(plugin, KeybindConstants.SYNC_CHANNEL, data);
            plugin.getLogger().info("Sent sync to " + player.getName() + " (" + plugin.getConfigManager().getActions().size() + " actions)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send sync to " + player.getName() + ": " + e.getMessage());
        }
    }

    public void sendSyncToAll() {
        byte[] data = buildSyncPacket();
        if (data == null) return;

        int recipients = 0;
        for (UUID id : moddedPlayers) {
            Player player = plugin.getServer().getPlayer(id);
            if (player == null || !player.isOnline()) {
                continue;
            }
            recipients++;
            SchedulerCompat.run(player, plugin, () -> {
                try {
                    player.sendPluginMessage(plugin, KeybindConstants.SYNC_CHANNEL, data);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send sync to " + player.getName() + ": " + e.getMessage());
                }
            });
        }
        plugin.getLogger().info("Sent sync to " + recipients + " modded player(s).");
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

            KeybindSyncData syncData = new KeybindSyncData(version, actionDefinitions);
            return KeybindPacketCodec.encodeSync(syncData);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to build sync packet: " + e.getMessage());
            return null;
        }
    }
}
