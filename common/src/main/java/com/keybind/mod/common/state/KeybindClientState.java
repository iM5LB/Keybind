package com.keybind.mod.common.state;

import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.sync.KeybindSyncPlan;
import com.keybind.mod.common.sync.KeybindSyncPlanner;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class KeybindClientState {

    private final Map<String, String> displayNames = new LinkedHashMap<>();
    private final Map<String, String> persistedBindings = new LinkedHashMap<>();

    private String currentServer;
    private boolean synced;

    public KeybindSyncPlan startSync(
            String serverAddress,
            Map<String, String> savedBindings,
            Collection<String> registeredActions,
            List<KeybindActionDefinition> actions
    ) {
        currentServer = serverAddress;
        synced = true;

        KeybindSyncPlan syncPlan = KeybindSyncPlanner.plan(savedBindings, registeredActions, actions);

        persistedBindings.clear();
        persistedBindings.putAll(syncPlan.bindings());

        displayNames.clear();
        for (Map.Entry<String, String> entry : syncPlan.displayNames().entrySet()) {
            displayNames.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }

        return syncPlan;
    }

    public String getDisplayName(String actionName) {
        if (actionName == null) {
            return null;
        }
        return displayNames.get(actionName.toLowerCase(Locale.ROOT));
    }

    public void seedDisplayNames(Map<String, String> names) {
        if (names == null) {
            return;
        }
        for (Map.Entry<String, String> entry : names.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            displayNames.putIfAbsent(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
    }

    public Map<String, String> snapshotDisplayNames() {
        return new LinkedHashMap<>(displayNames);
    }

    public void removeAction(String actionName) {
        displayNames.remove(actionName.toLowerCase(Locale.ROOT));
        persistedBindings.remove(actionName);
    }

    public boolean updateBinding(String actionName, String currentKey) {
        String existing = persistedBindings.get(actionName);
        if (Objects.equals(existing, currentKey)) {
            return false;
        }
        persistedBindings.put(actionName, currentKey);
        return true;
    }

    public Map<String, String> snapshotPersistedBindings() {
        return new LinkedHashMap<>(persistedBindings);
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public boolean isSynced() {
        return synced;
    }

    public void clearSync() {
        currentServer = null;
        synced = false;
    }
}
