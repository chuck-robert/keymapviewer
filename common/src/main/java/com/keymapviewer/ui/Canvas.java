package com.keymapviewer.ui;

/**
 * 跨版本绘制抽象：各版本模块的渲染管线（GuiGraphicsExtractor / GuiGraphics）
 * 各自实现该接口，common 里的面板/绘制辅助只依赖它。
 */
public interface Canvas {

    void text(String s, int x, int y, int color);

    void fill(int x, int y, int w, int h, int color);

    void outline(int x, int y, int w, int h, int color);

    int textWidth(String s);

    int lineHeight();

    default void beginStratum() {
    }

    default void enableScissor(int x, int y, int w, int h) {
    }

    default void disableScissor() {
    }
}