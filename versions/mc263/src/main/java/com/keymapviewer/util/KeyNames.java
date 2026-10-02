package com.keymapviewer.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import com.keymapviewer.input.SdlKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * GLFW 规范键码 → 键盘视图键帽短标签（26.3 起 GLFW 常量由 SdlKeys 自带）。
 * 字母/数字/标点直接使用 QWERTY 字符，特殊键使用简短英文，
 * 其余交给 Minecraft 的本地化键名（key.keyboard.*）。
 */
public final class KeyNames {
    private static final Map<Integer, String> CACHE = new HashMap<>();

    private KeyNames() {
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
        // 字母区
        if (code >= 'A' && code <= 'Z') {
            return String.valueOf((char) code);
        }
        // 数字行
        if (code >= '0' && code <= '9') {
            return String.valueOf((char) code);
        }
        // 小键盘数字
        if (code >= SdlKeys.GLFW_KEY_KP_0 && code <= SdlKeys.GLFW_KEY_KP_9) {
            return String.valueOf(code - SdlKeys.GLFW_KEY_KP_0);
        }
        if (code == SdlKeys.GLFW_KEY_SPACE) {
            return "Space";
        }
        if (code == SdlKeys.GLFW_KEY_ENTER) {
            return "Enter";
        }
        if (code == SdlKeys.GLFW_KEY_TAB) {
            return "Tab";
        }
        if (code == SdlKeys.GLFW_KEY_BACKSPACE) {
            return "Back";
        }
        if (code == SdlKeys.GLFW_KEY_ESCAPE) {
            return "Esc";
        }
        if (code == SdlKeys.GLFW_KEY_CAPS_LOCK) {
            return "Caps";
        }
        if (code == SdlKeys.GLFW_KEY_LEFT_SHIFT) {
            return "Shift";
        }
        if (code == SdlKeys.GLFW_KEY_RIGHT_SHIFT) {
            return "Shift";
        }
        if (code == SdlKeys.GLFW_KEY_LEFT_CONTROL || code == SdlKeys.GLFW_KEY_RIGHT_CONTROL) {
            return "Ctrl";
        }
        if (code == SdlKeys.GLFW_KEY_LEFT_ALT || code == SdlKeys.GLFW_KEY_RIGHT_ALT) {
            return "Alt";
        }
        if (code == SdlKeys.GLFW_KEY_LEFT_SUPER || code == SdlKeys.GLFW_KEY_RIGHT_SUPER) {
            return "Win";
        }
        if (code == SdlKeys.GLFW_KEY_MENU) {
            return "Menu";
        }
        if (code == SdlKeys.GLFW_KEY_INSERT) {
            return "Ins";
        }
        if (code == SdlKeys.GLFW_KEY_DELETE) {
            return "Del";
        }
        if (code == SdlKeys.GLFW_KEY_PRINT_SCREEN) {
            return "PrtSc";
        }
        if (code == SdlKeys.GLFW_KEY_SCROLL_LOCK) {
            return "ScrLk";
        }
        if (code == SdlKeys.GLFW_KEY_PAUSE) {
            return "Pause";
        }
        if (code == SdlKeys.GLFW_KEY_PAGE_UP) {
            return "PgUp";
        }
        if (code == SdlKeys.GLFW_KEY_PAGE_DOWN) {
            return "PgDn";
        }
        if (code == SdlKeys.GLFW_KEY_HOME) {
            return "Home";
        }
        if (code == SdlKeys.GLFW_KEY_END) {
            return "End";
        }
        if (code == SdlKeys.GLFW_KEY_UP) {
            return "Up";
        }
        if (code == SdlKeys.GLFW_KEY_DOWN) {
            return "Down";
        }
        if (code == SdlKeys.GLFW_KEY_LEFT) {
            return "Left";
        }
        if (code == SdlKeys.GLFW_KEY_RIGHT) {
            return "Right";
        }
        if (code >= SdlKeys.GLFW_KEY_F1 && code <= SdlKeys.GLFW_KEY_F12) {
            return "F" + (code - SdlKeys.GLFW_KEY_F1 + 1);
        }
        if (code >= SdlKeys.GLFW_KEY_F13 && code <= SdlKeys.GLFW_KEY_F24) {
            return "F" + (code - SdlKeys.GLFW_KEY_F1 + 1);
        }
        // 标点符号
        if (code < 128) {
            return String.valueOf((char) code);
        }
        // 其它：用 Minecraft 本地化键名
        try {
            String mc = InputConstants.Type.KEYBOARD.getOrCreate(SdlKeys.glfwToScancode(code)).getDisplayName().getString();
            if (mc != null && !mc.isEmpty() && !mc.startsWith("key.keyboard.")) {
                return mc;
            }
        } catch (Throwable t) {
            // ignore
        }
        return "#" + code;
    }

