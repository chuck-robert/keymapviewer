package com.keymapviewer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.keymapviewer.hotkey.KeyBindingInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局配置，持久化到 config/keymapviewer.json。
 * HUD 位置由「预设锚点 + XY 偏移」描述。
 * 纯逻辑模块：配置目录由各版本模块在客户端启动时注入（{@link #setConfigPath}）。
 */
public final class ModConfig {
    public static final ModConfig INSTANCE = new ModConfig();

    /** HUD 位置预设锚点（显示文本由各版本模块通过 labelKey 本地化）。 */
    public enum HudPreset {
        TOP_LEFT("keymapviewer.preset.top_left"),
        TOP_RIGHT("keymapviewer.preset.top_right"),
        BOTTOM_LEFT("keymapviewer.preset.bottom_left"),
        BOTTOM_RIGHT("keymapviewer.preset.bottom_right"),
        TOP_CENTER("keymapviewer.preset.top_center"),
        MID_LEFT("keymapviewer.preset.mid_left"),
        MID_RIGHT("keymapviewer.preset.mid_right"),
        HOTBAR_LEFT("keymapviewer.preset.hotbar_left"),
        HOTBAR_RIGHT("keymapviewer.preset.hotbar_right");

        public final String labelKey;

        HudPreset(String labelKey) {
            this.labelKey = labelKey;
        }
    }

    private static volatile Path configPath;

    /** 各版本模块入口调用：指向 FabricLoader 的 config 目录。 */
    public static void setConfigPath(Path path) {
        configPath = path;
    }

    public static Path configPath() {
        Path p = configPath;
        return p != null ? p : Paths.get("config", "keymapviewer.json");
    }

    // HUD
    public HudPreset hudPreset = HudPreset.TOP_LEFT;
    public int hudOffsetX = 0;
    public int hudOffsetY = 0;
    public float hudScale = 1.0f;
    public float hudTextOpacity = 0.9f;
    public float hudBgOpacity = 0.75f;
    public boolean hudEnabled = false; // 首次加载默认隐藏 HUD
    public boolean showHudCategories = true;

    // 显示过滤（MaLiLib 键位始终显示，无开关）
    public boolean showVanilla = true;
    public boolean showUnbound = false;
    public boolean recentOnly = false;

    // 固定到 HUD 的键位（最多 10 个；键 = source|mod|category|action）
    public List<String> pinned = new ArrayList<>();

    // 视图
    public String defaultView = "list";

    private ModConfig() {
    }

    // ---------------------------------------------------------------
    // 固定键位（HUD 显示用）
    // ---------------------------------------------------------------

    public static String pinKey(KeyBindingInfo info) {
        return info.source.name() + '|' + info.modName + '|' + info.category + '|' + info.actionName;
    }

    public boolean isPinned(KeyBindingInfo info) {
        return pinned.contains(pinKey(info));
    }

    /** 尝试添加固定键位；返回 false 表示已达上限。 */
    public boolean tryPin(KeyBindingInfo info) {
        String key = pinKey(info);
        if (pinned.contains(key)) {
            return true;
        }
        if (pinned.size() >= 10) {
            return false;
        }
        pinned.add(key);
        save();
        return true;
    }

    public void unpin(KeyBindingInfo info) {
        pinned.remove(pinKey(info));
        save();
    }

    public void togglePin(KeyBindingInfo info) {
        if (isPinned(info)) {
            unpin(info);
        } else {
            tryPin(info);
        }
    }

    // ---------------------------------------------------------------
    // 持久化
    // ---------------------------------------------------------------

    public void load() {
        Path file = configPath();
        if (!Files.isReadable(file)) {
            return;
        }
        try {
            String json = Files.readString(file);
            ModConfig loaded = new Gson().fromJson(json, ModConfig.class);
            if (loaded != null) {
                copyFrom(loaded);
            }
        } catch (Exception e) {
            KeyViewerLog.LOG.error("Failed to load keymapviewer config", e);
        }
    }

    public void save() {
        Path file = configPath();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(this);
            Files.writeString(file, json);
        } catch (IOException e) {
            KeyViewerLog.LOG.error("Failed to save keymapviewer config", e);
        }
    }

    private void copyFrom(ModConfig other) {
        this.hudPreset = other.hudPreset != null ? other.hudPreset : HudPreset.TOP_LEFT;
        this.hudOffsetX = clampInt(other.hudOffsetX, -1000, 1000, 0);
        this.hudOffsetY = clampInt(other.hudOffsetY, -1000, 1000, 0);
        this.hudScale = clamp(other.hudScale, 0.5f, 3.0f, 1.0f);
        this.hudTextOpacity = clamp(other.hudTextOpacity, 0.1f, 1.0f, 0.9f);
        this.hudBgOpacity = clamp(other.hudBgOpacity, 0.1f, 1.0f, 0.75f);
        this.hudEnabled = other.hudEnabled;
        this.showHudCategories = other.showHudCategories;
        this.showVanilla = other.showVanilla;
        this.showUnbound = other.showUnbound;
        this.recentOnly = other.recentOnly;
        if (other.pinned != null && other.pinned.size() <= 10) {
            List<String> clean = new ArrayList<>();
            for (String k : other.pinned) {
                if (k != null && k.indexOf('|') > 0 && !clean.contains(k)) {
                    clean.add(k);
                }
            }
            this.pinned = clean;
        }
        String ext = other.defaultView;
        this.defaultView = ("keyboard".equals(ext) || "hud".equals(ext)) ? ext : "list";
    }

    private static int clampInt(int v, int min, int max, int def) {
        return (v < min || v > max) ? def : v;
    }

    private static float clamp(float v, float min, float max, float def) {
        return (v < min || v > max || Float.isNaN(v)) ? def : v;
    }
}