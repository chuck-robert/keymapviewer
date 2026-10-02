package com.keymapviewer.hotkey.malilib;

import com.keymapviewer.config.KeyViewerLog;
import com.keymapviewer.hotkey.BindTarget;
import com.keymapviewer.hotkey.KeySourceHandle;
import com.keymapviewer.hotkey.KeySpec;
import com.keymapviewer.input.SdlKeys;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;

/**
 * MaLiLib 热键的读写句柄。
 *
 * 修改走 MaLiLib 公开 API（clearKeys / addKey / resetToDefault），随后调用
 * ConfigManager.saveAllConfigs() 触发所有已注册 Masa 模组的配置落盘。
 * 持有 IHotkey 以便恢复默认绑定（IConfigValue.resetToDefault()）。
 *
 * 键码空间：本模组内部统一用 GLFW 规范键码、鼠标用 -100..-93（左键=-100）；
 * MaLiLib 0.30（26.3）已改为 SDL 键码——可打印键用字符码、特殊键用 0x40000000|扫描码，
 * 鼠标键为 MOUSE_LEFT=-99…MOUSE_EXTRA_5=-92 常量。故写入前一律经 {@link SdlKeys} 转换，
 * 常量值也不再靠临时反射读取（直接来自 SdlKeys 的固定表，避免版本间数值漂移）。
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
                    int code;
                    if (spec.mouse()) {
                        code = SdlKeys.malilibCodeFromMouseOrdinal(
                                SdlKeys.mouseOrdinalFromCanonical(spec.value()));
                        if (code == Integer.MIN_VALUE) {
                            continue;   // 无法映射的鼠标键，跳过
                        }
                    } else {
                        code = SdlKeys.glfwToMalilib(spec.value());
                        if (code < 0) {
                            continue;   // 无法映射的键盘键，跳过
                        }
                    }
                    keybind.addKey(code);
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
}
