package com.keybind.plugin;

import com.keybind.plugin.metrics.Metrics;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

public final class KeybindPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 25110;

    private ConfigManager configManager;
    private ActionExecutor actionExecutor;
    private SyncSender syncSender;
    private KeyCaptureService keyCaptureService;
    private com.keybind.plugin.update.UpdateChecker updateChecker;
    private com.keybind.plugin.gui.KeybindInventoryMenu inventoryMenu;
    private Metrics metrics;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        actionExecutor = new ActionExecutor(this, configManager);
        syncSender = new SyncSender(this);
        keyCaptureService = new KeyCaptureService(this);
        inventoryMenu = new com.keybind.plugin.gui.KeybindInventoryMenu(this);
        updateChecker = new com.keybind.plugin.update.UpdateChecker(this);

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
        getServer().getPluginManager().registerEvents(inventoryMenu, this);
        getServer().getPluginManager().registerEvents(updateChecker, this);

        updateChecker.checkForUpdates(false);

        // Initialize bStats metrics if enabled
        setupMetrics();

        Listener dialogListener = KeybindDialogs.createListener(this);
        if (dialogListener != null) {
            getServer().getPluginManager().registerEvents(dialogListener, this);
            getLogger().info("Minecraft dialog manager enabled (/kbind manage).");
        } else {
            getLogger().info("Minecraft dialogs unavailable — using interactive Chest GUI (/kbind manage).");
        }

        getLogger().info("Keybind plugin enabled (Channel: " + channel + ", Sync: " + syncChannel + ")");
    }

    @Override
    public void onDisable() {
        if (metrics != null) {
            metrics.shutdown();
        }
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

    public com.keybind.plugin.update.UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public com.keybind.plugin.gui.KeybindInventoryMenu getInventoryMenu() {
        return inventoryMenu;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setupMetrics() {
        if (configManager.isMetricsEnabled()) {
            if (metrics == null) {
                try {
                    metrics = new Metrics(this, BSTATS_PLUGIN_ID);
                    metrics.addCustomChart(new Metrics.SingleLineChart("actions_count", () -> configManager.getActions().size()));
                } catch (Exception e) {
                    getLogger().warning("Could not initialize bStats metrics: " + e.getMessage());
                }
            }
        } else {
            if (metrics != null) {
                metrics.shutdown();
                metrics = null;
            }
        }
    }
}
