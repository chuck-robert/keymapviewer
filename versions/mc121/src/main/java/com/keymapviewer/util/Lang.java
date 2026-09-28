package com.keymapviewer.util;

import net.minecraft.network.chat.Component;

/** 多语言文本工具：所有 UI 文案统一走语言文件。 */
public final class Lang {
    private Lang() {
    }

    public static String t(String key) {
        return Component.translatable(key).getString();
    }

    /** 带参数的本地化文本，如 Lang.tf("keymapviewer.list.resetDone", action)。 */
    public static String tf(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}