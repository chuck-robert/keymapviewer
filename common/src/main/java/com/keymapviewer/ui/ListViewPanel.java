package com.keymapviewer.ui;

import com.keymapviewer.config.ModConfig;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingManager;
import com.keymapviewer.util.Colors;
import com.keymapviewer.util.Ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 列表视图（纯逻辑 + Canvas 绘制）：
 * - 模组行可折叠（抽屉）；
 * - 右侧滚动条（可滚轮/拖拽）；
 * - 首列「显示在 HUD」勾选（最多 10 个）；点击按键列改键（草稿模式）；
 * - 行尾「重置」按钮恢复默认绑定。
 */
public final class ListViewPanel {
    private record Row(boolean header, KeyBindingInfo info, String modName, int count) {
        static Row header(String modName, int count) {
            return new Row(true, null, modName, count);
        }

        static Row data(KeyBindingInfo info) {
            return new Row(false, info, null, 0);
        }
    }

    private final List<KeyBindingInfo> src = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final Set<String> collapsedMods = new HashSet<>();

    private int panelX, panelY, panelW, panelH;
    private int scrollOffset;

    private String notice;
    private long noticeUntil;

    private boolean draggingScroll;
    private int dragStartMouseY;
    private int dragStartOffset;

    // 绘制时缓存的列几何（点击判定复用）
    private int cachedResetX = -1;
    private int cachedResetW = -1;
    private int cachedKeyX = -1;
    private int cachedKeyW = -1;

    private static final int ROW_H = 15;
    private static final int HEAD_H = 14;
    private static final int PAD = 4;
    private static final int PIN_COL_W = 20;
    private static final int SCROLL_W = 6;
    private static final int RESET_GAP = 6;

    public ListViewPanel() {
    }

    public void setFiltered(List<KeyBindingInfo> filtered) {
        src.clear();
        src.addAll(filtered);
        rebuildRows();
    }

    public void setArea(int x, int y, int w, int h) {
        this.panelX = x;
        this.panelY = y;
        this.panelW = w;
        this.panelH = h;
        clampScroll();
    }

    public List<KeyBindingInfo> data() {
        return src;
    }

    private void rebuildRows() {
        rows.clear();
        String prev = null;
        boolean prevCollapsed = false;
        int headerIndex = -1;
        int modCount = 0;
        for (KeyBindingInfo info : src) {
            boolean header = prev == null || !info.modName.equals(prev);
            if (header) {
                prev = info.modName;
                prevCollapsed = collapsedMods.contains(info.modName);
                modCount = 0;
                rows.add(Row.header(info.modName, 0));
                headerIndex = rows.size() - 1;
            }
            modCount++;
            rows.set(headerIndex, Row.header(prev, modCount));
            if (!prevCollapsed) {
                rows.add(Row.data(info));
            }
        }
        clampScroll();
    }

    private void clampScroll() {
        int max = Math.max(0, rows.size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(scrollOffset, max));
    }

    private int visibleRows() {
        return Math.max(1, (panelH - HEAD_H - PAD - 4) / ROW_H);
    }

    private boolean needsScrollbar() {
        return rows.size() > visibleRows();
    }

    // ---------------------------------------------------------------
    // 列位置
    // ---------------------------------------------------------------

    private int scrollbarX() {
        return panelX + panelW - SCROLL_W - 1;
    }

    private String resetLabel() {
        return Ui.loc.t("keymapviewer.action.reset");
    }

    private int resetBtnWidth(Canvas cv) {
        return cv.textWidth(resetLabel()) + 12;
    }

    private int resetBtnX(Canvas cv) {
        int right = needsScrollbar() ? scrollbarX() - 3 : panelX + panelW - PAD;
        return right - resetBtnWidth(cv);
    }

    private int colPin() {
        return panelX + PAD;
    }

    private int colMod() {
        return colPin() + PIN_COL_W + 4;
    }

