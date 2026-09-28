package com.keymapviewer.input;

/** 键盘采样抽象：判断某个 GLFW 键码当前是否被按住。 */
public interface KeySampler {
    boolean hasWindow();

    boolean isKeyDown(int glfwKeyCode);

    static KeySampler noWindow() {
        return new KeySampler() {
            @Override
            public boolean hasWindow() {
                return false;
            }

            @Override
            public boolean isKeyDown(int glfwKeyCode) {
                return false;
            }
        };
    }
}