package com.keymapviewer.hotkey;

import java.util.List;

/** 键位来源 Provider：把某一套键位系统枚举成统一模型。 */
public interface KeyBindingProvider {
    String getSourceName();

    boolean isAvailable();

    List<KeyBindingInfo> collect();
}