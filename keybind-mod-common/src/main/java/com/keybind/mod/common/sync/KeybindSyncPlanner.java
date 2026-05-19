package com.keybind.mod.common.sync;

import com.keybind.mod.common.KeybindActionDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KeybindSyncPlanner {

    private KeybindSyncPlanner() {
    }

    public static KeybindSyncPlan plan(
            Map<String, String> savedBindings,
            Collection<String> registeredActions,
            List<KeybindActionDefinition> actions
    ) {
        Map<String, String> plannedBindings = new LinkedHashMap<>();
        if (savedBindings != null) {
            plannedBindings.putAll(savedBindings);
        }

        Map<String, String> displayNames = new LinkedHashMap<>();
        List<String> newActions = new ArrayList<>();
        List<String> removedActions = new ArrayList<>();
        boolean configChanged = false;

        for (KeybindActionDefinition action : actions) {
            String name = action.name();
            displayNames.put(name, action.displayName());

            if (!plannedBindings.containsKey(name)) {
                plannedBindings.put(name, action.defaultKey());
                newActions.add(name);
                configChanged = true;
            }
        }

        for (String actionName : new ArrayList<>(registeredActions)) {
            if (!displayNames.containsKey(actionName)) {
                removedActions.add(actionName);
            }
        }

        for (String actionName : removedActions) {
            if (plannedBindings.remove(actionName) != null) {
                configChanged = true;
            }
        }

        return new KeybindSyncPlan(
                plannedBindings,
                displayNames,
                newActions,
                removedActions,
                configChanged
        );
    }
}
