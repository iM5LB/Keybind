package com.keybind.mod.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plugin-facing key names used in config.yml {@code default-key}.
 * GLFW codes match the official glfw3.h values so the client can reverse a press.
 */
public final class KeyNames {

    private static final Map<Integer, String> GLFW_KEYS = new LinkedHashMap<>();

    static {
        putRange(48, 57, i -> String.valueOf((char) i));
        putRange(65, 90, i -> String.valueOf((char) i));
        putRange(290, 314, i -> "F" + (i - 289));
        putRange(320, 329, i -> "KP_" + (i - 320));

        GLFW_KEYS.put(32, "SPACE");
        GLFW_KEYS.put(39, "APOSTROPHE");
        GLFW_KEYS.put(44, "COMMA");
        GLFW_KEYS.put(45, "MINUS");
        GLFW_KEYS.put(46, "PERIOD");
        GLFW_KEYS.put(47, "SLASH");
        GLFW_KEYS.put(59, "SEMICOLON");
        GLFW_KEYS.put(61, "EQUAL");
        GLFW_KEYS.put(91, "LEFT_BRACKET");
        GLFW_KEYS.put(92, "BACKSLASH");
        GLFW_KEYS.put(93, "RIGHT_BRACKET");
        GLFW_KEYS.put(96, "GRAVE_ACCENT");
        GLFW_KEYS.put(161, "WORLD_1");
        GLFW_KEYS.put(162, "WORLD_2");
        GLFW_KEYS.put(256, "ESCAPE");
        GLFW_KEYS.put(257, "ENTER");
        GLFW_KEYS.put(258, "TAB");
        GLFW_KEYS.put(259, "BACKSPACE");
        GLFW_KEYS.put(260, "INSERT");
        GLFW_KEYS.put(261, "DELETE");
        GLFW_KEYS.put(262, "RIGHT");
        GLFW_KEYS.put(263, "LEFT");
        GLFW_KEYS.put(264, "DOWN");
        GLFW_KEYS.put(265, "UP");
        GLFW_KEYS.put(266, "PAGE_UP");
        GLFW_KEYS.put(267, "PAGE_DOWN");
        GLFW_KEYS.put(268, "HOME");
        GLFW_KEYS.put(269, "END");
        GLFW_KEYS.put(280, "CAPS_LOCK");
        GLFW_KEYS.put(281, "SCROLL_LOCK");
        GLFW_KEYS.put(282, "NUM_LOCK");
        GLFW_KEYS.put(283, "PRINT_SCREEN");
        GLFW_KEYS.put(284, "PAUSE");
        GLFW_KEYS.put(330, "KP_DECIMAL");
        GLFW_KEYS.put(331, "KP_DIVIDE");
        GLFW_KEYS.put(332, "KP_MULTIPLY");
        GLFW_KEYS.put(333, "KP_SUBTRACT");
        GLFW_KEYS.put(334, "KP_ADD");
        GLFW_KEYS.put(335, "KP_ENTER");
        GLFW_KEYS.put(336, "KP_EQUAL");
        GLFW_KEYS.put(340, "LEFT_SHIFT");
        GLFW_KEYS.put(341, "LEFT_CONTROL");
        GLFW_KEYS.put(342, "LEFT_ALT");
        GLFW_KEYS.put(343, "LEFT_SUPER");
        GLFW_KEYS.put(344, "RIGHT_SHIFT");
        GLFW_KEYS.put(345, "RIGHT_CONTROL");
        GLFW_KEYS.put(346, "RIGHT_ALT");
        GLFW_KEYS.put(347, "RIGHT_SUPER");
        GLFW_KEYS.put(348, "MENU");
    }

    private KeyNames() {
    }

    public static String fromMouseButton(int button) {
        return switch (button) {
            case 0 -> "MOUSE_LEFT";
            case 1 -> "MOUSE_RIGHT";
            case 2 -> "MOUSE_MIDDLE";
            default -> button >= 3 && button <= 7 ? "MOUSE_" + (button + 1) : "";
        };
    }

    public static String fromGlfwKey(int key) {
        return GLFW_KEYS.getOrDefault(key, "");
    }

    public static Iterable<Integer> glfwKeys() {
        return GLFW_KEYS.keySet();
    }

    public static int escapeKey() {
        return 256;
    }

    private static void putRange(int start, int endInclusive, java.util.function.IntFunction<String> name) {
        for (int i = start; i <= endInclusive; i++) {
            GLFW_KEYS.put(i, name.apply(i));
        }
    }
}
