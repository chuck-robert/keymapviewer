package com.keymapviewer.hotkey.vanilla;

import com.keymapviewer.hotkey.BindTarget;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingProvider;
import com.keymapviewer.hotkey.KeySourceHandle;
import com.keymapviewer.hotkey.KeySpec;
import com.keymapviewer.input.SdlKeys;
import com.keymapviewer.util.KeyNames;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 原版 KeyMapping 提供者。
 * 覆盖 Minecraft 原生键位 + 一切通过 Fabric KeyMappingHelper 注册的模组 vanilla 键位
 * （Options.keyMappings 是它们的统一注册表）。
 *
 * <p>26.3 的两处差异（相对 26.1.x/26.2）：
 * <ol>
 *   <li>{@code InputConstants.Type.KEYSYM} 更名为 {@code KEYBOARD}，且取值是 <b>SDL 扫描码</b>
 *       ——读写都要经 {@link SdlKeys} 与 GLFW 规范空间互转；</li>
 *   <li>鼠标键号改为 SDL 约定（1=左 2=中 3=右），需换成语言文件既有的 1=左 2=右 3=中 序号。</li>
 * </ol>
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
            InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(km);
            boolean bound = !InputConstants.UNKNOWN.equals(key);
            int[] codes = new int[0];
            String display = "";
            if (bound) {
                if (key.getType() == InputConstants.Type.KEYBOARD) {
                    // 26.3：原版键值为 SDL 扫描码，先归一化回本模组的 GLFW 规范码
                    int v = SdlKeys.scancodeToGlfw(key.getValue());
                    if (v >= 32 && v <= 348) {
                        codes = new int[]{v};
                    }
                    if (v >= 0) {
                        display = KeyNames.pretty(v);
                    } else {
                        display = key.getDisplayName().getString();
                    }
                } else if (key.getType() == InputConstants.Type.MOUSE) {
                    // 26.3：鼠标键号为 SDL 约定，转成语言文件序号（1=左 2=右 3=中）
                    int btn = SdlKeys.mouseOrdinalFromVanilla(key.getValue());
                    if (btn < 1) {
                        btn = 1;
                    }
                    // 鼠标规范化编码 -100..-93（左键=-100），与 MaLiLib 空间一致，供键盘视图/冲突/导出使用
                    codes = new int[]{SdlKeys.canonicalMouseCode(btn)};
                    display = Component.translatable("keymapviewer.mouse." + btn).getString();
                } else {
                    display = key.getDisplayName().getString();
                }
            }
            String category;
            try {
                category = km.getCategory().label().getString();
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
        int f3 = SdlKeys.GLFW_KEY_F3;
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
            return KeyMappingHelper.getBoundKeyOf(km).equals(km.getDefaultKey());
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
                // 原版键位只接受首键；本模组的规范码在此转回 26.3 的 SDL 扫描码 / SDL 鼠标键号
                InputConstants.Key key = InputConstants.UNKNOWN;
                if (specs != null && specs.length > 0) {
                    KeySpec spec = specs[0];
                    if (spec.mouse()) {
                        int btn = SdlKeys.vanillaButtonFromMouseOrdinal(
                                SdlKeys.mouseOrdinalFromCanonical(spec.value()));
                        if (btn > 0) {
                            key = InputConstants.Type.MOUSE.getOrCreate(btn);
                        }
                    } else {
                        int scancode = SdlKeys.glfwToScancode(spec.value());
                        if (scancode >= 0) {
                            key = InputConstants.Type.KEYBOARD.getOrCreate(scancode);
                        }
                    }
                }
                mapping.setKey(key);
                // 重建键→绑定表，使新绑定立即生效
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            };
        }
    }
}
