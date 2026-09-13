package com.keybind.plugin;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public final class KeybindPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ActionExecutor actionExecutor;
    private SyncSender syncSender;
    private KeyCaptureService keyCaptureService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        actionExecutor = new ActionExecutor(this, configManager);
        syncSender = new SyncSender(this);
        keyCaptureService = new KeyCaptureService(this);

        KeybindCommand command = new KeybindCommand(this, actionExecutor, configManager);
        getCommand("kbind").setExecutor(command);
        getCommand("kbind").setTabCompleter(command);

        Messenger messenger = getServer().getMessenger();
        String channel = configManager.getChannel();
        String syncChannel = configManager.getSyncChannel();
        String captureChannel = configManager.getCaptureChannel();

        messenger.registerIncomingPluginChannel(this, channel, new PacketListener(this, actionExecutor));
        messenger.registerIncomingPluginChannel(this, captureChannel, keyCaptureService);
        messenger.registerOutgoingPluginChannel(this, channel);
        messenger.registerOutgoingPluginChannel(this, syncChannel);
        messenger.registerOutgoingPluginChannel(this, captureChannel);

        getServer().getPluginManager().registerEvents(syncSender, this);
        getServer().getPluginManager().registerEvents(keyCaptureService, this);

        Listener dialogListener = KeybindDialogs.createListener(this);
        if (dialogListener != null) {
            getServer().getPluginManager().registerEvents(dialogListener, this);
            getLogger().info("Minecraft dialog manager enabled (/kbind manage).");
        } else {
            getLogger().info("Minecraft dialogs unavailable — use /kbind add|set|remove on this server version.");
        }

        getLogger().info("Keybind plugin enabled (Channel: " + channel + ", Sync: " + syncChannel + ")");
    }

    @Override
    public void onDisable() {
        Messenger messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this, configManager.getChannel());
        messenger.unregisterIncomingPluginChannel(this, configManager.getCaptureChannel());
        messenger.unregisterOutgoingPluginChannel(this, configManager.getChannel());
        messenger.unregisterOutgoingPluginChannel(this, configManager.getSyncChannel());
        messenger.unregisterOutgoingPluginChannel(this, configManager.getCaptureChannel());
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

    public KeyCaptureService getKeyCaptureService() {
        return keyCaptureService;
    }
}
