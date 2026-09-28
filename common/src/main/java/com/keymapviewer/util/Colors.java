package com.keymapviewer.util;

/** ARGB 颜色常量与合成工具（Minecraft 绘制接口使用 ARGB 顺序的 int）。 */
public final class Colors {
    public static final int TRANSPARENT = 0x00000000;

    // 文本
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    public static final int TEXT_GRAY = 0xFF9A9A9A;
    public static final int TEXT_DIM = 0xFF6B6B6B;
    public static final int TEXT_RED = 0xFFFF5656;
    public static final int TEXT_GREEN = 0xFF59D964;
    public static final int TEXT_YELLOW = 0xFFFFD454;
    public static final int TEXT_BLUE = 0xFF5AA9FF;

    // 色板 —— 键位状态
    public static final int KEY_UNBOUND_BG = 0xFF242424;
    public static final int KEY_UNBOUND_TEXT = 0xFF7E7E7E;
    public static final int KEY_BOUND_BG = 0xFF3C3F46;
    public static final int KEY_BOUND_TEXT = 0xFFFFFFFF;
    public static final int KEY_CONFLICT_BG = 0xFF53303A;
    public static final int KEY_CONFLICT_TEXT = 0xFFFF9999;
    public static final int KEY_HELD_BG = 0xFF3D5A3D;
    public static final int KEY_HELD_TEXT = 0xFFC2FFC2;

    // HUD 面板
    public static final int HUD_BG_BLACK = 0xFF0A0A0C;

    private Colors() {
    }

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (clamp01(alpha) * 255.0f);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int withAlphaPercent(int argb, int alphaPercent) {
        int a = Math.max(0, Math.min(100, alphaPercent)) * 255 / 100;
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}