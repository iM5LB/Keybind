package com.keybind.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public final class KeybindPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ActionExecutor actionExecutor;
    private SyncSender syncSender;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        actionExecutor = new ActionExecutor(this, configManager);
        syncSender = new SyncSender(this);

        KeybindCommand command = new KeybindCommand(this, actionExecutor, configManager);
        getCommand("kbind").setExecutor(command);
        getCommand("kbind").setTabCompleter(command);

        Messenger messenger = getServer().getMessenger();
        String channel = configManager.getChannel();
        String syncChannel = configManager.getSyncChannel();

        messenger.registerIncomingPluginChannel(this, channel, new PacketListener(this, actionExecutor));
        messenger.registerOutgoingPluginChannel(this, channel);
        messenger.registerOutgoingPluginChannel(this, syncChannel);

        getServer().getPluginManager().registerEvents(syncSender, this);

        getLogger().info("Keybind plugin enabled (Channel: " + channel + ", Sync: " + syncChannel + ")");
    }

    @Override
    public void onDisable() {
        Messenger messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this, configManager.getChannel());
        messenger.unregisterOutgoingPluginChannel(this, configManager.getChannel());
        messenger.unregisterOutgoingPluginChannel(this, configManager.getSyncChannel());
        getLogger().info("Keybind plugin disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    public SyncSender getSyncSender() {
        return syncSender;
    }
}
