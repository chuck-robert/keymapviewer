package com.keymapviewer.hud;

import com.keymapviewer.config.ModConfig;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingManager;
import com.keymapviewer.ui.Canvas;
import com.keymapviewer.util.Colors;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD 行模型（纯逻辑）：行构建 / 尺寸测量 / 预设位置解析 / 绘制。
 * 各版本模块提供 Canvas 即可复用同一套排版。
 */
public final class HudModel {
    public static final int KIND_HEADER = 0;
    public static final int KIND_CATEGORY = 1;
    public static final int KIND_BINDING = 2;

    private static final int LINE_H = 10;
    private static final int KEY_GAP = 14;

    public record HudLine(int kind, String left, String right, int leftColor, int rightColor, int rightKind) {
    }

    private HudModel() {
    }

    /** String → 像素宽 的函数别名。 */
    public interface TextMeasure {
        int of(String s);
    }

    public static List<HudLine> composeLines(List<KeyBindingInfo> list, ModConfig cfg, String unboundLabel) {
        List<HudLine> lines = new ArrayList<>();
        String prevMod = null;
        String prevCat = null;
        for (KeyBindingInfo info : list) {
            if (prevMod == null || !info.modName.equals(prevMod)) {
                lines.add(new HudLine(KIND_HEADER, "[" + info.modName + "]", null,
                        Colors.TEXT_YELLOW, 0, KIND_HEADER));
                prevMod = info.modName;
                prevCat = null;
            }
            if (cfg.showHudCategories && (prevCat == null || !info.category.equals(prevCat))) {
                lines.add(new HudLine(KIND_CATEGORY, info.category, null,
                        Colors.TEXT_DIM, 0, KIND_CATEGORY));
                prevCat = info.category;
            }
            String keyText = info.isBound() ? info.displayKey : unboundLabel;
            int keyColor = Colors.TEXT_GRAY;
            if (info.held) {
                keyColor = Colors.TEXT_GREEN;
            } else if (info.isConflicted()) {
                keyColor = Colors.TEXT_RED;
            } else if (info.isBound()) {
                keyColor = Colors.TEXT_WHITE;
            }
            int actionColor = info.isBound() ? Colors.TEXT_WHITE : Colors.TEXT_GRAY;
            lines.add(new HudLine(KIND_BINDING, info.actionName, keyText, actionColor, keyColor, KIND_BINDING));
        }
        return lines;
    }

    public record Dim(int width, int height) {
    }

    public static Dim measure(List<HudLine> lines, TextMeasure textWidth) {
        int actionMax = 0;
        int keyMax = 0;
        int headerWidth = 0;
        for (HudLine l : lines) {
            if (l.kind() == KIND_BINDING) {
                actionMax = Math.max(actionMax, textWidth.of(l.left()));
                keyMax = Math.max(keyMax, textWidth.of(l.right()));
            } else {
                headerWidth = Math.max(headerWidth, textWidth.of(l.left()));
            }
        }
        int bindingWidth = actionMax + keyMax + KEY_GAP + 12;
        int panelW = Math.max(bindingWidth, headerWidth) + 10;
        int panelH = lines.size() * LINE_H + 10;
        return new Dim(panelW, panelH);
    }

    /**
     * 解析预设锚点 + 偏移 → HUD 左上角像素坐标。
     * 偏移量强制生效：只做宽松可见性保护（至少保留约 48px 可见），不因贴边而吞掉偏移。
     */
    public static int[] resolvePosition(int gw, int gh, int hudW, int hudH,
                                        ModConfig.HudPreset preset, int offsetX, int offsetY) {
        int w = hudW + 12;
        int h = hudH + 12;
        int bx;
        int by;
        switch (preset) {
            case TOP_RIGHT -> {
                bx = gw - w - 4;
                by = 4;
            }
            case BOTTOM_LEFT -> {
                bx = 4;
                by = gh - h - 4;
            }
            case BOTTOM_RIGHT -> {
                bx = gw - w - 4;
                by = gh - h - 4;
            }
            case TOP_CENTER -> {
                bx = (gw - w) / 2;
                by = 4;
            }
            case MID_LEFT -> {
                bx = 4;
                by = (gh - h) / 2;
            }
            case MID_RIGHT -> {
                bx = gw - w - 4;
                by = (gh - h) / 2;
            }
            case HOTBAR_LEFT -> {
                bx = gw / 2 - 99 - w;
                by = gh - 30 - h;
            }
            case HOTBAR_RIGHT -> {
                bx = gw / 2 + 99;
                by = gh - 30 - h;
            }
            default -> {
                bx = 4;
                by = 4;
            }
        }
        int vis = 48;
        int x = Math.max(vis - w, Math.min(gw - vis, bx + offsetX));
        int y = Math.max(vis - h, Math.min(gh - vis, by + offsetY));
        return new int[]{x, y};
    }

