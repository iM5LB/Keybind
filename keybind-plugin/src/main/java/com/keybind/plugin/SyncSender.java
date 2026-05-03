package com.keybind.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            writeString(dos, version);
            writeVarInt(dos, actions.size());

            for (ConfigManager.ActionConfig action : actions.values()) {
                writeString(dos, action.getName());
                writeString(dos, action.getDisplayName());
                writeString(dos, action.getDefaultKey());
            }

            return baos.toByteArray();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to build sync packet: " + e.getMessage());
            return null;
        }
    }

    private void writeVarInt(DataOutputStream dos, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            dos.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        dos.writeByte(value);
    }

    private void writeString(DataOutputStream dos, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(dos, bytes.length);
        dos.write(bytes);
    }
}
