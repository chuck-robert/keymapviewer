# -*- coding: utf-8 -*-
"""第 5/4 批补丁：Masa 菜单按钮、导入导出打开按钮、K+C 提示、语言键。"""
import glob, json, os

# 1) 各模块新增 MaLiLibMenu
menu_java = '''package com.keymapviewer.hotkey.malilib;

/** 打开 MaLiLib 的「模组列表」根屏（Masa 右上角菜单的入口）。 */
public final class MaLiLibMenu {
    private MaLiLibMenu() {
    }

    public static boolean openRoot() {
        try {
            fi.dy.masa.malilib.config.gui.GuiModConfigs gui = new fi.dy.masa.malilib.config.gui.GuiModConfigs();
            fi.dy.masa.malilib.gui.GuiBase.openGui(gui);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
'''
for mod in os.listdir('versions'):
    p = os.path.join('versions', mod, 'src/main/java/com/keymapviewer/hotkey/malilib/MaLiLibMenu.java')
    if os.path.isdir(os.path.dirname(p)):
        if not os.path.exists(p):
            open(p, 'w', encoding='utf-8').write(menu_java)
            print('menu ->', p)

# 2) 语言键
extra_zh = {
    'keymapviewer.openMasaMenu': '◁ Masa 菜单',
    'keymapviewer.openIo': '打开备份文件',
}
extra_en = {
    'keymapviewer.openMasaMenu': '< Masa Menu',
    'keymapviewer.openIo': 'Open backup',
}
for lang in glob.glob('versions/*/src/main/resources/assets/keymapviewer/lang/*.json'):
    d = json.load(open(lang, encoding='utf-8'))
    d.update(extra_zh if lang.endswith('zh_cn.json') else extra_en)
    json.dump(d, open(lang, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    print('lang ->', lang)

# 3) 屏幕补丁
FIELDS_OLD = '''    private Button bExport;
    private Button bImport;'''
FIELDS_NEW = '''    private Button bExport;
    private Button bImport;
    private Button bOpenIO;
    private Button bMasaMenu;'''

BTN_OLD = '''        y += 24;
        int halfW = (w - 4) / 2;
        bExport = reg(Button.builder(Component.translatable("keymapviewer.export"), b -> onExport())
                .bounds(px, y, halfW, 18).build());
        bImport = reg(Button.builder(Component.translatable("keymapviewer.import"), b -> onImport())
                .bounds(px + halfW + 4, y, halfW, 18).build());
        y += 22;
        hudNoticeRow = y;'''
BTN_NEW = '''        y += 24;
        int third = (w - 8) / 3;
        bExport = reg(Button.builder(Component.translatable("keymapviewer.export"), b -> onExport())
                .bounds(px, y, third, 18).build());
        bImport = reg(Button.builder(Component.translatable("keymapviewer.import"), b -> onImport())
                .bounds(px + third + 4, y, third, 18).build());
        bOpenIO = reg(Button.builder(Component.translatable("keymapviewer.openIo"), b -> onOpenIO())
                .bounds(px + 2 * (third + 4), y, third, 18).build());
        y += 22;
        hudNoticeRow = y;'''

OPENIO_METHOD = '''    private void onOpenIO() {
        openInExplorer(com.keymapviewer.config.BindingsIo.filePath());
    }

    private static void openInExplorer(java.nio.file.Path p) {
        try {
            Runtime.getRuntime().exec(new String[]{"explorer", "/select," + p});
        } catch (Throwable ignored) {
        }
    }

'''
VIS_OLD = '''            bExport.visible = hud;
            bImport.visible = hud;'''
VIS_NEW = '''            bExport.visible = hud;
            bImport.visible = hud;
            bOpenIO.visible = hud;'''

for p in glob.glob('versions/*/src/main/java/com/keymapviewer/ui/KeySettingsScreen.java'):
    s = open(p, encoding='utf-8').read()
    ok = True
    # 字段
    if FIELDS_OLD in s:
        s = s.replace(FIELDS_OLD, FIELDS_NEW)
    else:
        ok = False
        print('MISS fields in', p)
    # 三按钮行 + onOpenIO 方法（在 onImport 方法后插入）
    if BTN_OLD in s:
        s = s.replace(BTN_OLD, BTN_NEW)
    else:
        ok = False
        print('MISS btn in', p)
    if 'private void onOpenIO()' not in s:
        s = s.replace('private void onImport() {', OPENIO_METHOD + 'private void onImport() {', 1)
    # 可见性
    if VIS_OLD in s:
        s = s.replace(VIS_OLD, VIS_NEW)
    else:
        ok = False
        print('MISS vis in', p)
    # Masa 菜单按钮（顶部），放在三个 tab 按钮创建后
    tabbtn = '''        reg(Button.builder(Component.translatable("keymapviewer.view.hud"), b -> switchTab(Tab.HUD_SETTINGS))
                .bounds(308, 25, 140, 18).build());'''
    if 'bMasaMenu = reg(Button.builder' not in s:
        masa = tabbtn + '''
        bMasaMenu = reg(Button.builder(Component.translatable("keymapviewer.openMasaMenu"), b ->
                com.keymapviewer.hotkey.malilib.MaLiLibMenu.openRoot())
                .bounds(this.width - 128, 3, 120, 16).build());
        bMasaMenu.visible = com.keymapviewer.hotkey.malilib.MaLiLibCompat.isLoaded();'''
        if tabbtn in s:
            s = s.replace(tabbtn, masa)
        else:
            ok = False
            print('MISS tabbtn in', p)
    # K+C 静态提示（HUD_SETTINGS 分支，颜色说明后不依赖；直接放 drawHudSettings 末尾）
    if 'keymapviewer.hud.toggle.hint' in s:
        pass
    else:
        # 在 onImport 独占 — 将提示画进 hudNotice 区域下方；用每模块的文本调用
        if 'ext.text(font,' in s:
            tc = 'ext.text(font,'
        elif 'graphics.drawString(font,' in s:
            tc = 'graphics.drawString(font,'
        else:
            tc = None
        if tc:
            hint = '''        %s com.keymapviewer.util.Ui.loc.t("keymapviewer.hud.toggle.hint"), 24, hudNoticeRow + 16, Colors.TEXT_DIM);''' % tc
            s = s.replace('        showHudNotice(msg);\n    }', '        showHudNotice(msg);\n    }\n' + hint, 1)
        else:
            ok = False
            print('MISS tc in', p)
    open(p, 'w', encoding='utf-8').write(s)
    print('screen ->', p, 'ok' if ok else 'WARN')