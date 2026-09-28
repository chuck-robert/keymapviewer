package com.keymapviewer.hotkey;

/**
 * 键位来源句柄：读取当前按下状态，并提供重新绑定/恢复默认的入口。
 * 隔离原版 KeyMapping 与 MaLiLib IKeybind 两套改动机制。
 */
public interface KeySourceHandle {
    /** 当前是否处于按住状态。 */
    boolean isHeld();

    /** 重新绑定时的编辑目标（不可编辑的来源返回 null）。 */
    BindTarget bindTarget();

    /** 是否支持恢复默认绑定。 */
    default boolean canResetDefault() {
        return false;
    }

    /** 恢复默认绑定（并落盘）。 */
    default void resetDefault() {
    }
}