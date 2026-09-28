package com.keymapviewer.hotkey.malilib;

import net.fabricmc.loader.api.FabricLoader;

/**
 * MaLiLib 可用性检测与兼容报告。
 *
 * 设计说明（对应计划书第 2、3 节）：
 * - 通过 FabricLoader.isModLoaded("malilib") 检测运行期是否安装 MaLiLib。
 * - 对 MaLiLib Keybind API 的调用全部集中于 {@link MaLiLibKeyProvider}，
 *   并在采集过程包 try/catch(Throwable)：即使未来 MaLiLib 大版本 API 变动，
 *   本模组也会降级为“仅原版键位 + 兼容性提示”，而不会崩溃。
 */
public final class MaLiLibCompat {
    public static final String MOD_ID = "malilib";

    private static String incompatibilityReason;

    private MaLiLibCompat() {
    }

    public static boolean isLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded(MOD_ID);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 记录因 MaLiLib API 变动导致的失败原因（可空）。 */
    public static void reportIncompatible(Throwable t) {
        incompatibilityReason = t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    /** 当前 MaLiLib 兼容性原因；null 表示正常或未安装。 */
    public static String incompatibilityReason() {
        return incompatibilityReason;
    }
}