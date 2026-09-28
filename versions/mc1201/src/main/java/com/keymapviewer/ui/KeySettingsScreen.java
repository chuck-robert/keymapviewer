package com.keymapviewer.ui;

import com.keymapviewer.config.ModConfig;
import com.keymapviewer.hotkey.KeyBindingInfo;
import com.keymapviewer.hotkey.KeyBindingManager;
import com.keymapviewer.util.Colors;
import com.keymapviewer.util.Lang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 主设置界面，三个分页：
 *  - 列表视图：搜索 + 原版/未绑定勾选 + 全量表格（可固定到 HUD、可改键）
 *  - 键盘视图：纯预览键盘（无编辑、无弹窗）
 *  - HUD 设置：显示开关 / 位置预设(W四个角/三条边/物品栏两侧) + XY 偏移 / 缩放透明度，实时预览
 *
 * 输入分发：改键捕捉（草稿模式）→ 原生控件 → 视图画布。点击捕捉行外部 = 提交草稿。
 */
public final class KeySettingsScreen extends Screen {
    public enum Tab {
        LIST, KEYBOARD, HUD_SETTINGS
    }

    private Tab activeTab;
    private KeyboardViewPanel keyboardPanel;
    private ListViewPanel listPanel;
    private int lastGeneration = -1;

    private final List<Renderable> myWidgets = new ArrayList<>();
    private final List<GuiEventListener> myListeners = new ArrayList<>();

    // 列表页：搜索 + 勾选
    private EditBox searchBox;
    private Checkbox cbVanilla;
    private Checkbox cbUnbound;

    // HUD设置页
    private Checkbox cbHud;
    private Checkbox cbCategories;
    private Button presetCycle;
    private Button bOffsetXMinus;
    private Button bOffsetXPlus;
    private Button bOffsetYMinus;
    private Button bOffsetYPlus;
    private Button bOffsetReset;
    private Button bTextMinus;
    private Button bTextPlus;
    private Button bBgMinus;
    private Button bBgPlus;
    private Button bScaleMinus;
    private Button bScalePlus;
    private Button bExport;
    private Button bImport;
    private String hudNotice;
    private long hudNoticeUntil;
    private int hudNoticeRow;

    // 本版 MaLiLib 0.16 无原生模组切换器（ConfigScreenRegistry/ModInfo 未引入），
    // 且跳转到空白的 GuiConfigsBase 体验差，故不注册按钮。
    private void tryRegMasaSwitcher() {
    }

    public KeySettingsScreen() {
        super(Component.translatable("keymapviewer.screen.title"));
        ModConfig cfg = ModConfig.INSTANCE;
        switch (cfg.defaultView == null ? "list" : cfg.defaultView) {
            case "keyboard" -> activeTab = Tab.KEYBOARD;
            case "hud" -> activeTab = Tab.HUD_SETTINGS;
            default -> activeTab = Tab.LIST;
        }
    }

    // ---------------------------------------------------------------
    // 布局
    // ---------------------------------------------------------------

    private <T extends GuiEventListener & Renderable & net.minecraft.client.gui.narration.NarratableEntry> T reg(T widget) {
        myWidgets.add(widget);
        myListeners.add(widget);
        return addRenderableWidget(widget);
    }

    @Override
    protected void init() {
        super.init();
        // 窗口 resize 时 Screen 会重复调用 init()，先清空已注册控件避免重复绘制
        myWidgets.clear();
        myListeners.clear();

        Minecraft mc = Minecraft.getInstance();
        KeyBindingManager.INSTANCE.refreshNow();
        keyboardPanel = new KeyboardViewPanel();
        listPanel = new ListViewPanel();

        // ---- 顶部标签页 ----
        reg(Button.builder(Component.translatable("keymapviewer.view.list"), b -> switchTab(Tab.LIST))
                .bounds(10, 25, 150, 18).build());
        reg(Button.builder(Component.translatable("keymapviewer.view.keyboard"), b -> switchTab(Tab.KEYBOARD))
                .bounds(164, 25, 140, 18).build());
        reg(Button.builder(Component.translatable("keymapviewer.view.hud"), b -> switchTab(Tab.HUD_SETTINGS))
                .bounds(308, 25, 140, 18).build());
        tryRegMasaSwitcher();


        // 列表页工具栏（仅列表页显示，其它页隐藏）
        int toolbarY = 47;
        searchBox = reg(new EditBox(mc.font, 10, toolbarY, 300, 18,
                Component.translatable("keymapviewer.search.hint")));
        searchBox.setResponder(s -> refreshListFilter());
        cbVanilla = reg(kvmCheckbox(330, toolbarY - 3,
                Component.translatable("keymapviewer.checkbox.showVanilla"),
                ModConfig.INSTANCE.showVanilla, () -> {
                    ModConfig.INSTANCE.showVanilla = cbVanilla.selected();
                    ModConfig.INSTANCE.save();
                    refreshListFilter();
                }));
        cbUnbound = reg(kvmCheckbox(420, toolbarY - 3,
                Component.translatable("keymapviewer.checkbox.showUnbound"),
                ModConfig.INSTANCE.showUnbound, () -> {
                    ModConfig.INSTANCE.showUnbound = cbUnbound.selected();
                    ModConfig.INSTANCE.save();
                    refreshListFilter();
                }));

        // ---- HUD 设置页控件 ----
        createHudSettingsControls();

        layoutPanels();
        updateWidgetVisibility();
    }

