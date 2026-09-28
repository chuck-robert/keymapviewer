# -*- coding: utf-8 -*-
"""一次性补丁：版本 1.0.2、语言键、键盘滚轮路由、导出/导入按钮、提示横幅。"""
import glob, json

VERSION_NEW = '1.0.2'

# 1) 版本号
for p in glob.glob('versions/*/gradle.properties') + ['gradle.properties']:
    s = open(p, encoding='utf-8').read()
    if 'mod_version=1.0.0' in s:
        s = s.replace('mod_version=1.0.0', 'mod_version=%s' % VERSION_NEW)
        open(p, 'w', encoding='utf-8').write(s)
        print('version ->', p)

# 2) 语言键
extra_zh = {
    'keymapviewer.view.hud': '模组设置',
    'keymapviewer.export': '导出所有快捷键',
    'keymapviewer.import': '导入快捷键设置',
    'keymapviewer.export.done': '已导出到 {0}',
    'keymapviewer.import.done': '已导入 {0} 项（未匹配 {1} 项）',
    'keymapviewer.import.missing': '导入失败：未找到快捷键备份文件',
    'keymapviewer.hud.toggle.hint': 'K+C 快速切换 HUD 显隐',
}
extra_en = {
    'keymapviewer.view.hud': 'Mod Settings',
    'keymapviewer.export': 'Export all keybinds',
    'keymapviewer.import': 'Import keybinds',
    'keymapviewer.export.done': 'Exported to {0}',
    'keymapviewer.import.done': 'Imported {0} ({1} unmatched)',
    'keymapviewer.import.missing': 'Import failed: keybind backup file not found',
    'keymapviewer.hud.toggle.hint': 'K+C toggles the HUD',
}
for lang in glob.glob('versions/*/src/main/resources/assets/keymapviewer/lang/*.json'):
    d = json.load(open(lang, encoding='utf-8'))
    d.update(extra_zh if lang.endswith('zh_cn.json') else extra_en)
    json.dump(d, open(lang, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    print('lang ->', lang)

# 3) 设置屏
SCROLL_OLD = '''    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (activeTab == Tab.LIST) {
            return listPanel.mouseScrolled(horizontal, vertical);
        }
        return false;
    }'''
SCROLL_NEW = '''    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (activeTab == Tab.LIST) {
            return listPanel.mouseScrolled(horizontal, vertical);
        }
        if (activeTab == Tab.KEYBOARD) {
            return keyboardPanel.mouseScrolled(vertical);
        }
        return false;
    }'''

FIELDS_OLD = '''    private Button bScaleMinus;
    private Button bScalePlus;'''
FIELDS_NEW = '''    private Button bScaleMinus;
    private Button bScalePlus;
    private Button bExport;
    private Button bImport;
    private String hudNotice;
    private long hudNoticeUntil;
    private int hudNoticeRow;'''

BTN_OLD = '''        bScalePlus = minusPlus(px, y, labW, btnW, false, () -> {
            cfg.hudScale = snap(cfg.hudScale + 0.1f);
            cfg.save();
        });
    }

    /** 行内 [-] 或 [+] 按钮'''
BTN_NEW = '''        bScalePlus = minusPlus(px, y, labW, btnW, false, () -> {
            cfg.hudScale = snap(cfg.hudScale + 0.1f);
            cfg.save();
        });
        y += 24;
        int halfW = (w - 4) / 2;
        bExport = reg(Button.builder(Component.translatable("keymapviewer.export"), b -> onExport())
                .bounds(px, y, halfW, 18).build());
        bImport = reg(Button.builder(Component.translatable("keymapviewer.import"), b -> onImport())
                .bounds(px + halfW + 4, y, halfW, 18).build());
        y += 22;
        hudNoticeRow = y;
    }

    private void onExport() {
        try {
            com.keymapviewer.config.BindingsIo.writeToFile(KeyBindingManager.INSTANCE.allBindings());
            showHudNotice(com.keymapviewer.util.Ui.loc.tf("keymapviewer.export.done",
                    com.keymapviewer.config.BindingsIo.filePath()));
        } catch (Throwable t) {
            showHudNotice(t.toString());
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
                    List<com.keymapviewer.hotkey.KeySpec> specs = new ArrayList<>();
                    for (int c : entry.codes()) {
                        specs.add(com.keymapviewer.hotkey.KeySpec.keyboard(c));
                    }
                    found.handle.bindTarget().rebind(specs.toArray(new com.keymapviewer.hotkey.KeySpec[0]));
                    applied++;
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

    /** 行内 [-] 或 [+] 按钮'''

VIS_OLD = '''            bScaleMinus.visible = hud;
            bScalePlus.visible = hud;
        }
    }'''
VIS_NEW = '''            bScaleMinus.visible = hud;
            bScalePlus.visible = hud;
            bExport.visible = hud;
            bImport.visible = hud;
        }
    }'''

for p in glob.glob('versions/*/src/main/java/com/keymapviewer/ui/KeySettingsScreen.java'):
    s = open(p, encoding='utf-8').read()
    basecall = None
    if 'ext.text(font,' in s:
        basecall = 'ext.text(font,'
    elif 'graphics.drawString(font,' in s:
        basecall = 'graphics.drawString(font,'
    ok = True
    for old, new in [(SCROLL_OLD, SCROLL_NEW), (FIELDS_OLD, FIELDS_NEW), (BTN_OLD, BTN_NEW), (VIS_OLD, VIS_NEW)]:
        if old in s:
            s = s.replace(old, new)
        else:
            ok = False
            print('MISS anchor in', p, ':', old[:40])
    # 提示横幅（HUD_SETTINGS 分支半透明提示行之后绘制）
    notice = '\n        if (hudNotice != null && com.keymapviewer.util.Ui.millis < hudNoticeUntil) {\n            %s hudNotice, 24, hudNoticeRow, Colors.TEXT_YELLOW);\n        } else {\n            hudNotice = null;\n        }' % basecall
    if basecall:
        idx = s.find('drawHudSettings(ext, font, mouseX, mouseY)')
        if idx >= 0:
            idx = s.find(';', idx)  # 此行结束
            s = s[:idx + 1] + notice + s[idx + 1:]
        else:
            ok = False
            print('MISS drawHudSettings call in', p)
    open(p, 'w', encoding='utf-8').write(s)
    print('screen ->', p, 'ok' if ok else 'WARN')