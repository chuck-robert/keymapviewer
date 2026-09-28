package com.keymapviewer.util;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** GLFW 键码 → 键帽短标签（纯逻辑，无 MC 依赖）。 */
public final class KeyLabels {
    private static final Map<Integer, String> CACHE = new HashMap<>();

    private KeyLabels() {
    }

    public static String label(int code) {
        String cached = CACHE.get(code);
        if (cached != null) {
            return cached;
        }
        String label = computeLabel(code);
        CACHE.put(code, label);
        return label;
    }

    private static String computeLabel(int code) {
        if (code >= 'A' && code <= 'Z') {
            return String.valueOf((char) code);
        }
        if (code >= '0' && code <= '9') {
            return String.valueOf((char) code);
        }
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) {
            return String.valueOf(code - GLFW.GLFW_KEY_KP_0);
        }
        if (code == GLFW.GLFW_KEY_SPACE) {
            return "Space";
        }
        if (code == GLFW.GLFW_KEY_ENTER) {
            return "Enter";
        }
        if (code == GLFW.GLFW_KEY_TAB) {
            return "Tab";
        }
        if (code == GLFW.GLFW_KEY_BACKSPACE) {
            return "Back";
        }
        if (code == GLFW.GLFW_KEY_ESCAPE) {
            return "Esc";
        }
        if (code == GLFW.GLFW_KEY_CAPS_LOCK) {
            return "Caps";
        }
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return "Shift";
        }
        if (code == GLFW.GLFW_KEY_LEFT_CONTROL || code == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            return "Ctrl";
        }
        if (code == GLFW.GLFW_KEY_LEFT_ALT || code == GLFW.GLFW_KEY_RIGHT_ALT) {
            return "Alt";
        }
        if (code == GLFW.GLFW_KEY_LEFT_SUPER || code == GLFW.GLFW_KEY_RIGHT_SUPER) {
            return "Win";
        }
        if (code == GLFW.GLFW_KEY_MENU) {
            return "Menu";
        }
        if (code == GLFW.GLFW_KEY_INSERT) {
            return "Ins";
        }
        if (code == GLFW.GLFW_KEY_DELETE) {
            return "Del";
        }
        if (code == GLFW.GLFW_KEY_PRINT_SCREEN) {
            return "PrtSc";
        }
        if (code == GLFW.GLFW_KEY_SCROLL_LOCK) {
            return "ScrLk";
        }
        if (code == GLFW.GLFW_KEY_PAUSE) {
            return "Pause";
        }
        if (code == GLFW.GLFW_KEY_PAGE_UP) {
            return "PgUp";
        }
        if (code == GLFW.GLFW_KEY_PAGE_DOWN) {
            return "PgDn";
        }
        if (code == GLFW.GLFW_KEY_HOME) {
            return "Home";
        }
        if (code == GLFW.GLFW_KEY_END) {
            return "End";
        }
        if (code == GLFW.GLFW_KEY_UP) {
            return "Up";
        }
        if (code == GLFW.GLFW_KEY_DOWN) {
            return "Down";
        }
        if (code == GLFW.GLFW_KEY_LEFT) {
            return "Left";
        }
        if (code == GLFW.GLFW_KEY_RIGHT) {
            return "Right";
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F24) {
            return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        }
        if (code >= 32 && code < 128) {
            return String.valueOf((char) code);
        }
        return "#" + code;
    }

    /** 极短标签（键帽过小时使用）。 */
    public static String abbrev(int code) {
        if (code >= 'A' && code <= 'Z') {
            return String.valueOf((char) code);
        }
        if (code >= '0' && code <= '9') {
            return String.valueOf((char) code);
        }
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) {
            return String.valueOf(code - GLFW.GLFW_KEY_KP_0);
        }
        switch (code) {
            case GLFW.GLFW_KEY_SPACE:
                return "S";
            case GLFW.GLFW_KEY_ENTER:
                return "En";
            case GLFW.GLFW_KEY_TAB:
                return "Tb";
            case GLFW.GLFW_KEY_BACKSPACE:
                return "Bks";
            case GLFW.GLFW_KEY_ESCAPE:
                return "Esc";
            case GLFW.GLFW_KEY_CAPS_LOCK:
                return "Cap";
            case GLFW.GLFW_KEY_LEFT_SHIFT:
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                return "Sh";
            case GLFW.GLFW_KEY_LEFT_CONTROL:
            case GLFW.GLFW_KEY_RIGHT_CONTROL:
                return "Ct";
            case GLFW.GLFW_KEY_LEFT_ALT:
            case GLFW.GLFW_KEY_RIGHT_ALT:
                return "Al";
            case GLFW.GLFW_KEY_LEFT_SUPER:
            case GLFW.GLFW_KEY_RIGHT_SUPER:
                return "Win";
            case GLFW.GLFW_KEY_MENU:
                return "Mn";
            case GLFW.GLFW_KEY_PRINT_SCREEN:
                return "Ps";
            case GLFW.GLFW_KEY_SCROLL_LOCK:
                return "SL";
            case GLFW.GLFW_KEY_PAUSE:
                return "Pau";
            case GLFW.GLFW_KEY_PAGE_UP:
                return "PU";
            case GLFW.GLFW_KEY_PAGE_DOWN:
                return "PD";
            case GLFW.GLFW_KEY_INSERT:
                return "Ins";
            case GLFW.GLFW_KEY_DELETE:
                return "Del";
            case GLFW.GLFW_KEY_HOME:
                return "Hm";
            case GLFW.GLFW_KEY_END:
                return "End";
            case GLFW.GLFW_KEY_NUM_LOCK:
                return "NL";
            case GLFW.GLFW_KEY_KP_DIVIDE:
                return "/";
            case GLFW.GLFW_KEY_KP_MULTIPLY:
                return "*";
            case GLFW.GLFW_KEY_KP_SUBTRACT:
                return "-";
            case GLFW.GLFW_KEY_KP_ADD:
                return "+";
            case GLFW.GLFW_KEY_KP_ENTER:
                return "En";
            case GLFW.GLFW_KEY_KP_DECIMAL:
                return ".";
            case GLFW.GLFW_KEY_UP:
                return "Up";
            case GLFW.GLFW_KEY_DOWN:
                return "Dn";
            case GLFW.GLFW_KEY_LEFT:
                return "Lt";
            case GLFW.GLFW_KEY_RIGHT:
                return "Rt";
            default:
                return label(code);
        }
    }
}