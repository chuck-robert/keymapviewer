package com.keymapviewer.hotkey.malilib;

import com.keymapviewer.config.KeyViewerLog;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingProvider;
import com.keymapviewer.util.KeyNames;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * MaLiLib Keybind 提供者（Adapter 层）。
 *
 * 通过 MaLiLib 公开 API 读取所有 Masa 系列模组注册的按键：
 *   InputEventHandler.getKeybindManager() -> IKeybindManager
 *   IKeybindManager.getKeybindCategories() -> List<KeybindCategory>
 *   KeybindCategory.getModName() / getCategory() / getHotkeys()
 *   IHotkey.getKeybind() / getName() / getTranslatedName()
 *   IKeybind.getKeys() / getKeysDisplayString() / isValid() / isKeybindHeld()
 *
 * 不硬编码任何 Tweakeroo/Litematica/MiniHUD 细节：未来新增 Masa 模组
 * 只要它们注册到 MaLiLib 的 KeybindCategory 体系即可自动兼容。
 */
public final class MaLiLibKeyProvider implements KeyBindingProvider {

    @Override
    public String getSourceName() {
        return "malilib";
    }

    @Override
    public boolean isAvailable() {
        return MaLiLibCompat.isLoaded();
    }

    @Override
    public List<KeyBindingInfo> collect() {
        List<KeyBindingInfo> out = new ArrayList<>();
        if (!isAvailable()) {
            return out;
        }
        try {
            IKeybindManager manager = InputEventHandler.getKeybindManager();
            for (KeybindCategory category : manager.getKeybindCategories()) {
                String modName = category.getModName();
                String categoryName = category.getCategory();
                for (IHotkey hotkey : category.getHotkeys()) {
                    try {
                        collectHotkey(out, modName, categoryName, hotkey);
                    } catch (Throwable t) {
                        KeyViewerLog.LOG.warn("[keymapviewer] Failed to read MaLiLib hotkey '{}': {}",
                                hotkey.getName(), t.toString());
                    }
                }
            }
        } catch (Throwable t) {
            // MaLiLib 已安装但其 Keybind API 与本版不一致：降级并提示，不崩溃。
            KeyViewerLog.LOG.error("[keymapviewer] MaLiLib keybind API mismatch: {}", t.toString());
            MaLiLibCompat.reportIncompatible(t);
        }
        return out;
    }