    private int colCat() {
        return colMod() + 130;
    }

    private int colAction() {
        return colCat() + 120;
    }

    private int keyColumnEnd(Canvas cv) {
        return resetBtnX(cv) - RESET_GAP;
    }

    private int colKeyX(Canvas cv) {
        int fromColumns = colAction() + 140;
        int end = keyColumnEnd(cv);
        return Math.max(PAD, Math.min(fromColumns, end - 80));
    }

    private int colKeyW(Canvas cv) {
        return Math.max(60, keyColumnEnd(cv) - colKeyX(cv));
    }

    // ---------------------------------------------------------------
    // 滚动条几何
    // ---------------------------------------------------------------

    private int scrollTrackTop() {
        return panelY + HEAD_H + 1;
    }

    private int scrollTrackBottom() {
        return panelY + panelH - 1;
    }

    private int scrollThumbHeight() {
        int track = scrollTrackBottom() - scrollTrackTop();
        int thumb = Math.max(22, track * visibleRows() / Math.max(1, rows.size()));
        return Math.min(track, thumb);
    }

    private int scrollThumbY() {
        int track = scrollTrackBottom() - scrollTrackTop() - scrollThumbHeight();
        int max = Math.max(0, rows.size() - visibleRows());
        if (max <= 0) {
            return scrollTrackTop();
        }
        return scrollTrackTop() + track * scrollOffset / max;
    }

    private void scrollToThumbY(int mouseY) {
        int track = scrollTrackBottom() - scrollTrackTop() - scrollThumbHeight();
        int max = Math.max(0, rows.size() - visibleRows());
        if (max <= 0 || track <= 0) {
            scrollOffset = 0;
            return;
        }
        int t = Math.max(0, Math.min(track, mouseY - scrollTrackTop()));
        scrollOffset = Math.round((float) max * t / track);
    }

    // ---------------------------------------------------------------
    // 绘制
    // ---------------------------------------------------------------

    public void draw(Canvas cv, int mouseX, int mouseY) {
        int headerTextY = panelY + Math.max(0, (HEAD_H - cv.lineHeight()) / 2);

        cachedResetW = resetBtnWidth(cv);
        cachedResetX = resetBtnX(cv);
        cachedKeyX = colKeyX(cv);
        cachedKeyW = colKeyW(cv);

        cv.text("HUD", colPin(), headerTextY, Colors.TEXT_DIM);
        cv.text(Ui.loc.t("keymapviewer.list.header.mod"), colMod(), headerTextY, Colors.TEXT_DIM);
        cv.text(Ui.loc.t("keymapviewer.list.header.category"), colCat(), headerTextY, Colors.TEXT_DIM);
        cv.text(Ui.loc.t("keymapviewer.list.header.action"), colAction(), headerTextY, Colors.TEXT_DIM);
        cv.text(Ui.loc.t("keymapviewer.list.header.key"), cachedKeyX, headerTextY, Colors.TEXT_DIM);
        cv.text(resetLabel(), cachedResetX, headerTextY, Colors.TEXT_DIM);
        cv.fill(panelX, panelY + HEAD_H - 1, panelW, 1, 0xFF3A3A40);
        String hintText = Ui.loc.t("keymapviewer.list.header.hint");
        int hintX = (needsScrollbar() ? scrollbarX() : panelX + panelW) - PAD - cv.textWidth(hintText);
        cv.text(hintText, hintX, headerTextY, Colors.TEXT_DIM);

        int y = panelY + HEAD_H + 5;
        for (int i = scrollOffset; i < rows.size(); i++) {
            if (y + ROW_H > panelY + panelH) {
                break;
            }
            Row row = rows.get(i);
            if (row.header()) {
                drawHeaderRow(cv, row, y, mouseX);
            } else {
                drawRow(cv, row.info(), y, mouseX, mouseY);
            }
            y += ROW_H;
        }

        if (rows.isEmpty()) {
            cv.text(Ui.loc.t("keymapviewer.list.empty"), colMod(), panelY + HEAD_H + 12, Colors.TEXT_GRAY);
        }

        if (needsScrollbar()) {
            int sbx = scrollbarX();
            cv.fill(sbx, scrollTrackTop(), SCROLL_W, scrollTrackBottom() - scrollTrackTop(), 0x88222A30);
            cv.fill(sbx, scrollThumbY(), SCROLL_W, scrollThumbHeight(), 0xAA9FB4C8);
        }

        // 覆盖红字的横幅等画到更高层，避免被行内文字盖住
        cv.beginStratum();
        if (KeyCapture.INSTANCE.isActive()) {
            boolean vanilla = KeyCapture.INSTANCE.target() != null
                    && KeyCapture.INSTANCE.target().source == KeyBindingInfo.Source.VANILLA;
            String hint;
            if (KeyCapture.INSTANCE.hasDraft()) {
                hint = Ui.loc.tf("keymapviewer.list.capture.entering", KeyCapture.INSTANCE.draftDisplay())
                        + (vanilla ? Ui.loc.t("keymapviewer.list.capture.vanillaOnly") : "")
                        + Ui.loc.t("keymapviewer.list.capture.confirm");
            } else {
                hint = Ui.loc.t("keymapviewer.list.capture.prompt");
            }
            cv.fill(panelX, panelY + HEAD_H + 2, panelW, 15, 0xC0222830);
            cv.text(hint, panelX + PAD + 8, panelY + HEAD_H + 5, Colors.TEXT_GREEN);
        }

        if (notice != null && Ui.millis < noticeUntil) {
            cv.text(notice, panelX + PAD, panelY + HEAD_H + 20, Colors.TEXT_YELLOW);
        } else {
            notice = null;
        }
    }

