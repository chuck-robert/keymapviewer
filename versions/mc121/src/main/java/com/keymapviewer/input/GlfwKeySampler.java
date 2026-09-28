package com.keymapviewer.input;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 26.1.x：通过 Window.handle() 拿到 GLFW 窗口句柄采样按键，
 * 实现 common 的 {@link KeySampler} 供聚合器使用。
 */
public final class GlfwKeySampler implements KeySampler {

    @Override
    public boolean hasWindow() {
        return handle() != 0L;
    }

    @Override
    public boolean isKeyDown(int glfwKeyCode) {
        long h = handle();
        if (h == 0L) {
            return false;
        }
        try {
            return GLFW.glfwGetKey(h, glfwKeyCode) == GLFW.GLFW_PRESS;
        } catch (Throwable t) {
            return false;
        }
    }

    private static long handle() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return 0L;
            }
            Window window = mc.getWindow();
            if (window == null) {
                return 0L;
            }
            long h = window.getWindow();
            return h == 0L ? 0L : h;
        } catch (Throwable t) {
            return 0L;
        }
    }
}