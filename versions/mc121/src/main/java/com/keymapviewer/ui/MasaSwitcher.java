package com.keymapviewer.ui;

import com.keymapviewer.hotkey.malilib.KeyMapViewerConfigGui;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * MaLiLib 原生模组快速切换下拉框（同其配置界面的右上角切换器），
 * 直接内嵌在本界面右上角，替代原先“< Masa菜单”跳转空页的按钮。
 */
public final class MasaSwitcher extends WidgetDropDownList<ModInfo> {

    public MasaSwitcher(int x, int y) {
        super(x, y, 132, 18, 220, 10, Registry.CONFIG_SCREEN.getAllModsWithConfigScreens());
        ModInfo self = Registry.CONFIG_SCREEN.getModInfoFromConfigScreen(KeyMapViewerConfigGui.class);
        if (self == null) {
            String title = "KeymapViewer";
            try {
                title = Component.translatable("keymapviewer.screen.title").getString();
            } catch (Throwable ignored) {
            }
            final String fTitle = title;
            self = new ModInfo("keymapviewer", fTitle, () -> new KeyMapViewerConfigGui());
            Registry.CONFIG_SCREEN.registerConfigScreenFactory(self);
        }
        // 与 maLib 原版一致：默认选中当前模组；直接赋值字段避免触发 setSelectedEntry 的跳转动作
        this.selectedEntry = self;
    }

    @Override
    protected void setSelectedEntry(int index) {
        super.setSelectedEntry(index);
        ModInfo sel = getSelectedEntry();
        if (sel == null || "keymapviewer".equals(sel.getModId())) {
            return;
        }
        java.util.function.Supplier<GuiBase> factory = sel.getConfigScreenSupplier();
        if (factory != null) {
            Minecraft.getInstance().setScreen((Screen) factory.get());
        }
    }

    @Override
    protected String getDisplayString(ModInfo info) {
        return info.getModName();
    }

    /** 下拉列表展开中（供外层吞掉点击，避免穿透到面板）。 */
    public boolean openNow() {
        return isOpen;
    }

    /** 本界面渲染入口（0.25+ 的 render 参数序为 graphics, x, y, selected）。 */
    public void renderAt(GuiGraphics g, int mouseX, int mouseY) {
        this.render(g, mouseX, mouseY, false);
    }
}