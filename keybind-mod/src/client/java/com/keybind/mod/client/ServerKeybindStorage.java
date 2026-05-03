package com.keybind.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.keybind.mod.KeybindMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServerKeybindStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SERVERS_DIR = "keybind-servers";

    public static Map<String, String> load(String serverAddress) {
        Path path = getPath(serverAddress);
        if (!Files.exists(path)) return null;

        try (Reader reader = Files.newBufferedReader(path)) {
            ServerConfig config = GSON.fromJson(reader, ServerConfig.class);
            return (config != null) ? config.bindings : null;
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to load config for: {}", serverAddress, e);
        }
        return null;
    }

    public static void save(String serverAddress, Map<String, String> bindings) {
        Path path = getPath(serverAddress);
        ServerConfig config = new ServerConfig();
        config.serverAddress = serverAddress;
        config.bindings = bindings;

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to save config for: {}", serverAddress, e);
        }
    }

    public static Map<String, String> loadAllActions() {
        Map<String, String> allActions = new LinkedHashMap<>();
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(SERVERS_DIR);
        if (!Files.exists(dir)) return allActions;

        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    ServerConfig config = GSON.fromJson(reader, ServerConfig.class);
                    if (config != null && config.bindings != null) {
                        allActions.putAll(config.bindings);
                    }
                } catch (IOException e) {
                    KeybindMod.LOGGER.error("Failed to load: {}", path, e);
                }
            });
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to list configs", e);
        }
        return allActions;
    }

    private static Path getPath(String serverAddress) {
        String safeFileName = serverAddress.replaceAll("[^a-zA-Z0-9._\\-]", "_") + ".json";
        return FabricLoader.getInstance().getConfigDir().resolve(SERVERS_DIR).resolve(safeFileName);
    }

    private static class ServerConfig {
        String serverAddress;
        Map<String, String> bindings = new LinkedHashMap<>();
    }
}
