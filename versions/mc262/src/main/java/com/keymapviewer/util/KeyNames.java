package com.keymapviewer.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * GLFW 键码 → 键盘视图键帽短标签。
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
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT) {
            return "Shift";
        }
        if (code == GLFW.GLFW_KEY_RIGHT_SHIFT) {
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
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F12) {
            return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        }
        if (code >= GLFW.GLFW_KEY_F13 && code <= GLFW.GLFW_KEY_F24) {
            return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        }
        // 标点符号
        if (code < 128) {
            return String.valueOf((char) code);
        }
        // 其它：用 Minecraft 本地化键名
        try {
            String mc = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
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
        if (code >= GLFW.GLFW_KEY_KP_0 && code <= GLFW.GLFW_KEY_KP_9) {
            return String.valueOf(code - GLFW.GLFW_KEY_KP_0);
        }
        if (code == GLFW.GLFW_KEY_SPACE) {
            try {
                String mc = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
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
            String mc = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
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
            case GLFW.GLFW_KEY_LEFT_CONTROL:
                return "left.ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL:
                return "right.ctrl";
            case GLFW.GLFW_KEY_LEFT_SHIFT:
                return "left.shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                return "right.shift";
            case GLFW.GLFW_KEY_LEFT_ALT:
                return "left.alt";
            case GLFW.GLFW_KEY_RIGHT_ALT:
                return "right.alt";
            case GLFW.GLFW_KEY_LEFT_SUPER:
                return "left.win";
            case GLFW.GLFW_KEY_RIGHT_SUPER:
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