package com.keybind.mod.common;

public final class KeybindConstants {

    public static final String MOD_ID = "keybind";
    public static final String ACTION_CHANNEL_PATH = "main";
    public static final String SYNC_CHANNEL_PATH = "sync";
    public static final String ACTION_CHANNEL = MOD_ID + ":" + ACTION_CHANNEL_PATH;
    public static final String SYNC_CHANNEL = MOD_ID + ":" + SYNC_CHANNEL_PATH;
    public static final String CLIENT_CONFIG_FILE = "keybind.json";
    public static final String SERVERS_DIR = "keybind-servers";

    private KeybindConstants() {
    }
}
