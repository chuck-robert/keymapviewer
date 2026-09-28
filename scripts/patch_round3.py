# -*- coding: utf-8 -*-
"""round3：空格键显示/Mouse组合、滚动重写、%s格式化、导入去重、explorer、F12、去掉K+C守卫。"""
import glob, json, re

# ---------- 1) KeyNames.pretty 空格键（5 模块副本） ----------
SPACE_OLD = '''        if (code >= 32 && code < 128) {
            return String.valueOf((char) code);
        }
        try {'''
SPACE_NEW = '''        if (code == GLFW.GLFW_KEY_SPACE) {
            // 空格键显示为名字而非空格字符，避免在列表中看不出内容
            try {
                String mc = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
                if (mc != null && !mc.isEmpty() && !mc.equals("Unknown")) {
                    return mc;
                }
            } catch (Throwable t) {
                // ignore
            }
            return "Space";
        }
        if (code > 32 && code < 128) {
            return String.valueOf((char) code);
        }
        try {'''
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/util/KeyNames.java'):
    s = open(p, encoding='utf-8').read()
    if SPACE_OLD in s:
        s = s.replace(SPACE_OLD, SPACE_NEW, 1)
        open(p, 'w', encoding='utf-8').write(s)
        print('keynames ->', p)
    else:
        print('keynames NO-MATCH ->', p)

