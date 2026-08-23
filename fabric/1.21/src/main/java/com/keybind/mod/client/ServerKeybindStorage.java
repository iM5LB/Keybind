package com.keybind.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.config.KeybindPaths;
import com.keybind.mod.common.config.ServerKeybindFileData;
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

    public static Map<String, String> load(String serverAddress) {
        Path path = getPath(serverAddress);
        if (!Files.exists(path)) return null;

        try (Reader reader = Files.newBufferedReader(path)) {
            ServerKeybindFileData config = GSON.fromJson(reader, ServerKeybindFileData.class);
            return (config != null) ? config.bindings : null;
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to load config for: {}", serverAddress, e);
        }
        return null;
    }

    public static void save(String serverAddress, Map<String, String> bindings) {
        save(serverAddress, bindings, null);
    }

    public static void save(String serverAddress, Map<String, String> bindings, Map<String, String> displayNames) {
        Path path = getPath(serverAddress);
        ServerKeybindFileData config = new ServerKeybindFileData();
        config.serverAddress = serverAddress;
        config.bindings = bindings;
        if (displayNames != null) {
            config.displayNames = displayNames;
        }

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
        Path dir = KeybindPaths.serversDir(FabricLoader.getInstance().getConfigDir());
        if (!Files.exists(dir)) return allActions;

        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    ServerKeybindFileData config = GSON.fromJson(reader, ServerKeybindFileData.class);
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

    public static Map<String, String> loadAllDisplayNames() {
        Map<String, String> allDisplayNames = new LinkedHashMap<>();
        Path dir = KeybindPaths.serversDir(FabricLoader.getInstance().getConfigDir());
        if (!Files.exists(dir)) return allDisplayNames;

        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    ServerKeybindFileData config = GSON.fromJson(reader, ServerKeybindFileData.class);
                    if (config != null && config.displayNames != null) {
                        allDisplayNames.putAll(config.displayNames);
                    }
                } catch (IOException e) {
                    KeybindMod.LOGGER.error("Failed to load display names from: {}", path, e);
                }
            });
        } catch (IOException e) {
            KeybindMod.LOGGER.error("Failed to list configs", e);
        }
        return allDisplayNames;
    }

    private static Path getPath(String serverAddress) {
        return KeybindPaths.serverConfig(FabricLoader.getInstance().getConfigDir(), serverAddress);
    }
}
