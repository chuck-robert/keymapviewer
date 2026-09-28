package com.keymapviewer.hotkey;

import com.keymapviewer.config.KeyViewerLog;
import com.keymapviewer.config.ModConfig;
import com.keymapviewer.input.KeySampler;
import com.keymapviewer.util.Ui;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 键位聚合器（纯逻辑）：
 * 合并各版本模块注入的 Provider、维护冲突表、通过注入的 KeySampler 采样
 * “按下 / 最近触发”状态，并周期性刷新。
 */
public final class KeyBindingManager {
    public static final KeyBindingManager INSTANCE = new KeyBindingManager();

    private static final int ORDER_VANILLA = 0;
    private static final int ORDER_MALILIB = 1;
    private static final long REFRESH_INTERVAL_TICKS = 80;

    private List<KeyBindingProvider> providers = new ArrayList<>();
    private KeySampler sampler = KeySampler.noWindow();

    private final List<KeyBindingInfo> bindings = new ArrayList<>();
    private final Map<Integer, List<KeyBindingInfo>> byKeyCode = new HashMap<>();

    private long tick;
    private long lastRefreshTick = Long.MIN_VALUE;
    private boolean everRefreshed;
    private int generation;
    private String adapterError;

    private KeyBindingManager() {
    }

    /** 各版本模块入口调用：注入 Provider 列表与键盘采样器。 */
    public synchronized void configure(List<KeyBindingProvider> providerList, KeySampler samplerImpl) {
        if (providerList != null) {
            this.providers = new ArrayList<>(providerList);
        }
        if (samplerImpl != null) {
            this.sampler = samplerImpl;
        }
    }

    /** 记录适配层（如 MaLiLib）不兼容原因，供界面提示。 */
    public void reportAdapterError(String reason) {
        this.adapterError = reason;
    }

    public String adapterError() {
        return adapterError;
    }

    public int generation() {
        return generation;
    }

    /** 客户端 tick：采样按键、更新按下/最近触发状态，周期性重建绑定列表。 */
    public void tick() {
        tick++;
        if (tick - lastRefreshTick >= REFRESH_INTERVAL_TICKS || !everRefreshed) {
            refreshNow();
        }
        samplePressedKeys();
        updateHeldFlags();
        pruneRecent();
    }

    private boolean chordActive;

    /** 查询注入的采样器是否按住某键（跨版本静态入口）。 */
    public boolean isKeyHeld(int glfwCode) {
        return sampler != null && sampler.isKeyDown(glfwCode);
    }

    /** HUD 显隐切换由各版本模块注册的原版键位（默认 F12）决定，此处不再自行轮询。 */
    private void checkHudToggleCombo() {
    }

    /** 立即重建绑定列表（改键后 / 进入设置时调用）。 */
    public void refreshNow() {
        List<KeyBindingInfo> rebuilt = new ArrayList<>();
        for (KeyBindingProvider provider : providers) {
            if (!provider.isAvailable()) {
                continue;
            }
            try {
                rebuilt.addAll(provider.collect());
            } catch (Throwable t) {
                KeyViewerLog.LOG.error("[keymapviewer] provider {} failed: {}", provider.getSourceName(), t.toString());
            }
        }
        rebuilt.sort((a, b) -> {
            int bySource = Integer.compare(sourceOrder(a), sourceOrder(b));
            return bySource != 0 ? bySource : a.sortKey().compareTo(b.sortKey());
        });

        bindings.clear();
        bindings.addAll(rebuilt);

        byKeyCode.clear();
        for (KeyBindingInfo info : bindings) {
            for (int code : info.keyCodes) {
                byKeyCode.computeIfAbsent(code, k -> new ArrayList<>()).add(info);
            }
        }
        lastRefreshTick = tick;
        everRefreshed = true;
        generation++;
    }

    private static int sourceOrder(KeyBindingInfo a) {
        return a.source == KeyBindingInfo.Source.VANILLA ? ORDER_VANILLA : ORDER_MALILIB;
    }

    public List<KeyBindingInfo> allBindings() {
        return Collections.unmodifiableList(bindings);
    }

    public List<KeyBindingInfo> filtered(ModConfig cfg) {
        return filtered(cfg, false);
    }

    public List<KeyBindingInfo> filteredHud(ModConfig cfg) {
        return filtered(cfg, cfg.recentOnly);
    }

