package com.keybind.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.keybind.mod.common.config.KeybindClientConfigData;
import com.keybind.mod.common.config.KeybindPaths;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig extends KeybindClientConfigData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = KeybindPaths.clientConfig(FabricLoader.getInstance().getConfigDir());

    public static ModConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) return config;
            } catch (IOException e) {
                KeybindMod.LOGGER.error("Failed to load config", e);
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to save config", e);
        }
    }
}
