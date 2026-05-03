package com.keybind.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public final class KeybindPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ActionExecutor actionExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        actionExecutor = new ActionExecutor(this, configManager);

        // Register /kbind command
        KeybindCommand command = new KeybindCommand(this, actionExecutor, configManager);
        getCommand("kbind").setExecutor(command);
        getCommand("kbind").setTabCompleter(command);

        // Register plugin messaging channel for packet-based communication
        String channel = configManager.getChannel();
        Messenger messenger = getServer().getMessenger();
        messenger.registerIncomingPluginChannel(this, channel,
                new PacketListener(this, actionExecutor));
        messenger.registerOutgoingPluginChannel(this, channel);

        getLogger().info("Keybind plugin enabled! Channel: " + channel);
    }

    @Override
    public void onDisable() {
        String channel = configManager.getChannel();
        getServer().getMessenger().unregisterIncomingPluginChannel(this, channel);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, channel);
        getLogger().info("Keybind plugin disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }
}