    /**
     * 面向 HUD / 列表 / 捕捉草稿的本地化显示名：
     * 字母/数字/符号键直接显示字面字符（`\` 就是 `\`、分号就是 `;`）；
     * 修饰键与特殊键走 Minecraft 自带语言包（key.keyboard.*），中英文自动适配
     * （例如 left.alt → “左Alt” / “Left Alt”）。
     */
    public static String pretty(int code) {
        // 鼠标键（规范编码 -100..-93，对齐 MaLiLib MOUSE_BUTTON_n）
        if (code <= -93 && code >= -100) {
            String ours = langOrNull("keymapviewer.mouse." + (code + 101));
            return ours != null ? ours : "Btn" + (code + 101);
        }
        if (code >= 'A' && code <= 'Z') {
            return String.valueOf((char) code);
        }
        if (code >= '0' && code <= '9') {
            return String.valueOf((char) code);
        }
        if (code >= SdlKeys.GLFW_KEY_KP_0 && code <= SdlKeys.GLFW_KEY_KP_9) {
            return String.valueOf(code - SdlKeys.GLFW_KEY_KP_0);
        }
        if (code == SdlKeys.GLFW_KEY_SPACE) {
            try {
                String mc = InputConstants.Type.KEYBOARD.getOrCreate(SdlKeys.glfwToScancode(code)).getDisplayName().getString();
                if (mc != null && !mc.isEmpty() && !mc.equals("Unknown")) {
                    return mc;
                }
            } catch (Throwable t) {
                // ignore
            }
            return "Space";
        }
        if (code > 32 && code < 128) {
            return String.valueOf((char) code);
        }
        // 修饰键：优先本模组语言文件（keymapviewer.key.left.alt 等，可被资源包覆盖）
        String modKey = modifierLangKey(code);
        if (modKey != null) {
            String ours = langOrNull("keymapviewer.key." + modKey);
            if (ours != null) {
                return ours;
            }
        }
        try {
            String mc = InputConstants.Type.KEYBOARD.getOrCreate(SdlKeys.glfwToScancode(code)).getDisplayName().getString();
            if (mc != null && !mc.isEmpty() && !mc.startsWith("key.keyboard.") && !mc.equals("Unknown")) {
                return mc;
            }
        } catch (Throwable t) {
            // 回落到本地标签
        }
        return label(code);
    }

    /** 修饰键 → 模组语言条目名（keymapviewer.key.<name>），不支持返回 null。 */
    private static String modifierLangKey(int code) {
        switch (code) {
            case SdlKeys.GLFW_KEY_LEFT_CONTROL:
                return "left.ctrl";
            case SdlKeys.GLFW_KEY_RIGHT_CONTROL:
                return "right.ctrl";
            case SdlKeys.GLFW_KEY_LEFT_SHIFT:
                return "left.shift";
            case SdlKeys.GLFW_KEY_RIGHT_SHIFT:
                return "right.shift";
            case SdlKeys.GLFW_KEY_LEFT_ALT:
                return "left.alt";
            case SdlKeys.GLFW_KEY_RIGHT_ALT:
                return "right.alt";
            case SdlKeys.GLFW_KEY_LEFT_SUPER:
                return "left.win";
            case SdlKeys.GLFW_KEY_RIGHT_SUPER:
                return "right.win";
            default:
                return null;
        }
    }

