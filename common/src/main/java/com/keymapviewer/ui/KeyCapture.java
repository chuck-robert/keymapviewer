package com.keymapviewer.ui;

import com.keymapviewer.hotkey.BindTarget;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingManager;
import com.keymapviewer.hotkey.KeySpec;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 改键捕捉控制器（草稿模式，支持完整组合键）：点击键位单元格聚焦 → 输入按键累积草稿 →
 * 点击其它处提交（ESC 取消，Delete/Backspace 清除）。原版键位不支持组合键：提交时只绑定第一个按键。
 * 纯逻辑模块：按键名显示由各版本模块注入（{@link #NAMES}）。
 */
public final class KeyCapture {
    public static final KeyCapture INSTANCE = new KeyCapture();

    /** 显示用按键名函数（各版本模块设置为对应版本的本地化名，如 KeyNames::pretty）。 */
    public static Function<Integer, String> NAMES = code -> "#" + code;

    private static final int MAX_KEYS = 6;

    private KeyBindingInfo target;
    private boolean active;
    private final List<Integer> draft = new ArrayList<>();

    private KeyCapture() {
    }

    public boolean isActive() {
        return active;
    }

    public KeyBindingInfo target() {
        return target;
    }

    public void start(KeyBindingInfo target) {
        this.target = target;
        this.active = true;
        this.draft.clear();
    }

    public void cancel() {
        this.active = false;
        this.target = null;
        this.draft.clear();
    }

    public boolean hasDraft() {
        return !draft.isEmpty();
    }

    public String draftDisplay() {
        if (!active || draft.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int code : draft) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(NAMES.apply(code));
        }
        return sb.toString();
    }

    public boolean handleKeyPress(int glfwKeyCode) {
        if (!active) {
            return false;
        }
        if (glfwKeyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        if (glfwKeyCode == GLFW.GLFW_KEY_DELETE || glfwKeyCode == GLFW.GLFW_KEY_BACKSPACE) {
            apply(new KeySpec[0]);
            return true;
        }
        if (draft.size() < MAX_KEYS && !draft.contains(glfwKeyCode)) {
            draft.add(glfwKeyCode);
        }
        return true;
    }

    private boolean targetIsVanilla() {
        return target != null && target.source == KeyBindingInfo.Source.VANILLA;
    }

    public void commit() {
        if (!active) {
            return;
        }
        if (!draft.isEmpty()) {
            List<KeySpec> specs = new ArrayList<>();
            for (int code : draft) {
                specs.add(KeySpec.keyboard(code));
                if (targetIsVanilla()) {
                    break;
                }
            }
            apply(specs.toArray(new KeySpec[0]));
        } else {
            active = false;
            target = null;
            draft.clear();
        }
    }

    private void apply(KeySpec[] specs) {
        BindTarget bt = null;
        if (target != null && target.handle != null) {
            try {
                bt = target.handle.bindTarget();
            } catch (Throwable t) {
                bt = null;
            }
        }
        if (bt != null) {
            try {
                bt.rebind(specs);
            } catch (Throwable t) {
                cancel();
                return;
            }
        }
        active = false;
        target = null;
        draft.clear();
        KeyBindingManager.INSTANCE.notifyBindingChanged();
    }
}