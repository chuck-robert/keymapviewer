package com.keymapviewer.ui;

import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingManager;
import com.keymapviewer.util.Colors;
import com.keymapviewer.util.KeyLabels;
import com.keymapviewer.util.Ui;

import java.util.List;

/**
 * 键盘视图（纯预览）：模拟物理键盘布局，仅展示键位状态（未绑定/单绑定/冲突/按住），
 * 悬停键帽弹出提示框（快捷键 / 模组 / 操作），底部颜色图例。
 * 纯逻辑 + Canvas 绘制，无 MC 依赖。
 */
public final class KeyboardViewPanel {
    private int panelX, panelY, panelW, panelH;
    private int hoverKey = -1;
    private int tooltipScroll;
    private int lastTooltipCode = -1;
    private int tooltipVisibleRows = 0;

    public KeyboardViewPanel() {
    }

    public void setArea(int x, int y, int w, int h) {
        this.panelX = x;
        this.panelY = y;
        this.panelW = w;
        this.panelH = h;
    }

    // ---------------------------------------------------------------
    // 布局换算
    // ---------------------------------------------------------------

    private static final float KEY_W = 26f;
    private static final float KEY_H = 26f;
    private static final float GAP = 4f;

    private float fitScale() {
        float layoutW = KeyboardLayout.widthUnits() * (KEY_W + GAP);
        float layoutH = KeyboardLayout.rowCount() * (KEY_H + GAP);
        float sx = (panelW - 20f) / layoutW;
        float sy = (panelH - 20f) / layoutH;
        return Math.min(1.0f, Math.min(sx, sy));
    }

    private float scale() {
        return fitScale();
    }

    private int originX() {
        return panelX + 10 + (int) ((panelW - 20 - KeyboardLayout.widthUnits() * (KEY_W + GAP) * scale()) / 2);
    }

    private int originY() {
        return panelY + 10 + (int) ((panelH - 20 - KeyboardLayout.rowCount() * (KEY_H + GAP) * scale()) / 2);
    }

    private int keyX(KeyboardLayout.KeyPos pos) {
        return (int) (originX() + pos.col() * (KEY_W + GAP) * scale());
    }

    private int keyY(KeyboardLayout.KeyPos pos) {
        return (int) (originY() + pos.row() * (KEY_H + GAP) * scale());
    }

    private int keyW(KeyboardLayout.KeyPos pos) {
        float s = scale();
        return (int) (pos.width() * KEY_W * s + (pos.width() - 1) * GAP * s);
    }

    private int keyH() {
        return (int) (KEY_H * scale());
    }

    // ---------------------------------------------------------------
    // 绘制
    // ---------------------------------------------------------------

    public void draw(Canvas cv, int mouseX, int mouseY) {
        hoverKey = -1;
        for (KeyboardLayout.KeyPos pos : KeyboardLayout.allKeys()) {
            int x = keyX(pos);
            int y = keyY(pos);
            int w = keyW(pos);
            int h = keyH();
            int code = pos.code();
            int usage = KeyBindingManager.INSTANCE.usageCount(code);
            int bg = Colors.KEY_BOUND_BG;
            int fg = Colors.KEY_BOUND_TEXT;
            int border = 0xFF4A4A50;
            if (usage == 0) {
                bg = Colors.KEY_UNBOUND_BG;
                fg = Colors.KEY_UNBOUND_TEXT;
            } else if (usage > 1) {
                bg = Colors.KEY_CONFLICT_BG;
                fg = Colors.KEY_CONFLICT_TEXT;
                border = 0xFF8A3A3A;
            }
            if (KeyBindingManager.INSTANCE.isKeyDown(code)) {
                bg = Colors.KEY_HELD_BG;
                fg = Colors.KEY_HELD_TEXT;
            }
            cv.fill(x, y, w, h, bg);
            cv.outline(x, y, w, h, border);
            drawKeyLabel(cv, code < 0 ? mouseLabel(code) : labelFor(cv, code, w), x, y, w, h, fg);

            if (DrawUtils.inside(mouseX, mouseY, x, y, w, h)) {
                hoverKey = code;
            }
        }

        // 提示框/图例绘制在更高层（strata），避免被先画的键帽文字盖住
        cv.beginStratum();
        drawLegend(cv);

        if (hoverKey >= 0) {
            if (hoverKey != lastTooltipCode) {
                lastTooltipCode = hoverKey;
                tooltipScroll = 0;
            }
            List<KeyBindingInfo> list = KeyBindingManager.INSTANCE.bindingsForKey(hoverKey);
            drawKeyTooltip(cv, mouseX, mouseY, hoverKey, list);
        }

        if (fitScale() < 0.5f) {
            cv.text(Ui.loc.t("keymapviewer.keyboard.narrow"), panelX + 8, panelY + 2, Colors.TEXT_GRAY);
        }
    }

    private void drawLegend(Canvas cv) {
        int y = panelY + panelH - 16;
        if (y < panelY + 20) {
            return;
        }
        String joined = String.join("   ",
                Ui.loc.t("keymapviewer.keyboard.legend.bound"),
                Ui.loc.t("keymapviewer.keyboard.legend.unbound"),
                Ui.loc.t("keymapviewer.keyboard.legend.conflict"),
                Ui.loc.t("keymapviewer.keyboard.legend.held"));
        cv.fill(panelX, y - 2, cv.textWidth(joined) + 16, 12, 0xA0102020);
        cv.text(joined, panelX + 8, y, Colors.TEXT_DIM);
    }

