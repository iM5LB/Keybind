package com.keybind.plugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private final KeybindPlugin plugin;
    private final Map<String, ActionConfig> actions = new HashMap<>();
    private long globalCooldown;
    private String channel;
    private String syncChannel;

    public ConfigManager(KeybindPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        globalCooldown = config.getLong("global-cooldown", 500);
        channel = config.getString("channel", "keybind:main");
        syncChannel = config.getString("sync-channel", "keybind:sync");

        actions.clear();
        ConfigurationSection actionsSection = config.getConfigurationSection("actions");
        if (actionsSection == null) {
            plugin.getLogger().warning("No actions defined in config.yml!");
            return;
        }

        for (String key : actionsSection.getKeys(false)) {
            ConfigurationSection actionSection = actionsSection.getConfigurationSection(key);
            if (actionSection == null) continue;

            String command = actionSection.getString("command", key);
            String defaultKey = actionSection.getString("default-key", "");
            String permission = actionSection.getString("permission", "");
            long cooldown = actionSection.getLong("cooldown", globalCooldown);
            boolean console = actionSection.getBoolean("console", false);

            actions.put(key.toLowerCase(), new ActionConfig(key, command, defaultKey, permission, cooldown, console));
        }

        plugin.getLogger().info("Loaded " + actions.size() + " actions.");
    }

    public ActionConfig getAction(String name) {
        return actions.get(name.toLowerCase());
    }

    public Map<String, ActionConfig> getActions() {
        return Collections.unmodifiableMap(actions);
    }

    public Set<String> getActionNames() {
        return Collections.unmodifiableSet(actions.keySet());
    }

    public long getGlobalCooldown() {
        return globalCooldown;
    }

    public String getChannel() {
        return channel;
    }

    public String getSyncChannel() {
        return syncChannel;
    }

    public static class ActionConfig {
        private final String name;
        private final String command;
        private final String defaultKey;
        private final String permission;
        private final long cooldown;
        private final boolean console;

        public ActionConfig(String name, String command, String defaultKey, String permission, long cooldown, boolean console) {
            this.name = name;
            this.command = command;
            this.defaultKey = defaultKey;
            this.permission = permission;
            this.cooldown = cooldown;
            this.console = console;
        }

        public String getName() { return name; }
        public String getCommand() { return command; }
        public String getDefaultKey() { return defaultKey; }
        public String getPermission() { return permission; }
        public long getCooldown() { return cooldown; }
        public boolean isConsole() { return console; }
    }
}
