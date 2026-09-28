package com.keymapviewer.hotkey;

/**
 * 一次绑定的键位描述：区分键盘键（GLFW 键码）与鼠标键（0..7）。
 */
public record KeySpec(boolean mouse, int value) {

    /** 键盘用的 GLFW 键码（32..348），否则 -1。 */
    public int keyboardCode() {
        return mouse ? -1 : value;
    }

    public static KeySpec keyboard(int code) {
        return new KeySpec(false, code);
    }

    public static KeySpec mouse(int button) {
        return new KeySpec(true, button);
    }
}