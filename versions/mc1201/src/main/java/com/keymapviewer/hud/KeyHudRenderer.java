package com.keymapviewer.hud;

import com.keymapviewer.config.ModConfig;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingManager;
import com.keymapviewer.ui.CanvasGfx;
import com.keymapviewer.util.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * HUD 列表渲染（1.21.0–1.21.8，经典输入代，GuiGraphics + PoseStack 变换）。
 * 行模型 / 测量 / 位置 / 绘制统一走 {@link HudModel}。
 */
public final class KeyHudRenderer {

    private KeyHudRenderer() {
    }

    private static ModConfig cfg() {
        return ModConfig.INSTANCE;
    }

    /** HUD 渲染（HudRenderCallback）。 */
    public static void renderHud(GuiGraphics g, Minecraft mc) {
        ModConfig cfg = cfg();
        if (!cfg.hudEnabled) {
            return;
        }
        List<KeyBindingInfo> list = KeyBindingManager.INSTANCE.hudBindings(cfg);
        if (list.isEmpty()) {
            return;
        }
        List<HudModel.HudLine> lines = HudModel.composeLines(list, cfg, Lang.t("keymapviewer.unbound"));
        if (lines.isEmpty()) {
            return;
        }
        HudModel.Dim dim = HudModel.measure(lines, s -> mc.font.width(s));
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int[] pos = HudModel.resolvePosition(gw, gh, dim.width(), dim.height(),
                cfg.hudPreset, cfg.hudOffsetX, cfg.hudOffsetY);
        float scale = cfg.hudScale;

        // 1.21.0–1.21.8：GuiGraphics.pose() 为 PoseStack（经典 API）
        g.pose().pushPose();
        try {
            g.pose().translate(pos[0], pos[1], 0.0);
            g.pose().scale(scale, scale, 1.0f);
            HudModel.drawLines(new CanvasGfx(g), lines, cfg);
        } finally {
            g.pose().popPose();
        }
    }

    /** 设置界面内实时预览（按预设+偏移解析位置）。 */
    public static void renderPreview(GuiGraphics g, Minecraft mc) {
        ModConfig cfg = cfg();
        List<KeyBindingInfo> list = KeyBindingManager.INSTANCE.hudBindings(cfg);
        if (list.isEmpty()) {
            return;
        }
        List<HudModel.HudLine> lines = HudModel.composeLines(list, cfg, Lang.t("keymapviewer.unbound"));
        if (lines.isEmpty()) {
            return;
        }
        HudModel.Dim dim = HudModel.measure(lines, s -> mc.font.width(s));
        int gw = mc.getWindow().getGuiScaledWidth();
        int gh = mc.getWindow().getGuiScaledHeight();
        int[] pos = HudModel.resolvePosition(gw, gh, dim.width(), dim.height(),
                cfg.hudPreset, cfg.hudOffsetX, cfg.hudOffsetY);
        g.pose().pushPose();
        try {
            g.pose().translate(pos[0], pos[1], 0.0);
            HudModel.drawLines(new CanvasGfx(g), lines, cfg);
        } finally {
            g.pose().popPose();
        }
    }
}