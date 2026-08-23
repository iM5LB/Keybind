package com.keybind.mod.common;

import java.util.List;

public record KeybindSyncData(String version, List<KeybindActionDefinition> actions) {
}
