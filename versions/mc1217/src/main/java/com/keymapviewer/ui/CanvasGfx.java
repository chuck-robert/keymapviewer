package com.keymapviewer.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** 1.21.7+（经典绘制管线 GuiGraphics）的 Canvas 实现。 */
public final class CanvasGfx implements Canvas {
    private final GuiGraphics g;

    public CanvasGfx(GuiGraphics g) {
        this.g = g;
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    @Override
    public void text(String s, int x, int y, int color) {
        g.drawString(font(), s, x, y, color);
    }

    @Override
    public void fill(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.fill(x, y, x + w, y + h, color);
    }

    @Override
    public void outline(int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public int textWidth(String s) {
        return font().width(s);
    }

    @Override
    public int lineHeight() {
        return font().lineHeight;
    }

    @Override
    public void enableScissor(int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
    }

    @Override
    public void disableScissor() {
        g.disableScissor();
    }
}