package com.keybind.plugin;

import com.keybind.mod.common.KeybindConstants;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ConfigManager {

    public static final Pattern ACTION_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    public static final Set<String> RESERVED_NAMES = Set.of(
            "reload", "list", "manage", "gui", "menu", "add", "remove", "set", "help"
    );

    private final KeybindPlugin plugin;
    private final Map<String, ActionConfig> actions = new HashMap<>();
    private long globalCooldown;
    private boolean updateCheckerEnabled = true;
    private boolean metricsEnabled = true;
    
    public ConfigManager(KeybindPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public static boolean isValidActionName(String name) {
        return name != null && ACTION_NAME_PATTERN.matcher(name).matches();
    }

    public static boolean isReservedName(String name) {
        return name != null && RESERVED_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    public void reload() {
        try {
            plugin.reloadConfig();
            FileConfiguration config = plugin.getConfig();

            globalCooldown = config.getLong("global-cooldown", 500);
            if (config.isBoolean("update-checker")) {
                updateCheckerEnabled = config.getBoolean("update-checker");
            } else {
                updateCheckerEnabled = config.getBoolean("update-checker.enabled", true);
            }
            metricsEnabled = config.getBoolean("metrics", true);

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

            plugin.getLogger().info("Successfully loaded " + actions.size() + " actions.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load configuration! Please check for syntax errors: " + e.getMessage());
        }
    }

    public String formatDefaultName(String name) {
        if (name == null || name.isEmpty()) return "Unknown";
        String[] parts = name.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    public String saveAction(ActionConfig action) {
        if (!isValidActionName(action.getName()) || isReservedName(action.getName())) {
            return "Action names must be letters, numbers, or underscores, and cannot be a reserved command.";
        }
        if (action.getCommand() == null || action.getCommand().isBlank()) {
            return "Command is required.";
        }
        if (action.getCooldown() < 0) {
            return "Cooldown cannot be negative.";
        }

        FileConfiguration config = plugin.getConfig();
        ActionConfig existing = getAction(action.getName());
        String path = "actions." + (existing != null ? existing.getName() : action.getName().toLowerCase(Locale.ROOT));
        config.set(path + ".command", action.getCommand());
        config.set(path + ".display-name", action.getDisplayName());
        config.set(path + ".default-key", action.getDefaultKey());
        config.set(path + ".permission", action.getPermission() == null ? "" : action.getPermission());
        config.set(path + ".cooldown", action.getCooldown());
        config.set(path + ".console", action.isConsole());
        plugin.saveConfig();
        reload();
        return null;
    }

    public String deleteAction(String name) {
        ActionConfig existing = getAction(name);
        if (existing == null) {
            return "Unknown action: " + name;
        }

        plugin.getConfig().set("actions." + existing.getName(), null);
        plugin.saveConfig();
        reload();
        return null;
    }

    public String setGlobalCooldown(long milliseconds) {
        if (milliseconds < 0) {
            return "Global cooldown cannot be negative.";
        }
        plugin.getConfig().set("global-cooldown", milliseconds);
        plugin.saveConfig();
        reload();
        return null;
    }

    public void setUpdateCheckerEnabled(boolean enabled) {
        this.updateCheckerEnabled = enabled;
        plugin.getConfig().set("update-checker", enabled);
        plugin.saveConfig();
    }

    public void setMetricsEnabled(boolean enabled) {
        this.metricsEnabled = enabled;
        plugin.getConfig().set("metrics", enabled);
        plugin.saveConfig();
        plugin.setupMetrics();
    }

    public ActionConfig getAction(String name) {
        if (name == null) {
            return null;
        }
        return actions.get(name.toLowerCase(Locale.ROOT));
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

    public boolean isUpdateCheckerEnabled() {
        return updateCheckerEnabled;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public String getChannel() {
        return KeybindConstants.ACTION_CHANNEL;
    }

    public String getSyncChannel() {
        return KeybindConstants.SYNC_CHANNEL;
    }

    public String getCaptureChannel() {
        return KeybindConstants.CAPTURE_CHANNEL;
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