    private static String langOrNull(String key) {
        try {
            String s = net.minecraft.network.chat.Component.translatable(key).getString();
            return s != null && !s.startsWith("keymapviewer.") ? s : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 极短标签（键帽过小时使用）。 */
    public static String abbrev(int code) {
        if (code >= 'A' && code <= 'Z') {
            return String.valueOf((char) code);
        }
        if (code >= '0' && code <= '9') {
            return String.valueOf((char) code);
        }
        if (code >= SdlKeys.GLFW_KEY_KP_0 && code <= SdlKeys.GLFW_KEY_KP_9) {
            return String.valueOf(code - SdlKeys.GLFW_KEY_KP_0);
        }
        switch (code) {
            case SdlKeys.GLFW_KEY_SPACE:
                return "S";
            case SdlKeys.GLFW_KEY_ENTER:
                return "En";
            case SdlKeys.GLFW_KEY_TAB:
                return "Tb";
            case SdlKeys.GLFW_KEY_BACKSPACE:
                return "Bks";
            case SdlKeys.GLFW_KEY_ESCAPE:
                return "Esc";
            case SdlKeys.GLFW_KEY_CAPS_LOCK:
                return "Cap";
            case SdlKeys.GLFW_KEY_LEFT_SHIFT:
            case SdlKeys.GLFW_KEY_RIGHT_SHIFT:
                return "Sh";
            case SdlKeys.GLFW_KEY_LEFT_CONTROL:
            case SdlKeys.GLFW_KEY_RIGHT_CONTROL:
                return "Ct";
            case SdlKeys.GLFW_KEY_LEFT_ALT:
            case SdlKeys.GLFW_KEY_RIGHT_ALT:
                return "Al";
            case SdlKeys.GLFW_KEY_LEFT_SUPER:
            case SdlKeys.GLFW_KEY_RIGHT_SUPER:
                return "Win";
            case SdlKeys.GLFW_KEY_MENU:
                return "Mn";
            case SdlKeys.GLFW_KEY_PRINT_SCREEN:
                return "Ps";
            case SdlKeys.GLFW_KEY_SCROLL_LOCK:
                return "SL";
            case SdlKeys.GLFW_KEY_PAUSE:
                return "Pau";
            case SdlKeys.GLFW_KEY_PAGE_UP:
                return "PU";
            case SdlKeys.GLFW_KEY_PAGE_DOWN:
                return "PD";
            case SdlKeys.GLFW_KEY_INSERT:
                return "Ins";
            case SdlKeys.GLFW_KEY_DELETE:
                return "Del";
            case SdlKeys.GLFW_KEY_HOME:
                return "Hm";
            case SdlKeys.GLFW_KEY_END:
                return "End";
            case SdlKeys.GLFW_KEY_NUM_LOCK:
                return "NL";
            case SdlKeys.GLFW_KEY_KP_DIVIDE:
                return "/";
            case SdlKeys.GLFW_KEY_KP_MULTIPLY:
                return "*";
            case SdlKeys.GLFW_KEY_KP_SUBTRACT:
                return "-";
            case SdlKeys.GLFW_KEY_KP_ADD:
                return "+";
            case SdlKeys.GLFW_KEY_KP_ENTER:
                return "En";
            case SdlKeys.GLFW_KEY_KP_DECIMAL:
                return ".";
            case SdlKeys.GLFW_KEY_UP:
                return "Up";
            case SdlKeys.GLFW_KEY_DOWN:
                return "Dn";
            case SdlKeys.GLFW_KEY_LEFT:
                return "Lt";
            case SdlKeys.GLFW_KEY_RIGHT:
                return "Rt";
            default:
                return label(code);
        }
    }
}