    private void drawKeyTooltip(Canvas cv, int mouseX, int mouseY, int code, List<KeyBindingInfo> list) {
        int rowH = 12;
        int pad = 6;
        String unbound = Ui.loc.t("keymapviewer.unbound");
        // 组合键（如 F3+G）排在最前，避免被大量单键项淹没
        List<KeyBindingInfo> entries = new java.util.ArrayList<>(list);
        entries.sort((a, b) -> Integer.compare(comboRank(a), comboRank(b)));
        // 悬停副键（组合 F3+G 的 G）时标题给出完整组合，避免只见 G 不见 F3
        String keyName = KeyCapture.NAMES.apply(code);
        for (KeyBindingInfo info : entries) {
            String dk = info.displayKey;
            if (dk != null && dk.indexOf(" + ") >= 0) {
                String primary = dk.split(" \\+ ", 2)[0].trim();
                if (!primary.equals(keyName)) {
                    keyName = dk;
                }
                break;
            }
        }
        String title = Ui.loc.tf("keymapviewer.keyboard.tooltip.key", keyName);
        if (list.isEmpty()) {
            title += Ui.loc.t("keymapviewer.keyboard.tooltip.unbound");
        } else {
            title += Ui.loc.tf("keymapviewer.keyboard.tooltip.bindings", list.size());
        }
        int maxTw = 0;
        for (KeyBindingInfo info : entries) {
            maxTw = Math.max(maxTw, cv.textWidth("[" + info.modName + "] " + info.actionName + "  " + info.displayKey));
        }
        maxTw = Math.max(maxTw, cv.textWidth(title));
        int w = maxTw + pad * 2;

        // 内容较多时限制可视行数，用滚轮浏览；滚动范围每帧收敛
        int maxRows = Math.max(1, (panelH - 60) / rowH);
        tooltipVisibleRows = Math.min(entries.size(), maxRows);
        int limit = Math.max(0, entries.size() - tooltipVisibleRows);
        tooltipScroll = Math.max(0, Math.min(tooltipScroll, limit));
        int from = tooltipScroll;
        boolean clipped = entries.size() > tooltipVisibleRows;
        int contentsH = tooltipVisibleRows * rowH;
        int h = pad * 2 + (list.isEmpty() ? 12 : contentsH) + 4 + (clipped ? 10 : 0);
        int x = Math.min(mouseX + 14, panelX + panelW - w - 4);
        int y = Math.min(mouseY + 14, panelY + panelH - h - 4);
        cv.fill(x, y, w, h, 0xE0202024);
        cv.outline(x, y, w, h, 0xFF55555C);
        int ry = y + pad;
        cv.text(title, x + pad, ry, Colors.TEXT_YELLOW);
        ry += 12;
        for (int i = from; i < from + tooltipVisibleRows && i < entries.size(); i++) {
            KeyBindingInfo info = entries.get(i);
            String keyColorText = info.isBound() ? info.displayKey : unbound;
            int keyColor = KeyBindingManager.INSTANCE.isConflicted(info) ? Colors.TEXT_RED : Colors.TEXT_WHITE;
            String modStr = "[" + info.modName + "] ";
            cv.text(modStr, x + pad, ry, Colors.TEXT_GRAY);
            cv.text(info.actionName, x + pad + cv.textWidth(modStr), ry, Colors.TEXT_WHITE);
            cv.text(keyColorText, x + w - pad - cv.textWidth(keyColorText), ry, keyColor);
            ry += rowH;
        }
        if (clipped) {
            String hint = "滚动浏览 (" + (from + 1) + "-" + (from + tooltipVisibleRows) + "/" + entries.size() + ")";
            cv.text(hint, x + pad, ry + 1, Colors.TEXT_DIM);
        }
    }

    private static int comboRank(KeyBindingInfo info) {
        return info.isBound() && info.displayKey.indexOf(" + ") >= 0 ? 0 : 1;
    }

    /** 悬浮提示框滚轮滚动；返回 true 表示已消费（有提示框打开）。 */
    public boolean mouseScrolled(double vertical) {
        if (hoverKey < 0) {
            return false;
        }
        int total = KeyBindingManager.INSTANCE.bindingsForKey(hoverKey).size();
        int vis = Math.max(1, tooltipVisibleRows);
        int limit = Math.max(0, total - vis);
        if (vertical > 0) {
            tooltipScroll = Math.max(0, tooltipScroll - 1);
        } else if (vertical < 0) {
            tooltipScroll = Math.min(limit, tooltipScroll + 1);
        }
        return true;
    }

    private void drawKeyLabel(Canvas cv, String label, int x, int y, int w, int h, int fg) {
        if (label.isEmpty()) {
            return;
        }
        cv.text(label, x + Math.max(2, (w - cv.textWidth(label)) / 2),
                y + (h - cv.lineHeight()) / 2, fg);
    }

    private String labelFor(Canvas cv, int code, int keyW) {
        String label = KeyLabels.label(code);
        if (cv.textWidth(label) > keyW - 4) {
            label = KeyLabels.abbrev(code);
        }
        if (cv.textWidth(label) > keyW - 4) {
            label = "";
        }
        return label;
    }

    private static final String[] MOUSE_LABELS = {"LMB", "RMB", "MMB", "B4", "B5", "B6", "B7", "B8"};

    /** 鼠标键帽短标签（规范码 -100..-93 → 左/右/中/侧键）。 */
    private String mouseLabel(int code) {
        int ord = -code - 100;
        return ord >= 0 && ord < MOUSE_LABELS.length ? MOUSE_LABELS[ord] : "#" + code;
    }

    // ---------------------------------------------------------------
    // 输入：纯预览，无交互
    // ---------------------------------------------------------------

    public boolean mouseClicked(int mx, int my, int button) {
        return false;
    }

    public boolean keyPressed(int glfwKeyCode) {
        return false;
    }

    public void onClose() {
        // 无状态可清理
    }
}