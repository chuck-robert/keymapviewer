# Key Map Viewer（按键显示）

一个 100% 客户端的 Minecraft Fabric 模组：在 HUD 上实时显示你绑定的所有按键，
并提供「列表视图 + 键盘视图 + HUD 设置」三个分页用于查看、固定与修改
**原版键位** 与 **MaLiLib 系列模组**（Tweakeroo / Litematica / MiniHUD / Item Scroller 等）的按键。

兼容目标：**MC 1.20.1 · 1.21（0–11）· 26.1.x / 26.2**，每个目标各自配套 Fabric Loader 0.19.x / Fabric API / MaLiLib / Java 版本（见 COMPATIBILITY.md）

## 功能

- **HUD 实时显示**：默认按模组分组的按键清单；若在列表中固定了键位（勾选第一列，最多 10 个），
  则 HUD 优先只显示固定条目。
- **列表视图**：表格展示所有绑定，支持搜索（操作名/模组/分类/按键）、
  「显示原版 / 显示未绑定」勾选、滚动；点击按键列进入**改键**：
  - 点击按键 → 该行高亮（聚焦）；
  - 输入按键（支持 Ctrl/Shift/Alt/Win 组合，画面即时预览）；
  - 点击其它处 = 提交；ESC = 取消；Delete = 清除绑定。
  - 默认打开方式：**K** 键（可在原版按键设置中修改，分类为「按键查看器」）。
- **键盘视图**：纯预览——按状态显示键帽颜色（灰=未绑定 / 白=单绑定 / 红=冲突 / 绿=按下），
  悬停键帽在顶部显示绑定统计；仅预览，不可改键、无弹窗。
  布局为「主键盘 + 下方导航键/小键盘」，窄窗口自动等比缩小，不溢出。
- **HUD 设置页**（分节）：
  - 显示：HUD 开关 / 显示未绑定 / 只显示最近触发 / 按分类分组
  - 位置：**预设下拉**（四角 / 顶部居中 / 左右缘 / 物品栏两侧）+ **X/Y 偏移** 微调，实时预览
  - 外观：缩放 / 透明度（− + / 重置）
  - 说明：状态颜色图例
- **状态配色**：`灰` 未绑定 · `白` 已绑定 · `红` 键位冲突 · `绿` 当前按下/最近触发。
- **改键持久化**：原版 → `options.txt`；MaLiLib → `clearKeys()/addKey()` + `ConfigManager.saveAllConfigs()`。
- **MaLiLib 键位始终显示**（无开关）；未安装 MaLiLib 时自动回退为纯原版模式。

## 构建（多版本）

一键构建全部版本并把所有 jar 汇总到 `build_pure/`：

```bash
# 需要 JDK 25（1.20.1/1.21 模块会自动编译到各自的 Java 目标）
set JAVA_HOME=C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot
gradlew.bat buildPure
```

产物目录 `build_pure/`：

| jar 文件 | 覆盖的 MC 版本 | 渲染 | 输入模型 |
|---|---|---|---|
| `keymapviewer-1.0.0-mc1.20.1.jar` | 1.20.1 | GuiGraphics | 经典坐标 |
| `keymapviewer-1.0.0-mc1.21.0-1.21.8.jar` | 1.21.0 ~ 1.21.8 | GuiGraphics | 经典坐标 |
| `keymapviewer-1.0.0-mc1.21.9-1.21.11.jar` | 1.21.9 ~ 1.21.11 | GuiGraphics | 新事件模型 |
| `keymapviewer-1.0.0-mc26.1-26.2.jar` | 26.1.x / 26.2 | GuiGraphicsExtractor | 新事件模型 |

架构：`common/` 为纯逻辑共享层（配置/模型/聚合器/改键/键盘布局/HUD 行模型/Canvas 面板，
无 MC 依赖）；`versions/mc1201·mc121·mc1217·mc2612` 各只含 ~9 个真差异粘合文件
（Canvas 实现/本地化/键名/采样器/原版与 MaLiLib Provider/HUD/设置屏/入口）。
规则：**同一输入+渲染世代共享一个 jar**；每个 jar 在运行时由 Fabric Loader 做跨版本重映射。

> 版本边界依据（实证）：
> - 1.21.9 起键盘/鼠标改为 `KeyEvent`/`MouseButtonEvent` 记录模型并引入 `KeyMapping.Category`
>   （此前为经典坐标输入 + String 分类）；1.21.0–1.21.8 与 1.20.1 同为经典一代。
> - 26 系才是 `GuiGraphicsExtractor` 渲染管线；1.20.1–1.21.11 全程 `GuiGraphics`。
> - 各模块的 Fabric API / Loom / MaLiLib / Java 目标均有配套版本（见各 `versions/*/gradle.properties`）。
> - 构建每个历史版本模块需要与 **fabric-api 构建时配套的 Loom 版本**（旧版固件使用 buildscript 方式引入 Loom）。

## 配置

`config/keymapviewer.json`：

```json
{
  "hudPreset": "TOP_LEFT",
  "hudOffsetX": 0, "hudOffsetY": 0,
  "hudScale": 1.0,
  "hudTextOpacity": 0.9, "hudBgOpacity": 0.75,
  "hudEnabled": true, "showHudCategories": true,
  "showVanilla": true, "showUnbound": false, "recentOnly": false,
  "pinned": [],
  "defaultView": "list"
}
```

> HUD 文字与背景（黑框）透明度可独立调整；右侧预设（右上/右下/右中/物品栏右侧）
> 时整块按右缘对齐排版。

## 开发自测

```bash
KVM_DEV=1 gradlew.bat runClient   # 4s 自动开界面（KVM_DEV_VIEW=list|keyboard|hud）
                                   # 10s 切键盘视图，20s 导出 run/kvm_bindings.json
```