    private void createHudSettingsControls() {
        int px = 24;
        int w = 350;
        int y = 58;
        ModConfig cfg = ModConfig.INSTANCE;
        Minecraft mc = Minecraft.getInstance();

        hudSectionDisplayY = y - 12;
        // 显示
        cbHud = reg(kvmCheckbox(px, y,
                Component.translatable("keymapviewer.hud.show"), cfg.hudEnabled, () -> {
                    cfg.hudEnabled = cbHud.selected();
                    cfg.save();
                }));
        y += 20;
        cbCategories = reg(kvmCheckbox(px, y,
                Component.translatable("keymapviewer.hud.showCategories"), cfg.showHudCategories, () -> {
                    cfg.showHudCategories = cbCategories.selected();
                    cfg.save();
                }));
        y += 34;

        // 位置预设 + 偏移（内联行：标签 + [-] [+]）
        hudSectionPositionY = y - 12;
        presetCycle = reg(Button.builder(
                        Component.translatable("keymapviewer.hud.position")
                                .append(": ")
                                .append(Component.translatable(cfg.hudPreset.labelKey)),
                        b -> {
                            ModConfig.HudPreset[] vals = ModConfig.HudPreset.values();
                            ModConfig.HudPreset next = vals[(cfg.hudPreset.ordinal() + 1) % vals.length];
                            cfg.hudPreset = next;
                            cfg.save();
                            refreshPresetCycleLabel();
                        }).bounds(px, y, w, 18).build());
        y += 20;
        int btnW = 22;
        int labW = 104;

        hudOffsetXRow = y;
        bOffsetXMinus = minusPlus(px, y, labW, btnW, true, () -> {
            cfg.hudOffsetX = clampInt(cfg.hudOffsetX - 5);
            cfg.save();
        });
        bOffsetXPlus = minusPlus(px, y, labW, btnW, false, () -> {
            cfg.hudOffsetX = clampInt(cfg.hudOffsetX + 5);
            cfg.save();
        });
        y += 18;
        hudOffsetYRow = y;
        bOffsetYMinus = minusPlus(px, y, labW, btnW, true, () -> {
            cfg.hudOffsetY = clampInt(cfg.hudOffsetY - 5);
            cfg.save();
        });
        bOffsetYPlus = minusPlus(px, y, labW, btnW, false, () -> {
            cfg.hudOffsetY = clampInt(cfg.hudOffsetY + 5);
            cfg.save();
        });
        y += 18;
        bOffsetReset = reg(Button.builder(Component.translatable("keymapviewer.hud.offsetReset"), b -> {
            cfg.hudOffsetX = 0;
            cfg.hudOffsetY = 0;
            cfg.save();
        }).bounds(px, y, w, 18).build());
        y += 32;

        // 文字 / 背景透明度 + 缩放（内联 [-][+]）
        hudSectionOpacityY = y - 12;
        hudTextOpacityRow = y;
        bTextMinus = minusPlus(px, y, labW, btnW, true, () -> {
            cfg.hudTextOpacity = snapOp(cfg.hudTextOpacity - 0.05f);
            cfg.save();
        });
        bTextPlus = minusPlus(px, y, labW, btnW, false, () -> {
            cfg.hudTextOpacity = snapOp(cfg.hudTextOpacity + 0.05f);
            cfg.save();
        });
        y += 20;
        hudBgOpacityRow = y;
        bBgMinus = minusPlus(px, y, labW, btnW, true, () -> {
            cfg.hudBgOpacity = snapOp(cfg.hudBgOpacity - 0.05f);
            cfg.save();
        });
        bBgPlus = minusPlus(px, y, labW, btnW, false, () -> {
            cfg.hudBgOpacity = snapOp(cfg.hudBgOpacity + 0.05f);
            cfg.save();
        });
        y += 20;
        hudScaleRow = y;
        bScaleMinus = minusPlus(px, y, labW, btnW, true, () -> {
            cfg.hudScale = snap(cfg.hudScale - 0.1f);
            cfg.save();
        });
        bScalePlus = minusPlus(px, y, labW, btnW, false, () -> {
            cfg.hudScale = snap(cfg.hudScale + 0.1f);
            cfg.save();
        });
        y += 24;
        int halfW2 = (w - 4) / 2;
        bExport = reg(Button.builder(Component.translatable("keymapviewer.export"), b -> onExport())
                .bounds(px, y, halfW2, 18).build());
        bImport = reg(Button.builder(Component.translatable("keymapviewer.import"), b -> onImport())
                .bounds(px + halfW2 + 4, y, halfW2, 18).build());
        y += 22;
        hudNoticeRow = y;
    }

