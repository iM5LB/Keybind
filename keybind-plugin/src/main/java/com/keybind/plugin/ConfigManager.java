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
            ConfigurationSection section = actionsSection.getConfigurationSection(key);
            if (section == null) continue;

            actions.put(key.toLowerCase(), new ActionConfig(
                    key,
                    section.getString("display-name", formatDefaultName(key)),
                    section.getString("command", key),
                    section.getString("default-key", ""),
                    section.getString("permission", ""),
                    section.getLong("cooldown", globalCooldown),
                    section.getBoolean("console", false)
            ));
        }

        plugin.getLogger().info("Loaded " + actions.size() + " actions.");
    }

    private String formatDefaultName(String name) {
        if (name == null || name.isEmpty()) return "Unknown";
        String[] parts = name.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
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
        private final String displayName;
        private final String command;
        private final String defaultKey;
        private final String permission;
        private final long cooldown;
        private final boolean console;

        public ActionConfig(String name, String displayName, String command, String defaultKey, String permission, long cooldown, boolean console) {
            this.name = name;
            this.displayName = displayName;
            this.command = command;
            this.defaultKey = defaultKey;
            this.permission = permission;
            this.cooldown = cooldown;
            this.console = console;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getCommand() { return command; }
        public String getDefaultKey() { return defaultKey; }
        public String getPermission() { return permission; }
        public long getCooldown() { return cooldown; }
        public boolean isConsole() { return console; }
    }
}
