package com.keymapviewer.hotkey;

/**
 * 一份按键绑定的统一模型，同时承载「原版 KeyMapping」与「MaLiLib Keybind」。
 * 键盘视图 / 列表视图 / HUD 全部基于本模型渲染。
 */
public final class KeyBindingInfo {
    public enum Source {
        VANILLA,
        MALILIB
    }

    public final Source source;
    public final String modName;      // 所属模组显示名，如 "原版" / "Tweakeroo"
    public final String category;     // 分类名（原版为键位分类，MaLiLib 为 KeybindCategory）
    public final String actionName;   // 操作名（已本地化）
    public final String displayKey;   // 按键显示串，如 "M + C"；未绑定为空串
    public final int[] keyCodes;      // 参与键盘键位布局/冲突检测的 GLFW 键盘码（32..348）
    public final KeySourceHandle handle; // 读写状态 / 重新绑定的句柄（只读来源可为 null）
    public final boolean isDefault;      // 是否为该来源的默认绑定（未改动过）

    // 运行时动态状态
    public boolean held;
    public long lastTriggeredTick = -1;

    private String sortKey;

    public KeyBindingInfo(Source source, String modName, String category, String actionName,
                          String displayKey, int[] keyCodes, KeySourceHandle handle, boolean isDefault) {
        this.source = source;
        this.modName = modName;
        this.category = category;
        this.actionName = actionName;
        this.displayKey = displayKey;
        this.keyCodes = keyCodes;
        this.handle = handle;
        this.isDefault = isDefault;
    }

    public boolean isBound() {
        return displayKey != null && !displayKey.isEmpty();
    }

    public boolean isRecentlyTriggered(long nowTick, long windowTicks) {
        return lastTriggeredTick >= 0 && (nowTick - lastTriggeredTick) <= windowTicks;
    }

    public boolean isConflicted() {
        return KeyBindingManager.INSTANCE.isConflicted(this);
    }

    public boolean canResetDefault() {
        return handle != null && handle.canResetDefault();
    }

    /** 恢复默认绑定并落盘。 */
    public void resetDefaultBinding() {
        if (handle != null) {
            handle.resetDefault();
        }
    }

    public String fullLabel() {
        return actionName + (isBound() ? "  " + displayKey : "");
    }

    public String sortKey() {
        if (sortKey == null) {
            sortKey = source.name() + '' + modName + '' + category + '' + actionName;
        }
        return sortKey;
    }

    @Override
    public String toString() {
        return "[" + source + "] " + modName + "/" + category + "/" + actionName + " = " + displayKey;
    }
}