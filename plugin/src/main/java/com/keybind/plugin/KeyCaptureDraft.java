package com.keybind.plugin;

public final class KeyCaptureDraft {

    public enum Screen {
        ADD,
        EDIT,
        VIEW
    }

    public final Screen screen;
    public final String name;
    public final String displayName;
    public final String command;
    public final String defaultKey;
    public final String permission;
    public final String cooldown;
    public final boolean console;
    public final int page;

    public KeyCaptureDraft(
            Screen screen,
            String name,
            String displayName,
            String command,
            String defaultKey,
            String permission,
            String cooldown,
            boolean console,
            int page
    ) {
        this.screen = screen;
        this.name = name;
        this.displayName = displayName;
        this.command = command;
        this.defaultKey = defaultKey;
        this.permission = permission;
        this.cooldown = cooldown;
        this.console = console;
        this.page = page;
    }

    public KeyCaptureDraft withKey(String key) {
        return new KeyCaptureDraft(screen, name, displayName, command, key, permission, cooldown, console, page);
    }
}
