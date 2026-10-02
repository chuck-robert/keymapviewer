package com.keymapviewer.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * 26.3 的按键采样实现。
 *
 * <p>26.1.x/26.2 时这里叫 {@code GlfwKeySampler}，靠 {@code Window.handle()} 拿 GLFW 窗口句柄再
 * 调 {@code glfwGetKey}。26.3 改用 SDL 后，{@code Window.handle()} 给的是 SDL 窗口句柄，
 * 且类路径上已没有 {@code lwjgl-glfw}——但原版自己提供了
 * {@link InputConstants#isKeyDown(int)}（参数是 SDL 扫描码），语义与 {@code glfwGetKey} 等价。</p>
 *
 * <p>于是采样只需把本模组的 GLFW 规范键码翻成 SDL 扫描码，无需再直接依赖任何 LWJGL 输入模块。</p>
 */
public final class SdlKeySampler implements KeySampler {

    @Override
    public boolean hasWindow() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc != null && mc.getWindow() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isKeyDown(int glfwKeyCode) {
        try {
            int scancode = SdlKeys.glfwToScancode(glfwKeyCode);
            if (scancode < 0) {
                return false;
            }
            return InputConstants.isKeyDown(scancode);
        } catch (Throwable t) {
            return false;
        }
    }
}
