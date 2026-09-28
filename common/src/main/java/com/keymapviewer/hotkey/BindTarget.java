package com.keymapviewer.hotkey;

/**
 * 重新绑定落盘接口：把一组键位描述写回目标绑定。
 * 传入空数组表示清除绑定。
 */
public interface BindTarget {
    void rebind(KeySpec[] specs);
}