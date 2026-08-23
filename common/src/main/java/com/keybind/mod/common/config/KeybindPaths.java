package com.keybind.mod.common.config;

import com.keybind.mod.common.KeybindConstants;

import java.nio.file.Path;

public final class KeybindPaths {

    private KeybindPaths() {
    }

    public static Path clientConfig(Path configDir) {
        return configDir.resolve(KeybindConstants.CLIENT_CONFIG_FILE);
    }

    public static Path serversDir(Path configDir) {
        return configDir.resolve(KeybindConstants.SERVERS_DIR);
    }

    public static Path serverConfig(Path configDir, String serverAddress) {
        return serversDir(configDir).resolve(sanitizeServerAddress(serverAddress) + ".json");
    }

    public static String sanitizeServerAddress(String serverAddress) {
        return serverAddress.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
