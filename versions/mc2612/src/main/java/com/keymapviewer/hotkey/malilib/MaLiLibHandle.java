package com.keymapviewer.hotkey.malilib;

import com.keymapviewer.config.KeyViewerLog;
import com.keymapviewer.hotkey.BindTarget;
import com.keymapviewer.hotkey.KeySourceHandle;
import com.keymapviewer.hotkey.KeySpec;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.util.KeyCodes;

/**
 * MaLiLib 热键的读写句柄。
 *
 * 修改走 MaLiLib 公开 API（clearKeys / addKey / resetToDefault），随后调用
 * ConfigManager.saveAllConfigs() 触发所有已注册 Masa 模组的配置落盘。
 * 持有 IHotkey 以便恢复默认绑定（IConfigValue.resetToDefault()）。
 *
 * 鼠标键编码：MaLiLib 用 KeyCodes.MOUSE_BUTTON_N 常量表示鼠标键，其数值随版本而定，
 * 这里在改键时通过反射读取常量值，保证对旧/新版本都正确。
 */
public final class MaLiLibHandle implements KeySourceHandle {
    private final IHotkey hotkey;
    private final IKeybind keybind;

    public MaLiLibHandle(IHotkey hotkey) {
        this.hotkey = hotkey;
        this.keybind = hotkey.getKeybind();
    }

    @Override
    public boolean isHeld() {
        try {
            return keybind != null && keybind.isKeybindHeld();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean canResetDefault() {
        return hotkey != null;
    }

    @Override
    public void resetDefault() {
        try {
            hotkey.resetToDefault();
            if (keybind != null) {
                keybind.markDirty();
            }
            saveAll();
            // 刷新 MaLiLib 键→热键表，使新绑定立即生效（否则需重进世界）
            InputEventHandler.getKeybindManager().updateUsedKeys();
        } catch (Throwable t) {
            KeyViewerLog.LOG.warn("[keymapviewer] failed to reset MaLiLib hotkey", t);
        }
    }

    @Override
    public BindTarget bindTarget() {
        return specs -> {
            if (keybind != null) {
                keybind.clearKeys();
                for (KeySpec spec : specs) {
                    int code = spec.mouse() ? mouseStorageCode(spec.value()) : spec.value();
                    if (code != Integer.MIN_VALUE) {
                        keybind.addKey(code);
                    }
                }
                try {
                    keybind.markDirty();
                } catch (Throwable t) {
                    // 部分实现没有 markDirty，忽略
                }
            }
            saveAll();
            // 刷新 MaLiLib 键→热键表，使新绑定立即生效（否则需重进世界）
            InputEventHandler.getKeybindManager().updateUsedKeys();
        };
    }

    private static void saveAll() {
        // saveAllConfigs 仅存在于具体实现（接口未声明），此处显式转型调用
        ((ConfigManager) ConfigManager.getInstance()).saveAllConfigs();
    }

    /** 读取 MaLiLib KeyCodes.MOUSE_BUTTON_N 常量；读取失败返回 MIN_VALUE（跳过）。 */
    private static int mouseStorageCode(int button) {
        if (button < 0 || button > 7) {
            return Integer.MIN_VALUE;
        }
        try {
            return (int) KeyCodes.class.getField("MOUSE_BUTTON_" + (button + 1)).get(null);
        } catch (Throwable t) {
            KeyViewerLog.LOG.warn("[keymapviewer] Cannot read MaLiLib mouse button constant MOUSE_BUTTON_{}", button + 1, t);
            return Integer.MIN_VALUE;
        }
    }
}