    private static void collectHotkey(List<KeyBindingInfo> out, String modName, String categoryName, IHotkey hotkey) {
        IKeybind keybind = hotkey.getKeybind();
        if (keybind == null) {
            return;
        }
        List<Integer> keys = keybind.getKeys();
        List<Integer> keyboardKeys = new ArrayList<>();
        List<Integer> mouseKeys = new ArrayList<>();
        if (keys != null) {
            for (Integer c : keys) {
                if (c == null) {
                    continue;
                }
                if (c >= 32 && c <= 348) {
                    keyboardKeys.add(c);
                } else if (mouseOrdinalOf(c) != null) {
                    // 鼠标键统一为规范编码 -100..-93（左键=-100），供键盘视图/冲突/导出使用
                    mouseKeys.add(mouseOrdinalOf(c) - 100);
                }
            }
        }
        int[] codes = new int[keyboardKeys.size() + mouseKeys.size()];
        int ci = 0;
        for (Integer k : keyboardKeys) {
            codes[ci++] = k;
        }
        for (Integer m : mouseKeys) {
            codes[ci++] = m;
        }
        // 冲突/键盘视图用排序副本；显示按原始顺序（保留 MaLiLib 序列型组合按下次序）
        java.util.Arrays.sort(codes);
        String display = "";
        try {
            if (keybind.isValid()) {
                java.util.List<String> parts = new java.util.ArrayList<>();
                boolean unknownKey = false;
                if (keys != null) {
                    for (Integer code : keys) {
                        String name = displayNameOf(code);
                        if (name != null) {
                            parts.add(name);
                        } else {
                            unknownKey = true;
                        }
                    }
                }
                String lib = null;
                try {
                    lib = keybind.getKeysDisplayString();
                    if (lib != null && lib.indexOf(',') >= 0) {
                        lib = lib.replaceAll(",", " + ");
                    }
                } catch (Throwable t) {
                    lib = null;
                }
                int libTokens = (lib == null || lib.isEmpty()) ? 0 : lib.split(" \\+ ").length;
                if (!parts.isEmpty() && !unknownKey && libTokens <= (keys == null ? 0 : keys.size())) {
                    // 全部键都可命名且库串不带更多信息：用本地化显示
                    display = String.join(" + ", parts);
                } else if (unknownKey || libTokens > (keys == null ? 0 : keys.size())) {
                    // 存在无法命名的键（鼠标/F3 调试引导等）：采用 MaLiLib 原始显示保持与游戏内一致
                    display = lib == null ? String.join(" + ", parts) : lib;
                } else {
                    // 纯鼠标等非键盘系列：回退到 MaLiLib 原始显示串
                    display = keybind.getKeysDisplayString();
                    if (display == null) {
                        display = "";
                    } else if (display.indexOf(',') >= 0) {
                        display = display.replaceAll(",", " + ");
                    }
                }
            }
        } catch (Throwable t) {
            display = "";
        }
        // 0.16.x 没有 getTranslatedName，用 GUI 显示名回退到资源名
        String name = hotkey.getConfigGuiDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = hotkey.getName();
        }
        if (name == null) {
            name = "?";
        }
        out.add(new KeyBindingInfo(KeyBindingInfo.Source.MALILIB, modName, categoryName,
                name.trim(), display, codes, new MaLiLibHandle(hotkey), isDefault(hotkey)));
    }

    private static boolean isDefault(IHotkey hotkey) {
        try {
            return !hotkey.isModified();
        } catch (Throwable t) {
            return true;
        }
    }

    private static String displayNameOf(int code) {
        if (code >= 32 && code <= 348) {
            return KeyNames.pretty(code);
        }
        Integer btn = mouseOrdinalOf(code);
        if (btn != null) {
            return com.keymapviewer.util.Ui.loc.t("keymapviewer.mouse." + (btn + 1));
        }
        try {
            String lib = fi.dy.masa.malilib.util.KeyCodes.getNameForKey(code);
            if (lib != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "(?:MOUSE|BUTTON)[_\\s-]*(\\d{1,2})", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lib);
                if (m.find()) {
                    int n = Integer.parseInt(m.group(1));
                    if (n >= 1 && n <= 8) {
                        return com.keymapviewer.util.Ui.loc.t("keymapviewer.mouse." + n);
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    /** 鼠标键 → 0..7 序号；优先版本内常量反射表，再直连判定（MaLiLib 全版本与 LWJGL 负值）。 */
    private static Integer mouseOrdinalOf(int code) {
        Integer ord = MOUSE_ORD.get(code);
        if (ord != null) {
            return ord;
        }
        if (code >= -100 && code <= -93) {
            return -code - 100;   // MaLiLib MOUSE_BUTTON_n = -(101-n)
        }
        if (code >= -8 && code <= -1) {
            return -code - 1;     // LWJGL GLFW_MOUSE_BUTTON / MC InputConstants
        }
        return null;
    }

    private static final java.util.Map<Integer, Integer> MOUSE_ORD = mouseMap();

    private static java.util.Map<Integer, Integer> mouseMap() {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        try {
            for (int i = 1; i <= 8; i++) {
                Object v = fi.dy.masa.malilib.util.KeyCodes.class.getField("MOUSE_BUTTON_" + i).get(null);
                map.put(((Number) v).intValue(), i - 1);
            }
        } catch (Throwable t) {
            // 老版本可能没有这些常量，忽略
        }
        return map;
    }
}