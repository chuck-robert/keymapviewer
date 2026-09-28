package com.keymapviewer.ui;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物理键盘布局（QWERTY）：GLFW 键码 → 网格坐标。
 * 主键区（行0-5）+ 导航键与小键盘（行7-11，中间留一行间隔）。
 */
public final class KeyboardLayout {

    public record KeyPos(int code, int row, float col, float width) {
    }

    private static final Map<Integer, KeyPos> POS = new HashMap<>();
    private static final List<KeyPos> ALL = new ArrayList<>();
    private static float maxCol = 0f;

    static {
        // 行0：功能键行
        put(GLFW.GLFW_KEY_ESCAPE, 0, 0f, 1f);
        put(GLFW.GLFW_KEY_F1, 0, 2f, 1f);
        put(GLFW.GLFW_KEY_F2, 0, 3f, 1f);
        put(GLFW.GLFW_KEY_F3, 0, 4f, 1f);
        put(GLFW.GLFW_KEY_F4, 0, 5f, 1f);
        put(GLFW.GLFW_KEY_F5, 0, 6.5f, 1f);
        put(GLFW.GLFW_KEY_F6, 0, 7.5f, 1f);
        put(GLFW.GLFW_KEY_F7, 0, 8.5f, 1f);
        put(GLFW.GLFW_KEY_F8, 0, 9.5f, 1f);
        put(GLFW.GLFW_KEY_F9, 0, 11f, 1f);
        put(GLFW.GLFW_KEY_F10, 0, 12f, 1f);
        put(GLFW.GLFW_KEY_F11, 0, 13f, 1f);
        put(GLFW.GLFW_KEY_F12, 0, 14f, 1f);
        put(GLFW.GLFW_KEY_PRINT_SCREEN, 0, 15.5f, 1f);
        put(GLFW.GLFW_KEY_SCROLL_LOCK, 0, 16.5f, 1f);
        put(GLFW.GLFW_KEY_PAUSE, 0, 17.5f, 1f);

        // 行1：数字行
        put(GLFW.GLFW_KEY_GRAVE_ACCENT, 1, 0f, 1f);
        put(GLFW.GLFW_KEY_1, 1, 1f, 1f);
        put(GLFW.GLFW_KEY_2, 1, 2f, 1f);
        put(GLFW.GLFW_KEY_3, 1, 3f, 1f);
        put(GLFW.GLFW_KEY_4, 1, 4f, 1f);
        put(GLFW.GLFW_KEY_5, 1, 5f, 1f);
        put(GLFW.GLFW_KEY_6, 1, 6f, 1f);
        put(GLFW.GLFW_KEY_7, 1, 7f, 1f);
        put(GLFW.GLFW_KEY_8, 1, 8f, 1f);
        put(GLFW.GLFW_KEY_9, 1, 9f, 1f);
        put(GLFW.GLFW_KEY_0, 1, 10f, 1f);
        put(GLFW.GLFW_KEY_MINUS, 1, 11f, 1f);
        put(GLFW.GLFW_KEY_EQUAL, 1, 12f, 1f);
        put(GLFW.GLFW_KEY_BACKSPACE, 1, 13f, 2f);

        // 行2：QWERTY 行
        put(GLFW.GLFW_KEY_TAB, 2, 0f, 1.5f);
        put(GLFW.GLFW_KEY_Q, 2, 1.75f, 1f);
        put(GLFW.GLFW_KEY_W, 2, 2.75f, 1f);
        put(GLFW.GLFW_KEY_E, 2, 3.75f, 1f);
        put(GLFW.GLFW_KEY_R, 2, 4.75f, 1f);
        put(GLFW.GLFW_KEY_T, 2, 5.75f, 1f);
        put(GLFW.GLFW_KEY_Y, 2, 6.75f, 1f);
        put(GLFW.GLFW_KEY_U, 2, 7.75f, 1f);
        put(GLFW.GLFW_KEY_I, 2, 8.75f, 1f);
        put(GLFW.GLFW_KEY_O, 2, 9.75f, 1f);
        put(GLFW.GLFW_KEY_P, 2, 10.75f, 1f);
        put(GLFW.GLFW_KEY_LEFT_BRACKET, 2, 11.75f, 1f);
        put(GLFW.GLFW_KEY_RIGHT_BRACKET, 2, 12.75f, 1f);
        put(GLFW.GLFW_KEY_BACKSLASH, 2, 13.75f, 1.5f);

        // 行3：ASDF 行
        put(GLFW.GLFW_KEY_CAPS_LOCK, 3, 0f, 1.75f);
        put(GLFW.GLFW_KEY_A, 3, 2f, 1f);
        put(GLFW.GLFW_KEY_S, 3, 3f, 1f);
        put(GLFW.GLFW_KEY_D, 3, 4f, 1f);
        put(GLFW.GLFW_KEY_F, 3, 5f, 1f);
        put(GLFW.GLFW_KEY_G, 3, 6f, 1f);
        put(GLFW.GLFW_KEY_H, 3, 7f, 1f);
        put(GLFW.GLFW_KEY_J, 3, 8f, 1f);
        put(GLFW.GLFW_KEY_K, 3, 9f, 1f);
        put(GLFW.GLFW_KEY_L, 3, 10f, 1f);
        put(GLFW.GLFW_KEY_SEMICOLON, 3, 11f, 1f);
        put(GLFW.GLFW_KEY_APOSTROPHE, 3, 12f, 1f);
        put(GLFW.GLFW_KEY_ENTER, 3, 13f, 2.25f);

        // 行4：ZXCV 行
        put(GLFW.GLFW_KEY_LEFT_SHIFT, 4, 0f, 2.25f);
        put(GLFW.GLFW_KEY_Z, 4, 2.5f, 1f);
        put(GLFW.GLFW_KEY_X, 4, 3.5f, 1f);
        put(GLFW.GLFW_KEY_C, 4, 4.5f, 1f);
        put(GLFW.GLFW_KEY_V, 4, 5.5f, 1f);
        put(GLFW.GLFW_KEY_B, 4, 6.5f, 1f);
        put(GLFW.GLFW_KEY_N, 4, 7.5f, 1f);
        put(GLFW.GLFW_KEY_M, 4, 8.5f, 1f);
        put(GLFW.GLFW_KEY_COMMA, 4, 9.5f, 1f);
        put(GLFW.GLFW_KEY_PERIOD, 4, 10.5f, 1f);
        put(GLFW.GLFW_KEY_SLASH, 4, 11.5f, 1f);
        put(GLFW.GLFW_KEY_RIGHT_SHIFT, 4, 12.5f, 2.75f);

        // 行5：底部修饰键
        put(GLFW.GLFW_KEY_LEFT_CONTROL, 5, 0f, 1.5f);
        put(GLFW.GLFW_KEY_LEFT_SUPER, 5, 1.5f, 1.25f);
        put(GLFW.GLFW_KEY_LEFT_ALT, 5, 2.75f, 1.25f);
        put(GLFW.GLFW_KEY_SPACE, 5, 4f, 6.25f);
        put(GLFW.GLFW_KEY_RIGHT_ALT, 5, 10.25f, 1.25f);
        put(GLFW.GLFW_KEY_RIGHT_SUPER, 5, 11.5f, 1.25f);
        put(GLFW.GLFW_KEY_MENU, 5, 12.75f, 1.25f);
        put(GLFW.GLFW_KEY_RIGHT_CONTROL, 5, 14f, 1.5f);

        // 行6：鼠标键（紧贴主键盘下方）。编码用规范鼠标码 -100..-93：
        // 左键=-100、右键=-99、中键=-98、侧键依次递减，与 MaLiLib MOUSE_BUTTON_n 一致
        put(-100, 6, 0f, 1f);
        put(-99, 6, 1f, 1f);
        put(-98, 6, 2f, 1f);
        put(-97, 6, 3f, 1f);
        put(-96, 6, 4f, 1f);

        // 行7：导航行1 + 小键盘行1（行6已被鼠标排占用，导航区与鼠标排之间无间隔行）
        put(GLFW.GLFW_KEY_INSERT, 7, 0f, 1f);
        put(GLFW.GLFW_KEY_HOME, 7, 1f, 1f);
        put(GLFW.GLFW_KEY_PAGE_UP, 7, 2f, 1f);
        put(GLFW.GLFW_KEY_NUM_LOCK, 7, 6f, 1f);
        put(GLFW.GLFW_KEY_KP_DIVIDE, 7, 7f, 1f);
        put(GLFW.GLFW_KEY_KP_MULTIPLY, 7, 8f, 1f);
        put(GLFW.GLFW_KEY_KP_SUBTRACT, 7, 9f, 1f);

        // 行8：导航行2 + 小键盘 7-9+
        put(GLFW.GLFW_KEY_DELETE, 8, 0f, 1f);
        put(GLFW.GLFW_KEY_END, 8, 1f, 1f);
        put(GLFW.GLFW_KEY_PAGE_DOWN, 8, 2f, 1f);
        put(GLFW.GLFW_KEY_KP_7, 8, 6f, 1f);
        put(GLFW.GLFW_KEY_KP_8, 8, 7f, 1f);
        put(GLFW.GLFW_KEY_KP_9, 8, 8f, 1f);
        put(GLFW.GLFW_KEY_KP_ADD, 8, 9f, 1f);

        // 行9：方向键上 + 小键盘 4-6
        put(GLFW.GLFW_KEY_UP, 9, 1f, 1f);
        put(GLFW.GLFW_KEY_KP_4, 9, 6f, 1f);
        put(GLFW.GLFW_KEY_KP_5, 9, 7f, 1f);
        put(GLFW.GLFW_KEY_KP_6, 9, 8f, 1f);

        // 行10：方向键 左/下/右 + 小键盘 1-3 Enter
        put(GLFW.GLFW_KEY_LEFT, 10, 0f, 1f);
        put(GLFW.GLFW_KEY_DOWN, 10, 1f, 1f);
        put(GLFW.GLFW_KEY_RIGHT, 10, 2f, 1f);
        put(GLFW.GLFW_KEY_KP_1, 10, 6f, 1f);
        put(GLFW.GLFW_KEY_KP_2, 10, 7f, 1f);
        put(GLFW.GLFW_KEY_KP_3, 10, 8f, 1f);
        put(GLFW.GLFW_KEY_KP_ENTER, 10, 9f, 1f);

        // 行11：小键盘 0（宽） + 小数点
        put(GLFW.GLFW_KEY_KP_0, 11, 6f, 2f);
        put(GLFW.GLFW_KEY_KP_DECIMAL, 11, 8f, 1f);

        ALL.sort((a, b) -> a.row() != b.row() ? Integer.compare(a.row(), b.row())
                : Float.compare(a.col(), b.col()));
    }

    private static void put(int code, int row, float col, float width) {
        KeyPos pos = new KeyPos(code, row, col, width);
        POS.put(code, pos);
        ALL.add(pos);
        if (col + width > maxCol) {
            maxCol = col + width;
        }
    }

    public static KeyPos position(int code) {
        return POS.get(code);
    }

    public static List<KeyPos> allKeys() {
        return ALL;
    }

    public static float widthUnits() {
        return maxCol + 1f;
    }

    public static int rowCount() {
        int max = 0;
        for (KeyPos k : ALL) {
            max = Math.max(max, k.row());
        }
        return max + 1;
    }
}