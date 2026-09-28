package com.keymapviewer.hotkey.malilib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/**
 * 打开一个 Masa 模组（Tweakeroo/MiniHUD 等）的配置界面；
 * 其 MaLiLib 表头自带「返回 Masa 模组菜单」的上角组件。
 * 用反射访问 Registry.CONFIG_SCREEN，保证各 MaLiLib 版本都能编译；老版本缺失时静默返回 false。
 */
public final class MaLiLibMenu {
    private MaLiLibMenu() {
    }

    public static boolean openRoot() {
        try {
            Class<?> regC = Class.forName("fi.dy.masa.malilib.registry.Registry");
            Field cfgF = regC.getField("CONFIG_SCREEN");
            Object cfgScreen = cfgF.get(null);
            Method allM = cfgScreen.getClass().getMethod("getAllModsWithConfigScreens");
            List<?> mods = (List<?>) allM.invoke(cfgScreen);
            if (mods == null || mods.isEmpty()) {
                return false;
            }
            Object modInfo = mods.get(0);
            Method facM = cfgScreen.getClass().getMethod("getConfigScreenFactoryFor", modInfo.getClass());
            Object fac = facM.invoke(cfgScreen, modInfo);
            if (fac == null) {
                return false;
            }
            Object gui = ((Supplier<?>) fac).get();
            Class<?> base = Class.forName("fi.dy.masa.malilib.gui.GuiBase");
            Method open = base.getMethod("openGui", base);
            open.invoke(null, gui);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
