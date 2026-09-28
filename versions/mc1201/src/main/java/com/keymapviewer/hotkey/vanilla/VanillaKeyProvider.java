package com.keymapviewer.hotkey.vanilla;

import com.keymapviewer.hotkey.BindTarget;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingProvider;
import com.keymapviewer.hotkey.KeySourceHandle;
import com.keymapviewer.hotkey.KeySpec;
import com.keymapviewer.util.KeyNames;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 原版 KeyMapping 提供者。
 * 覆盖 Minecraft 原生键位 + 一切通过 Fabric KeyMappingHelper 注册的模组 vanilla 键位
 * （Options.keyMappings 是它们的统一注册表）。
 */
public final class VanillaKeyProvider implements KeyBindingProvider {

    @Override
    public String getSourceName() {
        return "vanilla";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<KeyBindingInfo> collect() {
        List<KeyBindingInfo> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return out;
        }
        String modName = Component.translatable("keymapviewer.mod.vanilla").getString();
        for (KeyMapping km : mc.options.keyMappings) {
            if (km == null) {
                continue;
            }
            InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(km);
            boolean bound = !InputConstants.UNKNOWN.equals(key);
            int[] codes = new int[0];
            String display = "";
            if (bound) {
                if (key.getType() == InputConstants.Type.KEYSYM) {
                    int v = key.getValue();
                    if (v >= 32 && v <= 348) {
                        codes = new int[]{v};
                    }
                    if (v >= 0) {
                        display = KeyNames.pretty(v);
                    } else {
                        display = key.getDisplayName().getString();
                    }
                } else if (key.getType() == InputConstants.Type.MOUSE) {
                    int btn = Math.max(1, Math.min(8, key.getValue() + 1));
                    // 鼠标规范化编码 -100..-93（左键=-100），与 MaLiLib 空间一致，供键盘视图/冲突/导出使用
                    codes = new int[]{btn - 101};
                    display = Component.translatable("keymapviewer.mouse." + btn).getString();
                } else {
                    display = key.getDisplayName().getString();
                }
            }
            String category;
            try {
                // 1.21.0–1.21.8：KeyMapping.getCategory() 为 String（分类键），1.21.9 起才是记录
                category = Component.translatable(km.getCategory()).getString();
            } catch (Exception e) {
                category = "?";
            }
            String actionName = Component.translatable(km.getName()).getString();
            out.add(new KeyBindingInfo(KeyBindingInfo.Source.VANILLA, modName, category, actionName,
                    display, codes, new VanillaHandle(km), isVanillaDefault(km)));
        }
        injectDebugShortcuts(out, modName);
        return out;
    }

    private static void injectDebugShortcuts(List<KeyBindingInfo> out, String modName) {
        String category = Component.translatable("keymapviewer.category.debug").getString();
        int f3 = 292;
        Object[][] entries = {
            {65, "reloadChunks"}, {66, "hitboxes"}, {67, "copyLocation"}, {68, "clearChat"},
            {70, "renderDistance"}, {71, "chunkBorders"}, {72, "tooltips"}, {73, "copyBlockInfo"},
            {78, "gameMode"}, {80, "autoPause"}, {81, "shortcuts"}, {84, "reloadResources"}
        };
        for (Object[] e : entries) {
            int k = (int) e[0];
            String name = Component.translatable("keymapviewer.debug." + e[1]).getString();
            String disp = KeyNames.pretty(f3) + " + " + KeyNames.pretty(k);
            out.add(new KeyBindingInfo(KeyBindingInfo.Source.VANILLA, modName, category,
                    name, disp, new int[]{k, f3}, null, true));
        }
    }

    /** 原版 KeyMapping 的读写句柄。 */
    private static boolean isVanillaDefault(KeyMapping km) {
        try {
            return KeyBindingHelper.getBoundKeyOf(km).equals(km.getDefaultKey());
        } catch (Throwable t) {
            return true;
        }
    }

    private static final class VanillaHandle implements KeySourceHandle {
        private final KeyMapping mapping;

        VanillaHandle(KeyMapping mapping) {
            this.mapping = mapping;
        }

        @Override
        public boolean isHeld() {
            return mapping.isDown();
        }

        @Override
        public boolean canResetDefault() {
            return true;
        }

        @Override
        public void resetDefault() {
            mapping.setKey(mapping.getDefaultKey());
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
        }

        @Override
        public BindTarget bindTarget() {
            return specs -> {
                InputConstants.Key key = InputConstants.UNKNOWN;
                if (specs != null && specs.length > 0) {
                    KeySpec spec = specs[0];
                    InputConstants.Type type = spec.mouse() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
                    key = type.getOrCreate(spec.value());
                }
                mapping.setKey(key);
                // 重建键→绑定表，使新绑定立即生效
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            };
        }
    }
}