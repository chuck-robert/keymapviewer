package com.keymapviewer.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 26.1.x 渲染管线（GuiGraphicsExtractor）的 Canvas 实现。 */
public final class CanvasExt implements Canvas {
    private final GuiGraphicsExtractor ext;

    public CanvasExt(GuiGraphicsExtractor ext) {
        this.ext = ext;
    }

    @Override
    public void text(String s, int x, int y, int color) {
        ext.text(Minecraft.getInstance().font, s, x, y, color);
    }

    @Override
    public void fill(int x, int y, int w, int h, int color) {
        ext.fill(x, y, x + w, y + h, color);
    }

    @Override
    public void outline(int x, int y, int w, int h, int color) {
        ext.outline(x, y, w, h, color);
    }

    @Override
    public int textWidth(String s) {
        return Minecraft.getInstance().font.width(s);
    }

    @Override
    public int lineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public void beginStratum() {
        ext.nextStratum();
    }

    @Override
    public void enableScissor(int x, int y, int w, int h) {
        ext.enableScissor(x, y, w, h);
    }

    @Override
    public void disableScissor() {
        ext.disableScissor();
    }
}