    private void drawHeaderRow(Canvas cv, Row row, int y, int mouseX) {
        boolean hovered = DrawUtils.inside(mouseX, y, panelX, panelY, panelW, ROW_H);
        boolean collapsed = collapsedMods.contains(row.modName());
        cv.fill(panelX, y, panelW, ROW_H - 1, hovered ? 0xFF28334A : 0xFF1E232C);
        String marker = collapsed ? "[+] " : "[-] ";
        String title = marker + row.modName() + "(" + row.count() + ")";
        cv.text(title, colMod(), y + 3, collapsed ? Colors.TEXT_DIM : Colors.TEXT_YELLOW);
        if (collapsed) {
            cv.text(Ui.loc.t("keymapviewer.list.collapsedHint"),
                    panelX + panelW - 150 - (needsScrollbar() ? SCROLL_W + 4 : 0), y + 3, Colors.TEXT_DIM);
        }
    }

    private void drawRow(Canvas cv, KeyBindingInfo info, int y, int mouseX, int mouseY) {
        boolean capturing = KeyCapture.INSTANCE.isActive() && KeyCapture.INSTANCE.target() == info;
        String unbound = Ui.loc.t("keymapviewer.unbound");

        String keyText;
        int keyColor;
        if (capturing) {
            keyText = KeyCapture.INSTANCE.hasDraft()
                    ? KeyCapture.INSTANCE.draftDisplay()
                    : Ui.loc.t("keymapviewer.list.capture.inputting");
            keyColor = Colors.KEY_HELD_TEXT;
        } else if (!info.isBound()) {
            keyText = unbound;
            keyColor = Colors.TEXT_GRAY;
        } else if (info.held) {
            keyText = info.displayKey;
            keyColor = Colors.TEXT_GREEN;
        } else if (KeyBindingManager.INSTANCE.isConflicted(info)) {
            keyText = info.displayKey;
            keyColor = Colors.TEXT_RED;
        } else {
            keyText = info.displayKey;
            keyColor = Colors.TEXT_WHITE;
        }

        if (capturing) {
            cv.fill(panelX, y, panelW, ROW_H - 1, 0x33C8C800);
        }

        boolean pinned = ModConfig.INSTANCE.isPinned(info);
        int cbSize = 12;
        int cbX = colPin() + (PIN_COL_W - cbSize) / 2;
        int cbY = y + (ROW_H - cbSize) / 2;
        if (pinned) {
            cv.fill(cbX, cbY, cbSize, cbSize, 0xFF2F7A3A);
            String tick = "v";
            cv.text(tick, cbX + (cbSize - cv.textWidth(tick)) / 2,
                    cbY + (cbSize - cv.lineHeight()) / 2, Colors.TEXT_WHITE);
        } else {
            cv.fill(cbX, cbY, cbSize, cbSize, 0xFF1A1A1F);
        }
        cv.outline(cbX, cbY, cbSize, cbSize, DrawUtils.inside(mouseX, mouseY, cbX - 2, cbY - 2, cbSize + 4, cbSize + 4)
                ? 0xFF6AAA6A : 0xFF44444C);

        cv.text(truncate(cv, info.modName, colCat() - colMod() - 6), colMod(), y + 3, Colors.TEXT_WHITE);
        cv.text(truncate(cv, info.category, colAction() - colCat() - 8), colCat(), y + 3, Colors.TEXT_DIM);
        cv.text(truncate(cv, info.actionName, cachedKeyX - colAction() - 8), colAction(), y + 3,
                !info.isBound() ? Colors.TEXT_GRAY : Colors.TEXT_WHITE);

        int kw = cachedKeyW;
        boolean keyHover = !KeyCapture.INSTANCE.isActive()
                && DrawUtils.inside(mouseX, mouseY, cachedKeyX - PAD, y, kw + PAD * 2, ROW_H);
        if (keyHover) {
            cv.fill(cachedKeyX - PAD, y, kw + PAD * 2, ROW_H, 0x28888888);
        }
        cv.text(truncate(cv, keyText, kw), cachedKeyX, y + 3, keyColor);

        boolean resettable = info.canResetDefault();
        int rbx = cachedResetX;
        int rbw = cachedResetW;
        boolean rbHover = resettable && !KeyCapture.INSTANCE.isActive()
                && DrawUtils.inside(mouseX, mouseY, rbx - 2, y, rbw + 4, ROW_H);
        if (resettable) {
            boolean defaultBg = info.isDefault;
            int bg = rbHover ? 0xFF3A3A42 : (defaultBg ? 0xFF1E1E24 : 0xFF2E5A2E);
            int border = defaultBg ? 0xFF3A3A44 : 0xFF5AAD5A;
            int fg = rbHover ? Colors.TEXT_WHITE : (defaultBg ? Colors.TEXT_DIM : Colors.TEXT_GREEN);
            cv.fill(rbx - 2, y + 1, rbw + 4, ROW_H - 2, bg);
            cv.outline(rbx - 2, y + 1, rbw + 4, ROW_H - 2, border);
            cv.text(resetLabel(), rbx + (rbw - cv.textWidth(resetLabel())) / 2, y + 3, fg);
        } else {
            cv.text("-", rbx, y + 3, Colors.TEXT_DIM);
        }
    }

