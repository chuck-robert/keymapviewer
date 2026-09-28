package com.keymapviewer;

import com.keymapviewer.config.ModConfig;
import com.keymapviewer.hotkey.KeyBindingManager;
import com.keymapviewer.hotkey.malilib.MaLiLibCompat;
import com.keymapviewer.hotkey.malilib.MaLiLibKeyProvider;
import com.keymapviewer.hotkey.vanilla.VanillaKeyProvider;
import com.keymapviewer.hud.KeyHudRenderer;
import com.keymapviewer.input.GlfwKeySampler;
import com.keymapviewer.ui.KeyCapture;
import com.keymapviewer.ui.KeySettingsScreen;
import com.keymapviewer.util.KeyNames;
import com.keymapviewer.util.Lang;
import com.keymapviewer.util.Ui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;

/**
 * 模组入口（client 端，26.1.x）：
 * 1. 注入 common 的 Provider 列表 / 键盘采样器 / 本地化器 / 配置路径；
 * 2. 注册原版键位（默认 K）与 HUD 元素；
 * 3. 注册到 MaLiLib 模组列表（右上角菜单）；
 * 4. 每 tick 刷新按键状态。
 */
public class KeyMapViewerClient implements ClientModInitializer {
    public static final String MOD_ID = "keymapviewer";

    private static KeyMapping settingsKey;
    private static KeyMapping toggleHudKey;
    private static net.minecraft.client.gui.screens.Screen openedScreen;

    @Override
    public void onInitializeClient() {
        // ---- 注入 common 依赖 ----
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("keymapviewer.json");
        ModConfig.setConfigPath(configFile);
        Ui.loc = langAdapter();
        KeyCapture.NAMES = KeyNames::pretty;

        KeyBindingManager.INSTANCE.configure(
                List.of(new VanillaKeyProvider(), new MaLiLibKeyProvider()),
                new GlfwKeySampler());

        ModConfig.INSTANCE.load();

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "keymapviewer.key.openSettings",
                GLFW.GLFW_KEY_K,
                settingsCategory()));

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "keymapviewer.key.toggleHud",
                GLFW.GLFW_KEY_F12,
                settingsCategory()));

        registerWithMaLiLib();

        HudRenderCallback.EVENT.register((graphics, deltaTracker) ->
                KeyHudRenderer.renderHud(graphics, Minecraft.getInstance()));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (settingsKey != null && settingsKey.consumeClick()) {
                if (com.keymapviewer.util.Ui.settingsOpen) {
                    openedScreen = null;
                    mc.setScreen(null);
                    com.keymapviewer.util.Ui.settingsOpen = false;
                } else {
                    openedScreen = new KeySettingsScreen();
                    com.keymapviewer.util.Ui.settingsOpen = true;
                    mc.setScreen(openedScreen);
                }
            }
            while (toggleHudKey != null && toggleHudKey.consumeClick()) {
                com.keymapviewer.config.ModConfig cfg = com.keymapviewer.config.ModConfig.INSTANCE;
                cfg.hudEnabled = !cfg.hudEnabled;
                cfg.save();
            }
            devTick(mc);
            KeyBindingManager.INSTANCE.tick();
        });
    }

    private static Ui.Localizer langAdapter() {
        return new Ui.Localizer() {
            @Override
            public String t(String key) {
                return Lang.t(key);
            }

            @Override
            public String tf(String key, Object... args) {
                return Lang.tf(key, args);
            }
        };
    }

    /**
     * 1.20.1 时代的 MaLiLib（0.16.x）没有「模组配置列表」注册 API（Registry 是 0.27+ 才引入），跳过。
     */
    private void registerWithMaLiLib() {
        // 1.20.1 无需注册；保留空实现以保持入口一致
    }

    // 开发/兼容性自测钩子：设置环境变量 KVM_DEV 后，约 4 秒自动打开设置界面，
    // 并在 ~10 秒后把收集到的键位写入运行目录 kvm_bindings.json，便于无人值守验证。
    private int devTicks;

    private void devTick(Minecraft mc) {
        if (System.getenv("KVM_DEV") == null) {
            return;
        }
        devTicks++;
        if (devTicks == 80) {
            String view = System.getenv("KVM_DEV_VIEW");
            if ("keyboard".equals(view)) {
                ModConfig.INSTANCE.defaultView = "keyboard";
            } else if ("hud".equals(view)) {
                ModConfig.INSTANCE.defaultView = "hud";
            } else {
                ModConfig.INSTANCE.defaultView = "list";
            }
            mc.setScreen(new KeySettingsScreen());
            com.keymapviewer.util.Ui.settingsOpen = true;
            com.keymapviewer.config.KeyViewerLog.LOG.info("[keymapviewer] AUTOTEST settings-opened");
        } else if (devTicks == 200) {
            if (openedScreen instanceof KeySettingsScreen s) {
                s.showKeyboardView();
            }
        } else if (devTicks == 400) {
            KeyBindingManager.INSTANCE.dumpBindingsToFile(FabricLoader.getInstance().getGameDir().resolve("kvm_bindings.json"));
        }
    }

    // 1.21.0–1.21.8：KeyBinding 分类为 String（1.21.9 起才改为 Category 记录）
    private static String settingsCategory() {
        return "keymapviewer.category";
    }
}