# ---------- 2) 语言键：{0}→%s/%d、F12 提示、鼠标键名 ----------
def patch_lang(jsonp, z, e):
    d = json.load(open(jsonp, encoding='utf-8'))
    d.update(z if jsonp.endswith('zh_cn.json') else e)
    json.dump(d, open(jsonp, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

for lang in glob.glob('versions/*/src/main/resources/assets/keymapviewer/lang/*.json'):
    patch_lang(lang,
        {
            'keymapviewer.export.done': '已导出到 %s',
            'keymapviewer.import.done': '已导入 %d 项（未匹配/未变更 %d 项）',
            'keymapviewer.keyboard.tooltip.key': '键位 %s',
            'keymapviewer.keyboard.tooltip.bindings': ' · %d 个绑定',
            'keymapviewer.hud.toggle.hint': 'F12 切换 HUD 显隐',
            'keymapviewer.mouse.1': '左键', 'keymapviewer.mouse.2': '右键',
            'keymapviewer.mouse.3': '中键',
            'keymapviewer.mouse.4': '侧键4', 'keymapviewer.mouse.5': '侧键5',
            'keymapviewer.mouse.6': '侧键6', 'keymapviewer.mouse.7': '侧键7',
            'keymapviewer.mouse.8': '侧键8',
        },
        {
            'keymapviewer.export.done': 'Exported to %s',
            'keymapviewer.import.done': 'Imported %d (%d unmatched/unchanged)',
            'keymapviewer.keyboard.tooltip.key': 'Key %s',
            'keymapviewer.keyboard.tooltip.bindings': ' · %d binding(s)',
            'keymapviewer.hud.toggle.hint': 'F12 toggles the HUD',
            'keymapviewer.mouse.1': 'Left Click', 'keymapviewer.mouse.2': 'Right Click',
            'keymapviewer.mouse.3': 'Middle Click',
            'keymapviewer.mouse.4': 'Btn4', 'keymapviewer.mouse.5': 'Btn5',
            'keymapviewer.mouse.6': 'Btn6', 'keymapviewer.mouse.7': 'Btn7',
            'keymapviewer.mouse.8': 'Btn8',
        })
print('lang patched')

# ---------- 3) common KeyBindingManager：K+C 和弦 → F12 单键边沿 ----------
p = 'common/src/main/java/com/keymapviewer/hotkey/KeyBindingManager.java'
s = open(p, encoding='utf-8').read()
old = '''    private boolean chordActive;

    /** K+C 组合（两键任意顺序同时按下）切换 HUD 显隐；设置界面打开时忽略。 */
    private void checkHudToggleCombo() {
        boolean k = sampler != null && sampler.isKeyDown(GLFW.GLFW_KEY_K);
        boolean c = sampler != null && sampler.isKeyDown(GLFW.GLFW_KEY_C);
        if (k && c) {
            if (!chordActive && !Ui.settingsOpen) {
                ModConfig cfg = ModConfig.INSTANCE;
                cfg.hudEnabled = !cfg.hudEnabled;
                cfg.save();
            }
            chordActive = true;
        } else {
            chordActive = false;
        }
    }'''
new = '''    private boolean prevF12;

    /** F12（单键按下沿）切换 HUD 显隐；设置界面打开时忽略。 */
    private void checkHudToggleCombo() {
        if (Ui.settingsOpen) {
            prevF12 = false;
            return;
        }
        boolean f12 = sampler != null && sampler.isKeyDown(GLFW.GLFW_KEY_F12);
        if (f12 && !prevF12) {
            ModConfig cfg = ModConfig.INSTANCE;
            cfg.hudEnabled = !cfg.hudEnabled;
            cfg.save();
        }
        prevF12 = f12;
    }'''
if old in s:
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8').write(s)
    print('manager F12 ok')
else:
    print('manager NO-MATCH')

# ---------- 4) 客户端去掉 K+C 守卫 ----------
GUARD_OLD = '''                // K+C 组合（按住 C 时按 K）：交给 KeyBindingManager 切换 HUD，不开关菜单
                if (com.keymapviewer.hotkey.KeyBindingManager.INSTANCE.isKeyHeld(org.lwjgl.glfw.GLFW.GLFW_KEY_C)) {
                    continue;
                }
'''
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/KeyMapViewerClient.java'):
    s = open(p, encoding='utf-8').read()
    if GUARD_OLD in s:
        s = s.replace(GUARD_OLD, '', 1)
        open(p, 'w', encoding='utf-8').write(s)
        print('guard removed ->', p)
    else:
        print('guard NO-MATCH ->', p)

# ---------- 5) MaLiLib 显示包含鼠标键（5 模块 provider） ----------
PROV_OLD = '''        String display = "";
        try {
            if (keybind.isValid()) {
                if (!keyboardKeys.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < keyboardKeys.size(); i++) {
                        if (i > 0) {
                            sb.append(" + ");
                        }
                        sb.append(KeyNames.pretty(keyboardKeys.get(i)));
                    }
                    display = sb.toString();
                } else {'''
PROV_NEW = '''        String display = "";
        try {
            if (keybind.isValid()) {
                java.util.List<String> parts = new java.util.ArrayList<>();
                boolean anyNamed = false;
                if (keys != null) {
                    for (Integer code : keys) {
                        String name = displayNameOf(code);
                        if (name != null) {
                            parts.add(name);
                            anyNamed = true;
                        }
                    }
                }
                if (anyNamed) {
                    display = String.join(" + ", parts);
                } else {'''
if PROV_OLD in open(__import__('random').choice(glob.glob('versions/*/src/main/java/com/keymapviewer/hotkey/malilib/MaLiLibKeyProvider.java')), encoding='utf-8').read():
    for p in glob.glob('versions/*/src/main/java/com/keymapviewer/hotkey/malilib/MaLiLibKeyProvider.java'):
        s = open(p, encoding='utf-8').read()
        if PROV_OLD in s:
            s = s.replace(PROV_OLD, PROV_NEW, 1)
            # 追加 helper（放在 collectHotkey 之后、类结束前）
            helper = '''
    private static String displayNameOf(int code) {
        if (code >= 32 && code <= 348) {
            return KeyNames.pretty(code);
        }
        Integer btn = MOUSE_ORD.get(code);
        if (btn != null) {
            return com.keymapviewer.util.Ui.loc.t("keymapviewer.mouse." + (btn + 1));
        }
        return null;
    }

    private static final java.util.Map<Integer, Integer> MOUSE_ORD = mouseMap();

    private static java.util.Map<Integer, Integer> mouseMap() {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        try {
            for (int i = 1; i <= 8; i++) {
                Object v = fi.dy.masa.malilib.util.KeyCodes.class.getField("MOUSE_BUTTON_" + i).get(null);
                map.put(((Number) v).intValue(), i - 1);
            }
        } catch (Throwable t) {
            // 老版本可能没有这些常量，忽略
        }
        return map;
    }
}'''
            # 把原类结尾的最后一个 } 替换为 helper（helper 自带收尾 }）
            rindex = s.rstrip().rfind('}')
            s = s[:rindex] + helper
            open(p, 'w', encoding='utf-8').write(s)
            print('provider patched ->', p)
else:
    print('provider NO-MATCH (probe)')

# ---------- 6) 悬浮框滚动重写（common） ----------
p = 'common/src/main/java/com/keymapviewer/ui/KeyboardViewPanel.java'
s = open(p, encoding='utf-8').read()
DRAW_OLD = '''        // 内容较多时限制可视行数，用滚轮浏览
        int maxRows = Math.max(1, (panelH - 60) / rowH);
        tooltipVisibleRows = Math.min(entries.size(), maxRows);
        int from = Math.max(0, Math.min(tooltipScroll, entries.size() - tooltipVisibleRows));'''
DRAW_NEW = '''        // 内容较多时限制可视行数，用滚轮浏览；滚动范围每帧收敛
        int maxRows = Math.max(1, (panelH - 60) / rowH);
        tooltipVisibleRows = Math.min(entries.size(), maxRows);
        int limit = Math.max(0, entries.size() - tooltipVisibleRows);
        tooltipScroll = Math.max(0, Math.min(tooltipScroll, limit));
        int from = tooltipScroll;'''
s = s.replace(DRAW_OLD, DRAW_NEW, 1)
SCROLL_OLD = '''    /** 悬浮提示框滚轮滚动；返回 true 表示已消费（有提示框打开）。 */
    public boolean mouseScrolled(double vertical) {
        if (hoverKey < 0) {
            return false;
        }
        if (vertical > 0) {
            tooltipScroll = Math.max(0, tooltipScroll - 1);
        } else if (vertical < 0) {
            int max = Math.max(0, tooltipVisibleRows - 1);
            tooltipScroll = Math.min(max + (1), tooltipScroll + 1);
            int limit = Math.max(0, KeyBindingManager.INSTANCE.bindingsForKey(hoverKey).size() - tooltipVisibleRows);
            tooltipScroll = Math.max(0, Math.min(tooltipScroll, limit));
        }
        return true;
    }'''
SCROLL_NEW = '''    /** 悬浮提示框滚轮滚动；返回 true 表示已消费（有提示框打开）。 */
    public boolean mouseScrolled(double vertical) {
        if (hoverKey < 0) {
            return false;
        }
        int total = KeyBindingManager.INSTANCE.bindingsForKey(hoverKey).size();
        int vis = Math.max(1, tooltipVisibleRows);
        int limit = Math.max(0, total - vis);
        if (vertical > 0) {
            tooltipScroll = Math.max(0, tooltipScroll - 1);
        } else if (vertical < 0) {
            tooltipScroll = Math.min(limit, tooltipScroll + 1);
        }
        return true;
    }'''
s = s.replace(SCROLL_OLD, SCROLL_NEW, 1)
open(p, 'w', encoding='utf-8').write(s)
print('tooltip scroll ok')

# ---------- 7) 导入跳过未变更（5 屏） ----------
IMP_OLD = '''                if (found != null && found.handle != null && found.handle.bindTarget() != null) {
                    List<com.keymapviewer.hotkey.KeySpec> specs = new ArrayList<>();
                    for (int c : entry.codes()) {
                        specs.add(com.keymapviewer.hotkey.KeySpec.keyboard(c));
                    }
                    found.handle.bindTarget().rebind(specs.toArray(new com.keymapviewer.hotkey.KeySpec[0]));
                    applied++;
                } else {
                    skipped++;
                }'''
IMP_NEW = '''                if (found != null && found.handle != null && found.handle.bindTarget() != null) {
                    if (java.util.Arrays.equals(entry.codes(), found.keyCodes)) {
                        skipped++;
                    } else {
                        List<com.keymapviewer.hotkey.KeySpec> specs = new ArrayList<>();
                        for (int c : entry.codes()) {
                            specs.add(com.keymapviewer.hotkey.KeySpec.keyboard(c));
                        }
                        found.handle.bindTarget().rebind(specs.toArray(new com.keymapviewer.hotkey.KeySpec[0]));
                        applied++;
                    }
                } else {
                    skipped++;
                }'''
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/ui/KeySettingsScreen.java'):
    s = open(p, encoding='utf-8').read()
    if IMP_OLD in s:
        s = s.replace(IMP_OLD, IMP_NEW, 1)
        open(p, 'w', encoding='utf-8').write(s)
        print('import skip ->', p)
    else:
        print('import NO-MATCH ->', p)

# ---------- 8) explorer 修复（5 屏） ----------
EXP_OLD = '''    private static void openInExplorer(java.nio.file.Path p) {
        try {
            Runtime.getRuntime().exec(new String[]{"explorer", "/select," + p});
        } catch (Throwable ignored) {
        }
    }'''
EXP_NEW = '''    private static void openInExplorer(java.nio.file.Path p) {
        try {
            String path = java.nio.file.Files.exists(p) ? p.toAbsolutePath().toString() : p.toAbsolutePath().getParent().toString();
            // explorer 的 /select 参数整体带引号由 cmd 解析，才能精确定位文件（否则会打开“文档”）
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", "explorer", "/select," + "\\"" + path + "\\""});
        } catch (Throwable ignored) {
        }
    }'''
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/ui/KeySettingsScreen.java'):
    s = open(p, encoding='utf-8').read()
    if EXP_OLD in s:
        s = s.replace(EXP_OLD, EXP_NEW, 1)
        open(p, 'w', encoding='utf-8').write(s)
        print('explorer ->', p)
    else:
        print('explorer NO-MATCH ->', p)
print('done')