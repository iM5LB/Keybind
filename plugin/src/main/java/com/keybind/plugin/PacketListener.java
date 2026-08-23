package com.keybind.plugin;

import com.keybind.mod.common.network.KeybindPacketCodec;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

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

        String actionName;
        try {
            actionName = KeybindPacketCodec.decodeAction(message);
        } catch (IOException e) {
            plugin.getLogger().warning("Invalid packet from " + player.getName() + ": decode failed");
            return;
        }

        if (actionName.isEmpty()) {
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
}
