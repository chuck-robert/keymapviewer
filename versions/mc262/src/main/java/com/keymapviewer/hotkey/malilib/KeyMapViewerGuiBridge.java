package com.keymapviewer.hotkey.malilib;

import com.keymapviewer.ui.KeySettingsScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import net.minecraft.client.Minecraft;

/**
 * MaLiLib「打开配置界面」模组列表的桥接屏：
 * MaLiLib 列表要求配置屏工厂返回 GuiBase，这里用一个瞬态 GuiBase 立即转发到本模组的设置界面。
 * 仅当 MaLiLib 已安装时才会被实例化，因此不会给无 MaLiLib 环境带来类加载依赖。
 */
public final class KeyMapViewerGuiBridge extends GuiBase {

    public KeyMapViewerGuiBridge() {
        super();
    }

    @Override
    public void init() {
        Minecraft.getInstance().setScreenAndShow(new KeySettingsScreen());
    }

    @Override
    public void initGui() {
        Minecraft.getInstance().setScreenAndShow(new KeySettingsScreen());
    }
}