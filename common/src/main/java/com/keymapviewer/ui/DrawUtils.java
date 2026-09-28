package com.keymapviewer.ui;

import com.keymapviewer.util.Colors;

/** 极简绘制辅助（跨版本，基于 Canvas）。 */
public final class DrawUtils {
    private DrawUtils() {
    }

    public static void panel(Canvas cv, int x, int y, int w, int h, int bg, int border) {
        cv.fill(x, y, w, h, bg);
        cv.outline(x, y, w, h, border);
    }

    public static void button(Canvas cv, int x, int y, int w, int h,
                              String label, boolean hovered, boolean active, boolean enabled) {
        int bg;
        if (!enabled) {
            bg = 0xFF2A2A2E;
        } else if (active) {
            bg = hovered ? 0xFF3D6B3D : 0xFF335A33;
        } else {
            bg = hovered ? 0xFF46464C : 0xFF333338;
        }
        cv.fill(x, y, w, h, bg);
        cv.outline(x, y, w, h, active ? Colors.TEXT_GREEN : 0xFF55555C);
        int color = !enabled ? Colors.TEXT_DIM : (active ? Colors.TEXT_GREEN : Colors.TEXT_WHITE);
        int tw = cv.textWidth(label);
        cv.text(label, x + (w - tw) / 2, y + (h - cv.lineHeight()) / 2, color);
    }

    public static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}