    public List<KeyBindingInfo> hudBindings(ModConfig cfg) {
        if (cfg.pinned != null && !cfg.pinned.isEmpty()) {
            List<KeyBindingInfo> resolved = new ArrayList<>();
            for (String key : cfg.pinned) {
                for (KeyBindingInfo info : bindings) {
                    if (ModConfig.pinKey(info).equals(key)) {
                        resolved.add(info);
                        break;
                    }
                }
                if (resolved.size() >= 10) {
                    break;
                }
            }
            return resolved;
        }
        return filteredHud(cfg);
    }

    private List<KeyBindingInfo> filtered(ModConfig cfg, boolean recentOnly) {
        if (!everRefreshed) {
            refreshNow();
        }
        List<KeyBindingInfo> out = new ArrayList<>();
        for (KeyBindingInfo info : bindings) {
            boolean fromVanilla = info.source == KeyBindingInfo.Source.VANILLA;
            if (fromVanilla && !cfg.showVanilla) {
                continue;
            }
            if (!info.isBound() && !cfg.showUnbound) {
                continue;
            }
            if (recentOnly && !recentOrHeld(info)) {
                continue;
            }
            out.add(info);
        }
        return out;
    }

    private boolean recentOrHeld(KeyBindingInfo info) {
        return info.held || info.isRecentlyTriggered(tick, 120);
    }

    public boolean isConflicted(KeyBindingInfo info) {
        for (int code : info.keyCodes) {
            List<KeyBindingInfo> sharing = byKeyCode.get(code);
            if (sharing != null && sharing.size() > 1) {
                return true;
            }
        }
        return false;
    }

    public int usageCount(int keyCode) {
        List<KeyBindingInfo> sharing = byKeyCode.get(keyCode);
        return sharing == null ? 0 : sharing.size();
    }

    public List<KeyBindingInfo> bindingsForKey(int keyCode) {
        List<KeyBindingInfo> sharing = byKeyCode.get(keyCode);
        if (sharing == null) {
            return Collections.emptyList();
        }
        List<KeyBindingInfo> copy = new ArrayList<>(sharing);
        copy.sort((a, b) -> {
            int bySource = Integer.compare(sourceOrder(a), sourceOrder(b));
            return bySource != 0 ? bySource : a.sortKey().compareTo(b.sortKey());
        });
        return copy;
    }

    /** 开发自测：把当前键位列成文本写入文件（KVM_DEV 环境变量触发）。 */
    public void dumpBindingsToFile(java.nio.file.Path file) {
        try {
            StringBuilder sb = new StringBuilder();
            for (KeyBindingInfo info : bindings) {
                sb.append(info.toString()).append('\n');
            }
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file, sb.toString());
            KeyViewerLog.LOG.info("[keymapviewer] dumped {} bindings to {}", bindings.size(), file);
        } catch (Exception e) {
            KeyViewerLog.LOG.warn("[keymapviewer] failed to dump bindings: {}", e.toString());
        }
    }

    // ---------------------------------------------------------------
    // 状态采样
    // ---------------------------------------------------------------

    private final boolean[] prevKeyStates = new boolean[349];

    private void samplePressedKeys() {
        if (!sampler.hasWindow()) {
            return;
        }
        for (int code = 32; code < 349; code++) {
            boolean now = sampler.isKeyDown(code);
            if (now != prevKeyStates[code]) {
                prevKeyStates[code] = now;
                if (now) {
                    onKeyPressed(code, tick);
                }
            }
        }
    }

    private void onKeyPressed(int code, long nowTick) {
        List<KeyBindingInfo> sharing = byKeyCode.get(code);
        if (sharing == null) {
            return;
        }
        for (KeyBindingInfo info : sharing) {
            info.lastTriggeredTick = nowTick;
        }
        recentTriggered.add(code);
    }

    private final List<Integer> recentTriggered = new ArrayList<>();

    public List<Integer> recentKeys() {
        pruneRecent();
        return Collections.unmodifiableList(recentTriggered);
    }

    private static final int RECENT_LIMIT = 32;

    private void pruneRecent() {
        if (recentTriggered.size() > RECENT_LIMIT) {
            recentTriggered.subList(RECENT_LIMIT, recentTriggered.size()).clear();
        }
    }

    private void updateHeldFlags() {
        for (KeyBindingInfo info : bindings) {
            try {
                info.held = info.handle != null && info.handle.isHeld();
            } catch (Throwable t) {
                info.held = false;
            }
        }
    }

    public boolean isKeyDown(int code) {
        return sampler.isKeyDown(code);
    }

    public void notifyBindingChanged() {
        refreshNow();
    }

    public static boolean isKeyboardCode(int code) {
        return code >= 32 && code <= 348;
    }
}