    private void onExport() {
        try {
            java.nio.file.Path f = com.keymapviewer.config.BindingsIo.writeToFile(KeyBindingManager.INSTANCE.allBindings());
            showHudNotice(com.keymapviewer.util.Ui.loc.tf("keymapviewer.export.done", f));
        } catch (Throwable t) {
            showHudNotice(t.toString());
        }
    }

        private void onOpenIO() {
        openInExplorer(com.keymapviewer.config.BindingsIo.latestExport().orElse(com.keymapviewer.config.BindingsIo.exportDir()));
    }

    private static void openInExplorer(java.nio.file.Path p) {
        try {
            String path = java.nio.file.Files.exists(p) ? p.toAbsolutePath().toString() : p.toAbsolutePath().getParent().toString();
            // explorer 的 /select 参数整体带引号由 cmd 解析，才能精确定位文件（否则会打开“文档”）
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", "explorer", "/select," + "\"" + path + "\""});
        } catch (Throwable ignored) {
        }
    }
private void onImport() {
        try {
            List<com.keymapviewer.config.BindingsIo.ExportEntry> entries =
                    com.keymapviewer.config.BindingsIo.readFromFile();
            int applied = 0;
            int skipped = 0;
            for (com.keymapviewer.config.BindingsIo.ExportEntry entry : entries) {
                com.keymapviewer.hotkey.KeyBindingInfo found = null;
                for (com.keymapviewer.hotkey.KeyBindingInfo info : KeyBindingManager.INSTANCE.allBindings()) {
                    if (info.source.name().equals(entry.source())
                            && info.modName.equals(entry.mod())
                            && info.category.equals(entry.category())
                            && info.actionName.equals(entry.action())) {
                        found = info;
                        break;
                    }
                }
                if (found != null && found.handle != null && found.handle.bindTarget() != null) {
                    if (java.util.Arrays.equals(entry.codes(), found.keyCodes)) {
                        skipped++;
                    } else {
                        List<com.keymapviewer.hotkey.KeySpec> specs = new ArrayList<>();
                        for (int c : entry.codes()) {
                            if (c <= -93 && c >= -100) {
                                // 规范鼠标码 -100..-93 → KeySpec 鼠标序号 0..7
                                specs.add(com.keymapviewer.hotkey.KeySpec.mouse(c + 100));
                            } else if (c >= 0) {
                                specs.add(com.keymapviewer.hotkey.KeySpec.keyboard(c));
                            }
                        }
                        found.handle.bindTarget().rebind(specs.toArray(new com.keymapviewer.hotkey.KeySpec[0]));
                        applied++;
                    }
                } else {
                    skipped++;
                }
            }
            KeyBindingManager.INSTANCE.notifyBindingChanged();
            showHudNotice(com.keymapviewer.util.Ui.loc.tf("keymapviewer.import.done", applied, skipped));
        } catch (Throwable t) {
            showHudNotice(com.keymapviewer.util.Ui.loc.t("keymapviewer.import.missing"));
        }
    }

    private void showHudNotice(String msg) {
        hudNotice = msg;
        hudNoticeUntil = com.keymapviewer.util.Ui.millis + 4000;
    }

    /** 行内 [-] 或 [+] 按钮：位于标签 (labW) 右侧。 */
    private Button minusPlus(int px, int y, int labW, int btnW, boolean minus, Runnable onPress) {
        int gap = 4;
        int x = px + labW + (minus ? 0 : btnW + gap);
        return reg(Button.builder(Component.literal(minus ? "-" : "+"), b -> onPress.run())
                .bounds(x, y, btnW, 18).build());
    }
    private void refreshPresetCycleLabel() {
        if (presetCycle != null) {
            ModConfig cfg = ModConfig.INSTANCE;
            presetCycle.setMessage(Component.translatable("keymapviewer.hud.position")
                    .append(": ").append(Component.translatable(cfg.hudPreset.labelKey)));
        }
    }


    /** 1.20.1 用构造器创建带回调的勾选框（该版本没有 builder.onValueChange）。 */
    private KvmCheckbox kvmCheckbox(int x, int y, Component label, boolean selected, Runnable onChange) {
        int w = Minecraft.getInstance().font.width(label.getString()) + 28;
        return new KvmCheckbox(x, y, w, 20, label, selected, onChange);
    }

    private static final class KvmCheckbox extends Checkbox {
        private final Runnable onChange;

        KvmCheckbox(int x, int y, int w, int h, Component message, boolean selected, Runnable onChange) {
            super(x, y, w, h, message, selected);
            this.onChange = onChange;
        }

        @Override
        public void onPress() {
            super.onPress();
            onChange.run();
        }
    }

    private int hudSectionDisplayY;
    private int hudSectionPositionY;
    private int hudOffsetXRow;
    private int hudOffsetYRow;
    private int hudSectionOpacityY;
    private int hudTextOpacityRow;
    private int hudBgOpacityRow;
    private int hudScaleRow;

    private static int clampInt(int v) {
        return Math.max(-500, Math.min(500, v));
    }

    // ---------------------------------------------------------------
    // 布局/过滤
    // ---------------------------------------------------------------

    private void layoutPanels() {
        if (activeTab == Tab.KEYBOARD) {
            keyboardPanel.setArea(10, 50, this.width - 20, this.height - 62);
        } else {
            keyboardPanel.setArea(10, 50, this.width - 20, this.height - 62);
            listPanel.setArea(10, 72, this.width - 20, this.height - 82);
        }
        refreshListFilter();
    }

    private void updateWidgetVisibility() {
        boolean list = activeTab == Tab.LIST;
        if (searchBox != null) {
            searchBox.visible = list;
            if (list) {
                // 列表页内搜索框常驻聚焦，保证可直接输入（捕获期间按键由改键流程接管）
                searchBox.setFocused(true);
            } else {
                searchBox.setFocused(false);
            }
        }
        if (cbVanilla != null) {
            cbVanilla.visible = list;
            cbUnbound.visible = list;
        }
        boolean hud = activeTab == Tab.HUD_SETTINGS;
        if (cbHud != null) {
            cbHud.visible = hud;
            cbCategories.visible = hud;
            presetCycle.visible = hud;
            if (hud) {
                refreshPresetCycleLabel();
            }
            bOffsetXMinus.visible = hud;
            bOffsetXPlus.visible = hud;
            bOffsetYMinus.visible = hud;
            bOffsetYPlus.visible = hud;
            bOffsetReset.visible = hud;
            bTextMinus.visible = hud;
            bTextPlus.visible = hud;
            bBgMinus.visible = hud;
            bBgPlus.visible = hud;
            bScaleMinus.visible = hud;
            bScalePlus.visible = hud;
            bExport.visible = hud;
            bImport.visible = hud;

        }
    }

    private void refreshListFilter() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<KeyBindingInfo> matched = new ArrayList<>();
        for (KeyBindingInfo info : KeyBindingManager.INSTANCE.filtered(ModConfig.INSTANCE)) {
            if (query.isEmpty() || matches(info, query)) {
                matched.add(info);
            }
        }
        listPanel.setFiltered(matched);
    }

    private static boolean matches(KeyBindingInfo info, String q) {
        return info.modName.toLowerCase(Locale.ROOT).contains(q)
                || info.category.toLowerCase(Locale.ROOT).contains(q)
                || info.actionName.toLowerCase(Locale.ROOT).contains(q)
                || info.displayKey.toLowerCase(Locale.ROOT).contains(q);
    }

    private void switchTab(Tab tab) {
        if (activeTab == tab) {
            return;
        }
        activeTab = tab;
        layoutPanels();
        updateWidgetVisibility();
        if (tab == Tab.LIST) {
            refreshListFilter();
        }
        if (tab == Tab.HUD_SETTINGS) {
        }
    }

    /** 开发自测用。 */
    public void showKeyboardView() {
        if (keyboardPanel != null) {
            switchTab(Tab.KEYBOARD);
        }
    }

    // ---------------------------------------------------------------
    // 绘制
    // ---------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        int w = this.width;
        int h = this.height;

        // common 面板/本地化宿主每帧更新
        com.keymapviewer.util.Ui.scaledWidth = w;
        com.keymapviewer.util.Ui.scaledHeight = h;
        com.keymapviewer.util.Ui.millis = System.currentTimeMillis();

        graphics.fill(0, 0, w, 22, 0xB0101A16);
        graphics.fill(0, 22, w, h, 0x8A0E1013);
        graphics.drawString(font, Component.translatable("keymapviewer.screen.title").getString(), 10, 6, Colors.TEXT_YELLOW);
        graphics.drawString(font, Component.translatable("keymapviewer.screen.subtitle").getString(),
                10 + font.width(Component.translatable("keymapviewer.screen.title").getString()) + 24, 6, Colors.TEXT_DIM);

        updateWidgetVisibility();

        // 控件
        for (Renderable r : myWidgets) {
            r.render(graphics, mouseX, mouseY, delta);
        }

        Canvas canvas = new CanvasGfx(graphics);
        if (activeTab == Tab.LIST) {
            graphics.fill(10, 68, w - 10, h - 10, 0x66202226);
            listPanel.draw(canvas, mouseX, mouseY);
        } else if (activeTab == Tab.KEYBOARD) {
            graphics.fill(10, 46, w - 10, h - 10, 0x66202226);
            keyboardPanel.draw(canvas, mouseX, mouseY);
        } else {
            // HUD 设置页：无内嵌预览容器，直接观察游戏内 HUD（HudElement 会随改动实时移动/变化）
            int pw = 360;
            graphics.fill(10, 46, 10 + pw, h - 10, 0x6620281F);
            drawHudSettings(graphics, font, mouseX, mouseY);
        if (hudNotice != null && com.keymapviewer.util.Ui.millis < hudNoticeUntil) {
            graphics.drawString(font, hudNotice, 24, hudNoticeRow, Colors.TEXT_YELLOW);
        } else {
            hudNotice = null;
        }
            graphics.drawString(font, com.keymapviewer.util.Lang.t("keymapviewer.hud.liveHint"),
                    width - 200, h - 12, Colors.TEXT_GRAY);
        }

        // 数据代际变化 → 重新过滤
        if (KeyBindingManager.INSTANCE.generation() != lastGeneration) {
            lastGeneration = KeyBindingManager.INSTANCE.generation();
            refreshListFilter();
        }

        String err = KeyBindingManager.INSTANCE.adapterError();
        if (err != null && !err.isEmpty()) {
            graphics.fill(10, 0, w - 10, 22, 0xC04A2828);
            graphics.drawString(font, Lang.tf("keymapviewer.malilib.incompatible", err), 14, 6, Colors.TEXT_RED);
        }
    }

    private final String[] hudSectionTitles() {
        return new String[]{
                Lang.t("keymapviewer.hud.section.display"),
                Lang.t("keymapviewer.hud.section.position"),
                Lang.t("keymapviewer.hud.section.opacity")
        };
    }

    private void drawHudSettings(GuiGraphics graphics, Font font, int mx, int my) {
        int px = 24;
        ModConfig cfg = ModConfig.INSTANCE;
        int btnW = 22;
        int gap = 4;
        int valueX = px + 104 + btnW * 2 + gap + 10;
        String[] titles = hudSectionTitles();


        section(graphics, px, hudSectionDisplayY, titles[0]);
        section(graphics, px, hudSectionPositionY, titles[1]);
        section(graphics, px, hudSectionOpacityY, titles[2]);

        // 内联标签 + 值（值在行末，随预设/偏移即时刷新）
        graphics.drawString(font, Lang.t("keymapviewer.hud.offsetX"), px, hudOffsetXRow + 4, Colors.TEXT_WHITE);
        graphics.drawString(font, String.valueOf(cfg.hudOffsetX), valueX, hudOffsetXRow + 4, Colors.TEXT_YELLOW);
        graphics.drawString(font, Lang.t("keymapviewer.hud.offsetY"), px, hudOffsetYRow + 4, Colors.TEXT_WHITE);
        graphics.drawString(font, String.valueOf(cfg.hudOffsetY), valueX, hudOffsetYRow + 4, Colors.TEXT_YELLOW);

        graphics.drawString(font, Lang.t("keymapviewer.hud.textOpacity"), px, hudTextOpacityRow + 4, Colors.TEXT_WHITE);
        graphics.drawString(font, pct(cfg.hudTextOpacity), valueX, hudTextOpacityRow + 4, Colors.TEXT_YELLOW);
        graphics.drawString(font, Lang.t("keymapviewer.hud.bgOpacity"), px, hudBgOpacityRow + 4, Colors.TEXT_WHITE);
        graphics.drawString(font, pct(cfg.hudBgOpacity), valueX, hudBgOpacityRow + 4, Colors.TEXT_YELLOW);
        graphics.drawString(font, Lang.t("keymapviewer.hud.scale"), px, hudScaleRow + 4, Colors.TEXT_WHITE);
        graphics.drawString(font, pct(cfg.hudScale), valueX, hudScaleRow + 4, Colors.TEXT_YELLOW);
        graphics.drawString(font, com.keymapviewer.util.Ui.loc.t("keymapviewer.hud.toggle.hint"), 24, hudNoticeRow + 16, Colors.TEXT_DIM);
        graphics.drawString(font, com.keymapviewer.util.Ui.loc.tf("keymapviewer.io.export", com.keymapviewer.config.BindingsIo.exportDir()), 24, hudNoticeRow + 25, Colors.TEXT_DIM);
        graphics.drawString(font, com.keymapviewer.util.Ui.loc.tf("keymapviewer.io.import", com.keymapviewer.config.BindingsIo.importDir()), 24, hudNoticeRow + 34, Colors.TEXT_DIM);
    }

    private static String pct(float v) {
        return String.format(Locale.ROOT, "%.0f%%", v * 100f);
    }

    private void section(GuiGraphics graphics, int x, int y, String title) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, "■ " + title, x, y, Colors.TEXT_YELLOW);
    }

    // ---------------------------------------------------------------
    // 输入
    // ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // 改键捕捉：点击任意处 = 提交草稿（先于控件，避免控件/搜索受影响）
        if (KeyCapture.INSTANCE.isActive()) {
            KeyCapture.INSTANCE.commit();
            return true;
        }

        for (GuiEventListener wGui : myListeners) {
            if (wGui instanceof net.minecraft.client.gui.components.AbstractWidget aw && !aw.visible) {
                continue;
            }
            if (wGui.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (activeTab == Tab.LIST) {
            return listPanel.mouseClicked(mx, my, button, 0);
        }
        if (activeTab == Tab.KEYBOARD) {
            return keyboardPanel.mouseClicked(mx, my, button);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (activeTab == Tab.LIST) {
            // 1.20.1 无水平滚动轴，滚轮当作垂直滚动
            return listPanel.mouseScrolled(0, amount);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeTab == Tab.LIST) {
            return listPanel.mouseDragged((int) mouseX, (int) mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (listPanel != null) {
            listPanel.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int code = keyCode;
        if (KeyCapture.INSTANCE.isActive()) {
            if (code == GLFW.GLFW_KEY_ESCAPE) {
                KeyCapture.INSTANCE.cancel();
                return true;
            }
            return listPanel.keyPressed(code);
        }
        for (GuiEventListener wGui : myListeners) {
            if (wGui instanceof net.minecraft.client.gui.components.AbstractWidget aw && !aw.visible) {
                continue;
            }
            if (wGui.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (code == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (KeyCapture.INSTANCE.isActive()) {
            return true;
        }
        for (GuiEventListener wGui : myListeners) {
            if (wGui instanceof net.minecraft.client.gui.components.AbstractWidget aw && !aw.visible) {
                continue;
            }
            if (wGui.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        if (keyboardPanel != null) {
            keyboardPanel.onClose();
        }
        if (listPanel != null) {
            listPanel.onClose();
        }
        com.keymapviewer.util.Ui.settingsOpen = false;
        ModConfig.INSTANCE.save();
        KeyBindingManager.INSTANCE.notifyBindingChanged();
        super.onClose();
    }

    private static float snap(float v) {
        return Math.max(0.5f, Math.min(3.0f, v));
    }

    private static float snapOp(float v) {
        return Math.max(0.1f, Math.min(1.0f, v));
    }
}