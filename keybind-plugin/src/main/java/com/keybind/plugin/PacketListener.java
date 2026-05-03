package com.keybind.plugin;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class PacketListener implements PluginMessageListener {

    private final KeybindPlugin plugin;
    private final ActionExecutor actionExecutor;

    public PacketListener(KeybindPlugin plugin, ActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(plugin.getConfigManager().getChannel())) {
            return;
        }

        // Validate message
        if (message.length == 0 || message.length > 256) {
            plugin.getLogger().warning("Invalid packet from " + player.getName() + ": bad length");
            return;
        }

        String actionName = readString(message);
        if (actionName == null || actionName.isEmpty()) {
            plugin.getLogger().warning("Invalid packet from " + player.getName() + ": empty action");
            return;
        }

        // Sanitize: only allow alphanumeric and underscore
        if (!actionName.matches("^[a-zA-Z0-9_]+$")) {
            plugin.getLogger().warning("Invalid action name from " + player.getName() + ": " + actionName);
            return;
        }

        plugin.getLogger().fine("Packet from " + player.getName() + ": action=" + actionName);

        // Execute on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            actionExecutor.execute(player, actionName);
        });
    }

    /**
     * Read a length-prefixed UTF-8 string from byte array.
     * Format: [2-byte length][UTF-8 bytes]
     */
    private String readString(byte[] data) {
        if (data.length < 2) return null;

        int length = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        if (length <= 0 || 2 + length > data.length) return null;

        return new String(data, 2, length, StandardCharsets.UTF_8);
    }
}