    private static String truncate(Canvas cv, String s, int maxW) {
        if (cv.textWidth(s) <= maxW) {
            return s;
        }
        while (cv.textWidth(s) > maxW && s.length() > 3) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    // ---------------------------------------------------------------
    // 输入
    // ---------------------------------------------------------------

    public boolean mouseClicked(int mx, int my, int button, int modifiers) {
        if (button != 0) {
            return false;
        }
        if (KeyCapture.INSTANCE.isActive()) {
            KeyCapture.INSTANCE.commit();
            return true;
        }
        if (!DrawUtils.inside(mx, my, panelX, panelY, panelW, panelH)) {
            return false;
        }

        if (needsScrollbar()
                && DrawUtils.inside(mx, my, scrollbarX(), scrollTrackTop(), SCROLL_W + 2, scrollTrackBottom() - scrollTrackTop())) {
            if (my >= scrollThumbY() && my < scrollThumbY() + scrollThumbHeight()) {
                draggingScroll = true;
                dragStartMouseY = my;
                dragStartOffset = scrollOffset;
            } else {
                scrollToThumbY(my);
            }
            return true;
        }

        int visualRow = scrollOffset + (my - (panelY + HEAD_H + 5)) / ROW_H;
        if (visualRow < 0 || visualRow >= rows.size()) {
            return false;
        }
        Row row = rows.get(visualRow);
        if (row == null) {
            return false;
        }
        if (row.header()) {
            toggleMod(row.modName());
            return true;
        }
        KeyBindingInfo info = row.info();

        if (DrawUtils.inside(mx, my, colPin() - 3, panelY + HEAD_H, PIN_COL_W + 6, panelH - HEAD_H)) {
            if (ModConfig.INSTANCE.isPinned(info)) {
                ModConfig.INSTANCE.unpin(info);
            } else if (!ModConfig.INSTANCE.tryPin(info)) {
                showNotice(Ui.loc.t("keymapviewer.list.pinnedLimit"));
            }
            KeyBindingManager.INSTANCE.notifyBindingChanged();
            return true;
        }

        int rbx = cachedResetX >= 0 ? cachedResetX : colMod();
        int rbw = cachedResetW >= 0 ? cachedResetW : 40;
        if (DrawUtils.inside(mx, my, rbx - 2, panelY + HEAD_H, rbw + 4, panelH - HEAD_H)) {
            if (info.canResetDefault()) {
                info.resetDefaultBinding();
                showNotice(Ui.loc.tf("keymapviewer.list.resetDone", info.actionName));
                KeyBindingManager.INSTANCE.notifyBindingChanged();
            } else {
                showNotice(Ui.loc.t("keymapviewer.list.resetUnsupported"));
            }
            return true;
        }

        int kx = cachedKeyX >= 0 ? cachedKeyX : colMod();
        int kw = cachedKeyW >= 0 ? cachedKeyW : 80;
        if (DrawUtils.inside(mx, my, kx - PAD, panelY + HEAD_H, kw + PAD * 2, panelH - HEAD_H)) {
            if (info.handle != null && info.handle.bindTarget() != null) {
                KeyCapture.INSTANCE.start(info);
            }
            return true;
        }
        return false;
    }

    public boolean mouseDragged(int mx, int my) {
        if (draggingScroll) {
            int dy = my - dragStartMouseY;
            int max = Math.max(0, rows.size() - visibleRows());
            if (max > 0) {
                int perPixel = Math.max(1, (scrollTrackBottom() - scrollTrackTop() - scrollThumbHeight()) / max);
                scrollOffset = Math.max(0, Math.min(max, dragStartOffset + dy / perPixel));
            }
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        draggingScroll = false;
    }

    private void toggleMod(String modName) {
        if (!collapsedMods.add(modName)) {
            collapsedMods.remove(modName);
        }
        rebuildRows();
    }

    public boolean mouseScrolled(double horizontal, double vertical) {
        if (vertical > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (vertical < 0) {
            clampScroll();
            if (scrollOffset < Math.max(0, rows.size() - visibleRows())) {
                scrollOffset++;
            }
        }
        return true;
    }

    public boolean keyPressed(int glfwKeyCode) {
        if (KeyCapture.INSTANCE.isActive()) {
            return KeyCapture.INSTANCE.handleKeyPress(glfwKeyCode);
        }
        return false;
    }

    private void showNotice(String text) {
        notice = text;
        noticeUntil = Ui.millis + 3000;
    }

    public void onClose() {
        if (KeyCapture.INSTANCE.isActive()) {
            KeyCapture.INSTANCE.cancel();
        }
        draggingScroll = false;
    }
}