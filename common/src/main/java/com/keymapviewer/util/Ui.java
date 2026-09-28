package com.keymapviewer.util;

/**
 * 跨版本 UI 宿主：本地化与屏幕尺寸。各版本模块在客户端初始化与每帧渲染时更新。
 */
public final class Ui {
    public interface Localizer {
        String t(String key);

        String tf(String key, Object... args);
    }

    /** 默认原样返回 key（版本模块未设置时退回显示 key 本身）。 */
    public static Localizer loc = new Localizer() {
        @Override
        public String t(String key) {
            return key;
        }

        @Override
        public String tf(String key, Object... args) {
            return key;
        }
    };

    public static int scaledWidth = 400;
    public static int scaledHeight = 300;

    /** 单调毫秒时间（各版本模块每帧更新），供面板提示计时。 */
    public static long millis = 0L;

    /** 设置界面是否打开（由客户端入口与屏幕关闭时维护，避免读 Minecraft.screen 跨版本漂移）。 */
    public static boolean settingsOpen;

    private Ui() {
    }
}