package com.keybind.mod.client;

import com.keybind.mod.KeybindMod;
import com.keybind.mod.common.KeybindActionDefinition;
import com.keybind.mod.common.state.KeybindClientState;
import com.keybind.mod.common.sync.KeybindSyncPlan;
import com.keybind.mod.network.KeybindActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.*;

public class KeybindManager {

    private static final String KEYBIND_CATEGORY_ID = "key.categories.keybind.actions";
    /** String on &lt;1.21.9, KeyMapping.Category on 1.21.9+ / 26.x (resolved lazily). */
    private static final Object KEYBIND_CATEGORY = resolveKeybindCategory();
    private static final KeybindClientState CLIENT_STATE = new KeybindClientState();

    private final Map<String, KeyMapping> registeredMappings = new LinkedHashMap<>();
    private static final RegistrationHandler DEFAULT_REGISTRATION_HANDLER = new RegistrationHandler() {
        @Override
        public void register(Minecraft client, KeyMapping mapping) {
            injectKeyMapping(client, mapping);
        }

        @Override
        public void unregister(Minecraft client, KeyMapping mapping) {
            removeKeyMapping(client, mapping);
        }
    };

    private static RegistrationHandler registrationHandler = DEFAULT_REGISTRATION_HANDLER;

    public interface RegistrationHandler {
        void register(Minecraft client, KeyMapping mapping);
        void unregister(Minecraft client, KeyMapping mapping);
    }

    public static void setRegistrationHandler(RegistrationHandler handler) {
        registrationHandler = handler == null ? DEFAULT_REGISTRATION_HANDLER : handler;
    }

    public static String getDisplayName(String actionName) {
        return CLIENT_STATE.getDisplayName(actionName);
    }

    public static Object getKeybindCategory() {
        return KEYBIND_CATEGORY;
    }

    public static String formatActionName(String actionName) {
        if (actionName == null || actionName.isEmpty()) {
            return actionName;
        }
        String[] parts = actionName.split("[_\\-.]+");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (formatted.length() > 0) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                formatted.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return formatted.length() > 0 ? formatted.toString() : actionName;
    }

    public void registerAllKnownActions() {
        CLIENT_STATE.seedDisplayNames(ServerKeybindStorage.loadAllDisplayNames());
        Map<String, String> allActions = ServerKeybindStorage.loadAllActions();
        if (allActions.isEmpty()) {
            KeybindMod.LOGGER.info("No known actions to pre-register.");
            return;
        }
        for (Map.Entry<String, String> entry : allActions.entrySet()) {
            registerAction(entry.getKey(), entry.getValue());
        }
        KeybindMod.LOGGER.info("Pre-registered {} known actions.", allActions.size());
    }

    private void registerAction(String actionName, String keyName) {
        InputConstants.Key key = resolveKey(keyName);

        if (registeredMappings.containsKey(actionName)) {
            registeredMappings.get(actionName).setKey(key);
        } else {
            KeyMapping mapping = createKeyMapping("key.keybind." + actionName, key, KEYBIND_CATEGORY);

            Minecraft client = Minecraft.getInstance();
            registrationHandler.register(client, mapping);

            registeredMappings.put(actionName, mapping);
        }
    }

    private KeyMapping createKeyMapping(String name, InputConstants.Key key, Object category) {
        return ensureKeyMappingDefaults(createKeyMappingFallback(name, key, category));
    }

