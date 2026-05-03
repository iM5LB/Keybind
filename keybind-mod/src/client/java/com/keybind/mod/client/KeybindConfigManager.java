package com.keybind.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.keybind.mod.KeybindMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class KeybindConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "keybind-mod.json";

    /** Whether to use packet-based communication (true) or chat commands (false). */
    private boolean usePackets = true;

    /** Map of GLFW key name -> action name. Example: "K" -> "spawn" */
    private Map<String, String> bindings = new LinkedHashMap<>();

    public void load() {
        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            // Create default config
            bindings.put("K", "spawn");
            bindings.put("L", "home");
            save();
            KeybindMod.LOGGER.info("Created default keybind config.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            KeybindConfig config = GSON.fromJson(reader, KeybindConfig.class);
            if (config != null) {
                this.usePackets = config.usePackets;
                if (config.bindings != null) {
                    this.bindings = config.bindings;
                }
            }
            KeybindMod.LOGGER.info("Loaded " + bindings.size() + " keybind(s) from config.");
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to load keybind config", e);
        }
    }

    public void save() {
        Path configPath = getConfigPath();
        KeybindConfig config = new KeybindConfig();
        config.usePackets = this.usePackets;
        config.bindings = this.bindings;

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to save keybind config", e);
        }
    }

    public Map<String, String> getBindings() {
        return bindings;
    }

    public boolean usePackets() {
        return usePackets;
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    /** JSON structure for the config file. */
    private static class KeybindConfig {
        boolean usePackets = true;
        Map<String, String> bindings = new LinkedHashMap<>();
    }
}
