package com.keybind.plugin;

import com.keybind.mod.common.KeybindConstants;
import com.keybind.mod.common.network.KeybindPacketCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyCaptureService implements PluginMessageListener, Listener {

    private static final long TIMEOUT_TICKS = 20L * 15;

    private final KeybindPlugin plugin;
    private final Map<UUID, KeyCaptureDraft> pending = new ConcurrentHashMap<>();

    public KeyCaptureService(KeybindPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, KeyCaptureDraft draft) {
        pending.put(player.getUniqueId(), draft);
        try {
            player.sendPluginMessage(
                    plugin,
                    KeybindConstants.CAPTURE_CHANNEL,
                    KeybindPacketCodec.encodeAction(KeybindConstants.CAPTURE_START)
            );
        } catch (IOException e) {
            pending.remove(player.getUniqueId());
            player.sendMessage(Component.text("Could not start key capture.", NamedTextColor.RED));
            if (KeybindDialogs.isSupported()) {
                KeybindDialogs.resumeAfterCapture(player, plugin, draft, draft.defaultKey);
            } else {
                com.keybind.plugin.gui.KeybindInventoryMenu.open(player, plugin, draft.page);
            }
            return;
        }

        player.sendActionBar(Component.text("Press a key to set the default bind. ESC cancels.", NamedTextColor.YELLOW));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> timeout(player.getUniqueId()), TIMEOUT_TICKS);
    }

    public void complete(Player player, String keyName) {
        KeyCaptureDraft draft = pending.remove(player.getUniqueId());
        if (draft == null) {
            return;
        }
        if (keyName == null || keyName.isEmpty() || KeybindConstants.CAPTURE_CANCEL.equalsIgnoreCase(keyName)) {
            player.sendMessage(Component.text("[Keybind] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .append(Component.text("Key capture cancelled.", NamedTextColor.GRAY)));
            com.keybind.plugin.gui.KeybindInventoryMenu.open(player, plugin, draft.page);
            return;
        }
        player.sendMessage(Component.text("[Keybind] ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("Captured key: ", NamedTextColor.GREEN))
                .append(Component.text(keyName, NamedTextColor.WHITE, TextDecoration.BOLD)));

        ConfigManager.ActionConfig existing = plugin.getConfigManager().getAction(draft.name);
        if (existing != null) {
            ConfigManager.ActionConfig updated = new ConfigManager.ActionConfig(
                    existing.getName(), existing.getDisplayName(), existing.getCommand(),
                    keyName, existing.getPermission(), existing.getCooldown(), existing.isConsole()
            );
            plugin.getConfigManager().saveAction(updated);
            plugin.getSyncSender().sendSyncToAll();
        }
        com.keybind.plugin.gui.KeybindInventoryMenu.open(player, plugin, draft.page);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!KeybindConstants.CAPTURE_CHANNEL.equals(channel)) {
            return;
        }
        String keyName;
        try {
            keyName = KeybindPacketCodec.decodeAction(message);
        } catch (IOException e) {
            plugin.getLogger().warning("Invalid capture packet from " + player.getName());
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> complete(player, keyName));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    private void timeout(UUID playerId) {
        KeyCaptureDraft draft = pending.remove(playerId);
        if (draft == null) {
            return;
        }
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        player.sendMessage(Component.text("[Keybind] Key capture timed out.", NamedTextColor.YELLOW));
        com.keybind.plugin.gui.KeybindInventoryMenu.open(player, plugin, draft.page);
    }
}
