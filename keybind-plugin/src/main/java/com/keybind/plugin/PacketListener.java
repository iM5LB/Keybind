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

        if (message.length == 0 || message.length > 256) {
            plugin.getLogger().warning("Invalid packet from " + player.getName() + ": bad length");
            return;
        }

        String actionName = readVarIntString(message);
        if (actionName == null || actionName.isEmpty()) {
            plugin.getLogger().warning("Invalid packet from " + player.getName() + ": empty action");
            return;
        }

        if (!actionName.matches("^[a-zA-Z0-9_]+$")) {
            plugin.getLogger().warning("Invalid action name from " + player.getName() + ": " + actionName);
            return;
        }

        plugin.getLogger().fine("Packet from " + player.getName() + ": action=" + actionName);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            actionExecutor.execute(player, actionName);
        });
    }

    /**
     * Read a VarInt-prefixed UTF-8 string from byte array.
     * This matches Minecraft's FriendlyByteBuf.writeUtf() format used by CustomPacketPayload.
     */
    private String readVarIntString(byte[] data) {
        int index = 0;
        int length = 0;
        int shift = 0;

        // Decode VarInt
        while (index < data.length) {
            byte b = data[index++];
            length |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
            if (shift > 21) return null; // VarInt too large
        }

        if (length <= 0 || index + length > data.length) return null;

        return new String(data, index, length, StandardCharsets.UTF_8);
    }
}
