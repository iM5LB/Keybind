package com.keybind.mod.common.sync;

import java.util.List;
import java.util.Map;

public record KeybindSyncPlan(
        Map<String, String> bindings,
        Map<String, String> displayNames,
        List<String> newActions,
        List<String> removedActions,
        boolean configChanged
) {
}
