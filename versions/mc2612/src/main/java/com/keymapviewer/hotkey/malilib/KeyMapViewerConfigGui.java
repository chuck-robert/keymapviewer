package com.keymapviewer.hotkey.malilib;

import com.keymapviewer.ui.KeySettingsScreen;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class KeyMapViewerConfigGui extends GuiConfigsBase {
    // true = called by MaLiLib (redirect to KeySettingsScreen immediately)
    // false = called by our "Masa Menu" button (show the fast-switcher header)
    private final boolean redirect;

    public KeyMapViewerConfigGui() {
        super(16, 50, "keymapviewer", null, "keymapviewer.gui.title", "1.0.2");
        this.redirect = true;
    }

    public KeyMapViewerConfigGui(Screen previousScreen) {
        super(16, 50, "keymapviewer", previousScreen, "keymapviewer.gui.title", "1.0.2");
        this.redirect = false;
    }

    @Override
    public void init() {
        super.init();
        if (redirect) {
            Minecraft.getInstance().setScreen(new KeySettingsScreen());
        }
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return new ArrayList<>();
    }
}