    public static boolean isRightSide(ModConfig.HudPreset p) {
        return p == ModConfig.HudPreset.TOP_RIGHT || p == ModConfig.HudPreset.BOTTOM_RIGHT
                || p == ModConfig.HudPreset.MID_RIGHT || p == ModConfig.HudPreset.HOTBAR_RIGHT;
    }

    private static boolean hudAutotestMarked;

    /** 用 Canvas 绘制行模型。 */
    public static void drawLines(Canvas canvas, List<HudLine> lines, ModConfig cfg) {
        if (System.getenv("KVM_AUTOTEST") != null && !hudAutotestMarked) {
            hudAutotestMarked = true;
            com.keymapviewer.config.KeyViewerLog.LOG.info("[keymapviewer] AUTOTEST hud-ok");
        }
        float textOp = cfg.hudTextOpacity;
        float bgOp = cfg.hudBgOpacity;
        boolean right = isRightSide(cfg.hudPreset);

        Dim dim = measure(lines, canvas::textWidth);
        int panelW = dim.width;
        int panelH = dim.height;

        canvas.fill(0, 0, panelW, panelH, Colors.withAlpha(Colors.HUD_BG_BLACK, bgOp));
        canvas.outline(0, 0, panelW, panelH, Colors.withAlpha(0xFF3A3A40, bgOp));
        canvas.beginStratum();

        int actionMax = 0;
        int keyMax = 0;
        for (HudLine l : lines) {
            if (l.kind() == KIND_BINDING) {
                actionMax = Math.max(actionMax, canvas.textWidth(l.left()));
                keyMax = Math.max(keyMax, canvas.textWidth(l.right()));
            }
        }
        int pad = 4;
        int textLeft = pad + 4;
        int panelRight = panelW - pad - 4;
        int y = 5;

        for (HudLine l : lines) {
            if (l.kind() == KIND_HEADER) {
                if (right) {
                    canvas.text(l.left(), panelRight - canvas.textWidth(l.left()), y,
                            Colors.withAlpha(Colors.TEXT_YELLOW, textOp));
                } else {
                    canvas.text(l.left(), textLeft, y, Colors.withAlpha(Colors.TEXT_YELLOW, textOp));
                }
            } else if (l.kind() == KIND_CATEGORY) {
                if (right) {
                    canvas.text(l.left(), panelRight - canvas.textWidth(l.left()), y,
                            Colors.withAlpha(Colors.TEXT_DIM, textOp));
                } else {
                    canvas.text(l.left(), textLeft + 6, y, Colors.withAlpha(Colors.TEXT_DIM, textOp));
                }
            } else {
                int actionColor = Colors.withAlpha(l.leftColor(), textOp);
                int keyColor = Colors.withAlpha(l.rightColor(), textOp);
                if (right) {
                    int keyX = panelRight - canvas.textWidth(l.left()) - KEY_GAP - canvas.textWidth(l.right());
                    canvas.text(l.right(), keyX, y, keyColor);
                    canvas.text(l.left(), panelRight - canvas.textWidth(l.left()), y, actionColor);
                } else {
                    canvas.text(l.left(), textLeft, y, actionColor);
                    canvas.text(l.right(), textLeft + actionMax + KEY_GAP, y, keyColor);
                }
            }
            y += LINE_H;
        }
    }
}