    private KeyMapping createKeyMappingFallback(String name, InputConstants.Key key, Object category) {
        KeybindMod.LOGGER.info("Attempting KeyMapping constructor discovery for {}", name);
        Object conflictContext = resolveForgeConflictContext();
        Object noModifier = resolveForgeKeyModifierNone();
        List<String> failures = new ArrayList<>();

        for (java.lang.reflect.Constructor<?> constructor : KeyMapping.class.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            Object adaptedCategory = adaptCategory(params, category);
            try {
                if (params.length == 6
                        && params[0] == String.class
                        && isConflictContextType(params[1])
                        && isKeyModifierType(params[2])
                        && params[3].isAssignableFrom(key.getClass())
                        && isCategoryType(params[4])
                        && params[5] == int.class
                        && conflictContext != null
                        && noModifier != null
                        && adaptedCategory != null) {
                    return (KeyMapping) constructor.newInstance(name, conflictContext, noModifier, key, adaptedCategory, 0);
                }
                if (params.length == 5
                        && params[0] == String.class
                        && isConflictContextType(params[1])
                        && params[2].isAssignableFrom(key.getClass())
                        && isCategoryType(params[3])
                        && params[4] == int.class
                        && conflictContext != null
                        && adaptedCategory != null) {
                    return (KeyMapping) constructor.newInstance(name, conflictContext, key, adaptedCategory, 0);
                }
                // 1.21.9+: (String, Type, int, Category, int order)
                if (params.length == 5
                        && params[0] == String.class
                        && params[1] == InputConstants.Type.class
                        && params[2] == int.class
                        && isCategoryType(params[3])
                        && params[4] == int.class
                        && adaptedCategory != null) {
                    return (KeyMapping) constructor.newInstance(name, key.getType(), key.getValue(), adaptedCategory, 0);
                }
                if (params.length == 4
                        && params[0] == String.class
                        && params[1] == InputConstants.Type.class
                        && params[2] == int.class
                        && adaptedCategory != null
                        && params[3].isInstance(adaptedCategory)) {
                    return (KeyMapping) constructor.newInstance(name, key.getType(), key.getValue(), adaptedCategory);
                }
                if (params.length == 3 && params[0] == String.class && adaptedCategory != null) {
                    if (params[1].isAssignableFrom(key.getClass()) && params[2].isInstance(adaptedCategory)) {
                        return (KeyMapping) constructor.newInstance(name, key, adaptedCategory);
                    }
                    if (params[1] == int.class && params[2].isInstance(adaptedCategory)) {
                        return (KeyMapping) constructor.newInstance(name, key.getValue(), adaptedCategory);
                    }
                }
            } catch (Exception ex) {
                failures.add(constructor + " -> " + rootMessage(ex));
            }
        }

        for (java.lang.reflect.Constructor<?> constructor : KeyMapping.class.getConstructors()) {
            failures.add("available: " + constructor);
        }
        KeybindMod.LOGGER.error("KeyMapping creation failed for {} (category={} / resolved={}). Attempts: {}",
                name, category, KEYBIND_CATEGORY, failures);
        throw new RuntimeException("CRITICAL: Failed to create KeyMapping for " + name + " on this Minecraft version.");
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private static boolean isConflictContextType(Class<?> type) {
        String name = type.getSimpleName();
        return "IKeyConflictContext".equals(name) || name.contains("ConflictContext");
    }

    private static boolean isKeyModifierType(Class<?> type) {
        String name = type.getSimpleName();
        return "KeyModifier".equals(name) || name.contains("KeyModifier");
    }

    private static boolean isCategoryType(Class<?> type) {
        return type == String.class || type.getDeclaringClass() == KeyMapping.class;
    }

    private static Object adaptCategory(Class<?>[] params, Object category) {
        if (params.length == 0) {
            return category;
        }
        Class<?> categoryParam = params[params.length - 1];
        if (categoryParam == int.class && params.length >= 2) {
            categoryParam = params[params.length - 2];
        }

        if (categoryParam == String.class) {
            return category instanceof String ? category : KEYBIND_CATEGORY_ID;
        }
        if (isCategoryType(categoryParam) && categoryParam != String.class) {
            if (category != null && categoryParam.isInstance(category)) {
                return category;
            }
            if (KEYBIND_CATEGORY != null && categoryParam.isInstance(KEYBIND_CATEGORY)) {
                return KEYBIND_CATEGORY;
            }
            Object resolved = resolveKeybindCategoryObject();
            if (resolved != null && categoryParam.isInstance(resolved)) {
                return resolved;
            }
        }
        return category;
    }

    private static Object resolveKeybindCategory() {
        Object modern = resolveKeybindCategoryObject();
        Object resolved = modern != null ? modern : KEYBIND_CATEGORY_ID;
        ensureCategoryListed(resolved);
        return resolved;
    }

    /**
     * Controls UI (1.21.9+) only shows categories present in KeyMapping.Category.CATEGORIES.
     * Creating a Category via its constructor alone leaves it invisible in the Key Binds screen.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void ensureCategoryListed(Object category) {
        if (category == null || category instanceof String) {
            return;
        }
        try {
            Class<?> categoryClass = category.getClass();
            for (Field field : categoryClass.getDeclaredFields()) {
                if (!java.util.List.class.isAssignableFrom(field.getType())
                        || !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (!(value instanceof java.util.List<?> list)) {
                    continue;
                }
                for (Object existing : list) {
                    if (category.equals(existing) || categoriesEqual(category, existing)) {
                        return;
                    }
                }
                ((java.util.List) list).add(category);
                KeybindMod.LOGGER.info("Registered Keybind category into {}", field.getName());
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            KeybindMod.LOGGER.debug("Unable to list Keybind category: {}", ex.toString());
        }
    }

    private static Object resolveKeybindCategoryObject() {
        try {
            Class<?> categoryClass = findCategoryClass();
            if (categoryClass == null) {
                return null;
            }
            Class<?> idClass = findCategoryIdClass(categoryClass);
            if (idClass == null) {
                return null;
            }
            Object id = createResourceId(idClass, "keybind", "actions");
            if (id == null) {
                return null;
            }

            Object existing = findRegisteredCategory(categoryClass, id);
            if (existing != null) {
                return existing;
            }

            // Prefer static register(ID) when present.
            for (java.lang.reflect.Method method : categoryClass.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        || method.getParameterCount() != 1
                        || method.getReturnType() != categoryClass) {
                    continue;
                }
                if (!method.getParameterTypes()[0].isInstance(id)) {
                    continue;
                }
                try {
                    return method.invoke(null, id);
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    Object afterFail = findRegisteredCategory(categoryClass, id);
                    if (afterFail != null) {
                        return afterFail;
                    }
                }
            }

            for (java.lang.reflect.Constructor<?> constructor : categoryClass.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 1 && params[0].isInstance(id)) {
                    return constructor.newInstance(id);
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            KeybindMod.LOGGER.debug("Unable to resolve KeyMapping category object: {}", ex.toString());
        }
        return null;
    }

    private static Class<?> findCategoryClass() {
        for (java.lang.reflect.Constructor<?> constructor : KeyMapping.class.getConstructors()) {
            for (Class<?> param : constructor.getParameterTypes()) {
                if (param.getDeclaringClass() == KeyMapping.class) {
                    return param;
                }
            }
        }
        Class<?>[] nested = KeyMapping.class.getDeclaredClasses();
        return nested.length > 0 ? nested[0] : null;
    }

    private static Class<?> findCategoryIdClass(Class<?> categoryClass) {
        for (java.lang.reflect.Constructor<?> constructor : categoryClass.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length == 1 && params[0] != String.class) {
                return params[0];
            }
        }
        for (java.lang.reflect.Method method : categoryClass.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 1
                    || method.getReturnType() != categoryClass) {
                continue;
            }
            Class<?> param = method.getParameterTypes()[0];
            if (param != String.class) {
                return param;
            }
        }
        return null;
    }

    private static Object findRegisteredCategory(Class<?> categoryClass, Object id) {
        try {
            for (Field field : categoryClass.getDeclaredFields()) {
                if (!java.util.Collection.class.isAssignableFrom(field.getType())
                        && !java.util.Map.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof java.util.Map<?, ?> map) {
                    Object hit = map.get(id);
                    if (hit != null && categoryClass.isInstance(hit)) {
                        return hit;
                    }
                    for (Object entry : map.values()) {
                        if (categoryMatchesId(entry, id)) {
                            return entry;
                        }
                    }
                } else if (value instanceof java.util.Collection<?> collection) {
                    for (Object entry : collection) {
                        if (categoryMatchesId(entry, id)) {
                            return entry;
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static boolean categoryMatchesId(Object category, Object id) {
        if (category == null || id == null) {
            return false;
        }
        try {
            for (java.lang.reflect.Method method : category.getClass().getMethods()) {
                if (method.getParameterCount() != 0 || !method.getReturnType().isInstance(id)) {
                    continue;
                }
                if (id.equals(method.invoke(category))) {
                    return true;
                }
            }
            for (Field field : category.getClass().getDeclaredFields()) {
                if (!field.getType().isInstance(id)) {
                    continue;
                }
                field.setAccessible(true);
                if (id.equals(field.get(category))) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static Object createResourceId(Class<?> idClass, String namespace, String path) {
        try {
            for (java.lang.reflect.Method method : idClass.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        || method.getReturnType() != idClass) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                    return method.invoke(null, namespace, path);
                }
            }
            for (java.lang.reflect.Method method : idClass.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        || method.getReturnType() != idClass) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0] == String.class) {
                    return method.invoke(null, namespace + ":" + path);
                }
            }
            for (java.lang.reflect.Constructor<?> constructor : idClass.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                    return constructor.newInstance(namespace, path);
                }
                if (params.length == 1 && params[0] == String.class) {
                    return constructor.newInstance(namespace + ":" + path);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            KeybindMod.LOGGER.debug("Unable to create id {}.{}: {}", namespace, path, ex.toString());
        }
        return null;
    }

    private static Object resolveForgeConflictContext() {
        return resolveEnumConstant(
                "IN_GAME",
                "net.minecraftforge.client.settings.KeyConflictContext",
                "net.neoforged.neoforge.client.settings.KeyConflictContext"
        );
    }

    private static Object resolveForgeKeyModifierNone() {
        return resolveEnumConstant(
                "NONE",
                "net.minecraftforge.client.settings.KeyModifier",
                "net.neoforged.neoforge.client.settings.KeyModifier"
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object resolveEnumConstant(String name, String... classNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                return Enum.valueOf((Class) clazz.asSubclass(Enum.class), name);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static KeyMapping ensureKeyMappingDefaults(KeyMapping mapping) {
        Object fallback = resolveForgeConflictContext();
        if (fallback == null) {
            return mapping;
        }

        for (String interfaceName : new String[] {
                "net.minecraftforge.client.settings.IKeyConflictContext",
                "net.neoforged.neoforge.client.settings.IKeyConflictContext"
        }) {
            try {
                Class<?> conflictInterface = Class.forName(interfaceName);
                java.lang.reflect.Method getter = mapping.getClass().getMethod("getKeyConflictContext");
                if (getter.invoke(mapping) == null) {
                    mapping.getClass().getMethod("setKeyConflictContext", conflictInterface).invoke(mapping, fallback);
                }
                return mapping;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return mapping;
    }

    public static void injectKeyMapping(Minecraft client, KeyMapping mapping) {
        if (client == null || client.options == null) {
            return;
        }
        try {
            ensureCategoryListed(readCategory(mapping));

            Field field = findKeyMappingsField();
            if (field == null) {
                KeybindMod.LOGGER.error("Could not find Options.keyMappings field to inject {}", readMappingName(mapping));
                return;
            }

            field.setAccessible(true);
            KeyMapping[] original = (KeyMapping[]) field.get(client.options);
            if (ArrayUtils.contains(original, mapping)) {
                return;
            }

            int insertAt = findCategoryInsertIndex(original, mapping);
            KeyMapping[] updated = ArrayUtils.add(original, insertAt, mapping);
            field.set(client.options, updated);
            KeyMapping.resetMapping();
            KeybindMod.LOGGER.info("Dynamically registered action: {} (into {}[{}] @ {})",
                    readMappingName(mapping), field.getName(), updated.length, insertAt);
        } catch (Throwable e) {
            KeybindMod.LOGGER.error("Failed to dynamically register keybind via reflection", e);
        }
    }

    public static void removeKeyMapping(Minecraft client, KeyMapping mapping) {
        if (client == null || client.options == null) {
            return;
        }
        try {
            Field field = findKeyMappingsField();
            if (field == null) {
                return;
            }

            field.setAccessible(true);
            KeyMapping[] original = (KeyMapping[]) field.get(client.options);
            if (ArrayUtils.contains(original, mapping)) {
                KeyMapping[] updated = ArrayUtils.removeElement(original, mapping);
                field.set(client.options, updated);
                KeyMapping.resetMapping();
                KeybindMod.LOGGER.info("Dynamically removed action: {}", readMappingName(mapping));
            }
        } catch (Throwable e) {
            KeybindMod.LOGGER.error("Failed to dynamically remove keybind via reflection", e);
        }
    }

    private static Field findKeyMappingsField() {
        Field[] fields = Options.class.getDeclaredFields();
        Minecraft client = Minecraft.getInstance();
        Object options = client != null ? client.options : null;

        // When options exist, pick the largest KeyMapping[] field. On 1.21.9+ Options has both
        // keyMappings and debugKeys; intermediary field names hide which is which, and debugKeys
        // is declared first — injecting there makes binds work for input but hide them in Controls.
        if (options != null) {
            Field best = null;
            int bestLength = -1;
            for (Field field : fields) {
                if (field.getType() != KeyMapping[].class) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    KeyMapping[] value = (KeyMapping[]) field.get(options);
                    int length = value != null ? value.length : 0;
                    if (length > bestLength) {
                        best = field;
                        bestLength = length;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return best;
        }

        for (Field field : fields) {
            if (field.getType() != KeyMapping[].class) {
                continue;
            }
            String name = field.getName();
            if ("keyMappings".equals(name) || "allKeys".equals(name)) {
                return field;
            }
        }
        return null;
    }

    private static int findCategoryInsertIndex(KeyMapping[] mappings, KeyMapping mapping) {
        Object targetCategory = readCategory(mapping);
        if (targetCategory == null) {
            return mappings.length;
        }
        int lastSameCategory = -1;
        for (int i = 0; i < mappings.length; i++) {
            if (categoriesEqual(readCategory(mappings[i]), targetCategory)) {
                lastSameCategory = i;
            }
        }
        return lastSameCategory >= 0 ? lastSameCategory + 1 : mappings.length;
    }

    private static Object readCategory(KeyMapping mapping) {
        if (mapping == null) {
            return null;
        }

        // 1.21.9+: category is KeyMapping.Category (nested type). Read by field/method type so
        // intermediary-mapped getCategory() signatures do not NoSuchMethodError.
        for (Field field : KeyMapping.class.getDeclaredFields()) {
            if (field.getType().getDeclaringClass() != KeyMapping.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(mapping);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        for (java.lang.reflect.Method method : KeyMapping.class.getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType().getDeclaringClass() != KeyMapping.class) {
                continue;
            }
            try {
                Object value = method.invoke(mapping);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        // Pre-1.21.9: getCategory() returns String (matches compile target).
        try {
            return mapping.getCategory();
        } catch (Throwable ignored) {
            return KEYBIND_CATEGORY;
        }
    }

    private static String readMappingName(KeyMapping mapping) {
        try {
            return mapping.getName();
        } catch (Throwable ignored) {
            return String.valueOf(mapping);
        }
    }

    private static boolean categoriesEqual(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }
        return left.toString().equals(right.toString());
    }

    public void onServerSync(String serverAddress, String serverVersion, List<KeybindActionDefinition> actions) {
        KeybindMod.LOGGER.info("Starting sync for: {} (Version: {}, Actions: {})", serverAddress, serverVersion, actions.size());

        Map<String, String> savedBindings = ServerKeybindStorage.load(serverAddress);
        KeybindSyncPlan syncPlan = CLIENT_STATE.startSync(serverAddress, savedBindings, registeredMappings.keySet(), actions);

        for (KeybindActionDefinition action : actions) {
            String keyName = syncPlan.bindings().get(action.name());
            if (syncPlan.newActions().contains(action.name())) {
                KeybindMod.LOGGER.info("New action found: {} (default: {})", action.name(), keyName);
            }
            if (keyName != null && !keyName.isEmpty()) {
                registerAction(action.name(), keyName);
            }
        }

        Minecraft client = Minecraft.getInstance();
        for (String actionName : syncPlan.removedActions()) {
            KeyMapping mapping = registeredMappings.remove(actionName);
            KeybindMod.LOGGER.info("Removing obsolete action from client: {}", actionName);
            if (mapping != null && client.options != null) {
                registrationHandler.unregister(client, mapping);
            }
            CLIENT_STATE.removeAction(actionName);
            KeybindMod.LOGGER.info("Removed obsolete action from saved config: {}", actionName);
        }

        ServerKeybindStorage.save(serverAddress, syncPlan.bindings(), CLIENT_STATE.snapshotDisplayNames());
        
        KeyMapping.resetMapping();
        
        if (client.player != null) {
            String msg = "§a[Keybind] Synced " + actions.size() + " actions.";
            if (!syncPlan.newActions().isEmpty()) msg += " §7(" + syncPlan.newActions().size() + " new)";
            client.player.displayClientMessage(Component.literal(msg), false);
        }
        
        KeybindMod.LOGGER.info("Successfully synced {} actions for: {}", actions.size(), serverAddress);
    }

    public void onDisconnect() {
        CLIENT_STATE.clearSync();
        KeybindMod.LOGGER.info("Disconnected — keybind sync cleared.");
    }

    public void tick(Minecraft client) {
        if (!CLIENT_STATE.isSynced() || client.player == null || client.screen != null) return;

        boolean changed = false;
        for (Map.Entry<String, KeyMapping> entry : registeredMappings.entrySet()) {
            String action = entry.getKey();
            KeyMapping mapping = entry.getValue();

            if (CLIENT_STATE.getCurrentServer() != null) {
                String currentKey = mapping.saveString();
                changed |= CLIENT_STATE.updateBinding(action, currentKey);
            }

            while (mapping.consumeClick()) {
                sendAction(client, action);
            }
        }
        if (changed) {
            ServerKeybindStorage.save(
                    CLIENT_STATE.getCurrentServer(),
                    CLIENT_STATE.snapshotPersistedBindings(),
                    CLIENT_STATE.snapshotDisplayNames()
            );
            KeyMapping.resetMapping();
        }
    }

    public boolean isSynced() {
        return CLIENT_STATE.isSynced();
    }

    private void sendAction(Minecraft client, String action) {
        if (client.player == null) return;

        try {
            ClientPlayNetworking.send(new KeybindActionPayload(action));
            KeybindMod.LOGGER.debug("Sent action: {}", action);
        } catch (Exception e) {
            client.player.connection.sendCommand("kbind " + action);
            KeybindMod.LOGGER.debug("Sent command: /kbind {}", action);
        }
    }

    private InputConstants.Key resolveKey(String keyName) {
        if (keyName == null || keyName.isEmpty()) return InputConstants.UNKNOWN;
        String name = keyName.toUpperCase();

        // Check for internal Minecraft key strings first
        if (name.contains(".")) {
            try {
                return InputConstants.getKey(keyName);
            } catch (Exception ignored) {}
        }

        // Mouse Buttons
        return switch (name) {
            case "MOUSE_LEFT", "MOUSE_1" -> InputConstants.Type.MOUSE.getOrCreate(0);
            case "MOUSE_RIGHT", "MOUSE_2" -> InputConstants.Type.MOUSE.getOrCreate(1);
            case "MOUSE_MIDDLE", "MOUSE_3" -> InputConstants.Type.MOUSE.getOrCreate(2);
            case "MOUSE_4" -> InputConstants.Type.MOUSE.getOrCreate(3);
            case "MOUSE_5" -> InputConstants.Type.MOUSE.getOrCreate(4);
            case "MOUSE_6" -> InputConstants.Type.MOUSE.getOrCreate(5);
            case "MOUSE_7" -> InputConstants.Type.MOUSE.getOrCreate(6);
            case "MOUSE_8" -> InputConstants.Type.MOUSE.getOrCreate(7);

            // Function Keys
            case "F1" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F1);
            case "F2" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F2);
            case "F3" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F3);
            case "F4" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F4);
            case "F5" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F5);
            case "F6" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F6);
            case "F7" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F7);
            case "F8" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8);
            case "F9" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F9);
            case "F10" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F10);
            case "F11" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F11);
            case "F12" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F12);
            case "F13" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F13);
            case "F14" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F14);
            case "F15" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F15);
            case "F16" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F16);
            case "F17" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F17);
            case "F18" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F18);
            case "F19" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F19);
            case "F20" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F20);
            case "F21" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F21);
            case "F22" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F22);
            case "F23" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F23);
            case "F24" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F24);
            case "F25" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F25);

            // Alphanumeric
            case "A" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_A);
            case "B" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B);
            case "C" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C);
            case "D" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_D);
            case "E" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_E);
            case "F" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F);
            case "G" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G);
            case "H" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_H);
            case "I" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_I);
            case "J" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J);
            case "K" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_K);
            case "L" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_L);
            case "M" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_M);
            case "N" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_N);
            case "O" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_O);
            case "P" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_P);
            case "Q" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Q);
            case "R" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R);
            case "S" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_S);
            case "T" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_T);
            case "U" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_U);
            case "V" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_V);
            case "W" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_W);
            case "X" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X);
            case "Y" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Y);
            case "Z" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Z);
            case "0" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_0);
            case "1" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_1);
            case "2" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_2);
            case "3" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_3);
            case "4" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_4);
            case "5" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_5);
            case "6" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_6);
            case "7" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_7);
            case "8" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_8);
            case "9" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_9);

            // Symbols
            case "[", "LEFT_BRACKET" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET);
            case "]", "RIGHT_BRACKET" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_BRACKET);
            case "\\", "BACKSLASH" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_BACKSLASH);
            case ";", "SEMICOLON" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SEMICOLON);
            case "'", "APOSTROPHE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_APOSTROPHE);
            case ",", "COMMA" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_COMMA);
            case ".", "PERIOD" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PERIOD);
            case "/", "SLASH" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SLASH);
            case "`", "GRAVE_ACCENT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_GRAVE_ACCENT);
            case "-", "MINUS" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_MINUS);
            case "=", "EQUAL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_EQUAL);

            // Navigation & Special
            case "SPACE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SPACE);
            case "ENTER" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_ENTER);
            case "TAB" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_TAB);
            case "BACKSPACE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_BACKSPACE);
            case "INSERT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_INSERT);
            case "DELETE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_DELETE);
            case "RIGHT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT);
            case "LEFT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT);
            case "DOWN" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_DOWN);
            case "UP" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_UP);
            case "PAGE_UP" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PAGE_UP);
            case "PAGE_DOWN" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PAGE_DOWN);
            case "HOME" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_HOME);
            case "END" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_END);
            case "ESCAPE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_ESCAPE);
            case "CAPS_LOCK" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_CAPS_LOCK);
            case "SCROLL_LOCK" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SCROLL_LOCK);
            case "NUM_LOCK" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_NUM_LOCK);
            case "PRINT_SCREEN" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PRINT_SCREEN);
            case "PAUSE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_PAUSE);
            case "SCROLL_LOCK_EXTRA" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_SCROLL_LOCK);

            // Numpad
            case "KP_0", "NUMPAD_0" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_0);
            case "KP_1", "NUMPAD_1" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_1);
            case "KP_2", "NUMPAD_2" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_2);
            case "KP_3", "NUMPAD_3" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_3);
            case "KP_4", "NUMPAD_4" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_4);
            case "KP_5", "NUMPAD_5" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_5);
            case "KP_6", "NUMPAD_6" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_6);
            case "KP_7", "NUMPAD_7" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_7);
            case "KP_8", "NUMPAD_8" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_8);
            case "KP_9", "NUMPAD_9" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_9);
            case "KP_ADD", "NUMPAD_ADD" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_ADD);
            case "KP_DECIMAL", "NUMPAD_DECIMAL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_DECIMAL);
            case "KP_DIVIDE", "NUMPAD_DIVIDE" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_DIVIDE);
            case "KP_ENTER", "NUMPAD_ENTER" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_ENTER);
            case "KP_EQUAL", "NUMPAD_EQUAL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_EQUAL);
            case "KP_MULTIPLY", "NUMPAD_MULTIPLY" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_MULTIPLY);
            case "KP_SUBTRACT", "NUMPAD_SUBTRACT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_SUBTRACT);

            // Modifiers
            case "LEFT_SHIFT", "SHIFT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT);
            case "RIGHT_SHIFT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SHIFT);
            case "LEFT_CONTROL", "LEFT_CTRL", "CONTROL", "CTRL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL);
            case "RIGHT_CONTROL" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_CONTROL);
            case "LEFT_ALT", "ALT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT);
            case "RIGHT_ALT" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_ALT);
            case "LEFT_SUPER", "SUPER", "WINDOWS", "CMD" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SUPER);
            case "RIGHT_SUPER" -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SUPER);

            default -> {
                // Try literal key name (single char)
                if (name.length() == 1) {
                    int code = name.charAt(0);
                    try {
                        yield InputConstants.Type.KEYSYM.getOrCreate(code);
                    } catch (Exception ignored) {}
                }
                yield InputConstants.UNKNOWN;
            }
        };
    }
}
