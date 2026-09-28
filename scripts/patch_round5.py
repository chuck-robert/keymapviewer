# -*- coding: utf-8 -*-
"""round5：组合显示增强、F12 注册可见、去掉打开备份与自绘Masa按钮、桥接壳直开、路径小字。"""
import glob, json, os, re

# ---------- 1) provider 显示增强 ----------
DISP_OLD = '''        String display = "";
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
                    try {
                        String lib = keybind.getKeysDisplayString();
                        if (lib != null && lib.indexOf(',') >= 0) {
                            lib = lib.replaceAll(",", " + ");
                        }
                        if (lib != null) {
                            int libTokens = lib.isEmpty() ? 0 : lib.split(" \\\\+ ").length;
                            if (libTokens > parts.size()) {
                                // 库串包含本地无法命名的键（如 F3 调试引导键），采用库串保持与游戏内一致
                                display = lib;
                            }
                        }
                    } catch (Throwable t) {
                        // ignore
                    }
                } else {'''
DISP_NEW = '''        String display = "";
        try {
            if (keybind.isValid()) {
                java.util.List<String> parts = new java.util.ArrayList<>();
                boolean unknownKey = false;
                if (keys != null) {
                    for (Integer code : keys) {
                        String name = displayNameOf(code);
                        if (name != null) {
                            parts.add(name);
                        } else {
                            unknownKey = true;
                        }
                    }
                }
                String lib = null;
                try {
                    lib = keybind.getKeysDisplayString();
                    if (lib != null && lib.indexOf(',') >= 0) {
                        lib = lib.replaceAll(",", " + ");
                    }
                } catch (Throwable t) {
                    lib = null;
                }
                int libTokens = (lib == null || lib.isEmpty()) ? 0 : lib.split(" \\\\+ ").length;
                if (!parts.isEmpty() && !unknownKey && libTokens <= (keys == null ? 0 : keys.size())) {
                    // 全部键都可命名且库串不带更多信息：用本地化显示
                    display = String.join(" + ", parts);
                } else if (unknownKey || libTokens > (keys == null ? 0 : keys.size())) {
                    // 存在无法命名的键（鼠标/F3 调试引导等）：采用 MaLiLib 原始显示保持与游戏内一致
                    display = lib == null ? String.join(" + ", parts) : lib;
                } else {'''
if __name__ == '__main__':
    pass
# 规定换行缩进，见下
DISP_OLD2 = DISP_OLD
DISP_NEW2 = DISP_NEW
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/hotkey/malilib/MaLiLibKeyProvider.java'):
    s = open(p, encoding='utf-8').read()
    if DISP_OLD in s:
        s = s.replace(DISP_OLD, DISP_NEW, 1)
    else:
        print('disp NO-MATCH ->', p); continue
    # 增强 displayNameOf：找不到映射时尝试 MaLiLib 存储名解析 MOUSE_/BUTTON_n
    old_dn = '''        Integer btn = MOUSE_ORD.get(code);
        if (btn != null) {
            return com.keymapviewer.util.Ui.loc.t("keymapviewer.mouse." + (btn + 1));
        }
        return null;'''
    new_dn = '''        Integer btn = MOUSE_ORD.get(code);
        if (btn != null) {
            return com.keymapviewer.util.Ui.loc.t("keymapviewer.mouse." + (btn + 1));
        }
        try {
            String lib = fi.dy.masa.malilib.util.KeyCodes.getNameForKey(code);
            if (lib != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "(?:MOUSE|BUTTON)[_\\\\s-]*(\\\\d{1,2})", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(lib);
                if (m.find()) {
                    int n = Integer.parseInt(m.group(1));
                    if (n >= 1 && n <= 8) {
                        return com.keymapviewer.util.Ui.loc.t("keymapviewer.mouse." + n);
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;'''
    if old_dn in s:
        s = s.replace(old_dn, new_dn, 1)
    else:
        print('displayNameOf NO-MATCH ->', p)
    open(p, 'w', encoding='utf-8').write(s)
    print('provider ->', p)

# ---------- 2) manager：移除 F12 硬编码（改由原版键位负责） ----------
p = 'common/src/main/java/com/keymapviewer/hotkey/KeyBindingManager.java'
s = open(p, encoding='utf-8').read()
old_m = '''    private boolean prevF12;

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
new_m = '''    /** HUD 显隐切换由各版本模块注册的原版键位（默认 F12）决定，此处不再自行轮询。 */
    private void checkHudToggleCombo() {
    }'''
if old_m in s:
    s = s.replace(old_m, new_m, 1)
    s = s.replace('        checkHudToggleCombo();\n', '', 1)
    open(p, 'w', encoding='utf-8').write(s)
    print('manager ok')
else:
    print('manager NO-MATCH')

# ---------- 3) 客户端：注册 toggleHudKey + tick 消费 ----------
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/KeyMapViewerClient.java'):
    s = open(p, encoding='utf-8').read()
    # 字段
    if 'private static KeyMapping toggleHudKey;' not in s:
        s = s.replace('    private static KeyMapping settingsKey;',
                      '    private static KeyMapping settingsKey;\n    private static KeyMapping toggleHudKey;', 1)
    # 注册（在 settingsKey 注册后）
    old_reg = '''        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "keymapviewer.key.openSettings",
                GLFW.GLFW_KEY_K,
                settingsCategory()));'''
    new_reg = old_reg + '''

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "keymapviewer.key.toggleHud",
                GLFW.GLFW_KEY_F12,
                settingsCategory()));'''
    if old_reg in s:
        s = s.replace(old_reg, new_reg, 1)
    else:
        print('reg NO-MATCH ->', p)
    # tick 消费（在 devTick(mc) 调用前）
    old_tick = '            devTick(mc);'
    new_tick = '''            while (toggleHudKey != null && toggleHudKey.consumeClick()) {
                com.keymapviewer.config.ModConfig cfg = com.keymapviewer.config.ModConfig.INSTANCE;
                cfg.hudEnabled = !cfg.hudEnabled;
                cfg.save();
            }
            devTick(mc);'''
    if old_tick in s:
        s = s.replace(old_tick, new_tick, 1)
    else:
        print('tick NO-MATCH ->', p)
    open(p, 'w', encoding='utf-8').write(s)
    print('client ->', p)

# ---------- 4) 屏幕：移除 打开备份 按钮 + 自绘 Masa 按钮；按钮改半宽；加路径小字 ----------
MASA_FIELD = '    private Button bMasaMenu;\n'
MASA_CREATE = '''        bMasaMenu = reg(Button.builder(Component.translatable("keymapviewer.openMasaMenu"), b ->
                com.keymapviewer.hotkey.malilib.MaLiLibMenu.openRoot())
                .bounds(this.width - 128, 3, 120, 16).build());
        bMasaMenu.visible = com.keymapviewer.hotkey.malilib.MaLiLibCompat.isLoaded();'''
OPEN_FIELD = '    private Button bOpenIO;\n'
VISIO = '''            bOpenIO.visible = hud;'''
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/ui/KeySettingsScreen.java'):
    s = open(p, encoding='utf-8').read()
    s = s.replace(MASA_FIELD, '')
    s = s.replace(MASA_CREATE, '')
    s = s.replace(OPEN_FIELD, '')
    s = s.replace(VISIO, '')
    if '< Masa Menu' not in s and 'Masa 菜单' not in s and 'openMasaMenu' not in s:
        pass
    # 按钮改半宽（去掉 third 三列，保留 bExport/bImport 两半）
    old3 = '''        y += 24;
        int third = (w - 8) / 3;
        bExport = reg(Button.builder(Component.translatable("keymapviewer.export"), b -> onExport())
                .bounds(px, y, third, 18).build());
        bImport = reg(Button.builder(Component.translatable("keymapviewer.import"), b -> onImport())
                .bounds(px + third + 4, y, third, 18).build());
        bOpenIO = reg(Button.builder(Component.translatable("keymapviewer.openIo"), b -> onOpenIO())
                .bounds(px + 2 * (third + 4), y, third, 18).build());
        y += 22;'''
    new2 = '''        y += 24;
        int halfW2 = (w - 4) / 2;
        bExport = reg(Button.builder(Component.translatable("keymapviewer.export"), b -> onExport())
                .bounds(px, y, halfW2, 18).build());
        bImport = reg(Button.builder(Component.translatable("keymapviewer.import"), b -> onImport())
                .bounds(px + halfW2 + 4, y, halfW2, 18).build());
        y += 22;'''
    if old3 in s:
        s = s.replace(old3, new2, 1)
    else:
        print('btn3 NO-MATCH ->', p)
    # 路径小字：把 toggle.hint 行替换为 三行小字
    if 'ext.text(font,' in s:
        tc = 'ext.text(font,'
    else:
        tc = 'graphics.drawString(font,'
    hint_old = '''        %s com.keymapviewer.util.Ui.loc.t("keymapviewer.hud.toggle.hint"), 24, hudNoticeRow + 16, Colors.TEXT_DIM);''' % tc
    hint_new = '''        %s com.keymapviewer.util.Ui.loc.t("keymapviewer.hud.toggle.hint"), 24, hudNoticeRow + 16, Colors.TEXT_DIM);
        %s com.keymapviewer.util.Ui.loc.tf("keymapviewer.io.export", com.keymapviewer.config.BindingsIo.exportDir()), 24, hudNoticeRow + 25, Colors.TEXT_DIM);
        %s com.keymapviewer.util.Ui.loc.tf("keymapviewer.io.import", com.keymapviewer.config.BindingsIo.importDir()), 24, hudNoticeRow + 34, Colors.TEXT_DIM);''' % (tc, tc, tc)
    if hint_old in s:
        s = s.replace(hint_old, hint_new, 1)
    else:
        print('hint NO-MATCH ->', p)
    open(p, 'w', encoding='utf-8').write(s)
    print('screen ->', p)

# ---------- 5) 桥接壳：init 直接无缝进入我们的界面 ----------
for mod in ['mc1201', 'mc121', 'mc1217', 'mc2612', 'mc262']:
    p = os.path.join('versions', mod, 'src/main/java/com/keymapviewer/hotkey/malilib/KeyMapViewerConfigGui.java')
    s = open(p, encoding='utf-8').read()
    open_call = 'mc.setScreenAndShow(new KeySettingsScreen());' if mod == 'mc262' else 'mc.setScreen(new KeySettingsScreen());'
    new_init = '''    @Override
    public void init() {
        super.init();
        // 由 Masa 模组菜单切入时，直接无缝进入我们的分页界面
        Minecraft mc = Minecraft.getInstance();
        {OPEN}
    }'''.replace('{OPEN}', open_call)
    s = re.sub(r'@Override\n    public void init\(\).*?\n    \}\n', new_init + '\n', s, count=1, flags=re.S)
    s = re.sub(r'import fi\.dy\.masa\.malilib\.gui\.button\.ButtonGeneric;\n', '', s)
    open(p, 'w', encoding='utf-8').write(s)
    print('configgui ->', mod)

# ---------- 6) 语言键 ----------
zz = {
    'keymapviewer.key.toggleHud': '切换 HUD 显示',
    'keymapviewer.io.export': '导出目录：%s',
    'keymapviewer.io.import': '导入目录：%s（勿修改文件名）',
}
ee = {
    'keymapviewer.key.toggleHud': 'Toggle HUD',
    'keymapviewer.io.export': 'Export folder: %s',
    'keymapviewer.io.import': 'Import folder: %s (do not rename files)',
}
for lang in glob.glob('versions/*/src/main/resources/assets/keymapviewer/lang/*.json'):
    d = json.load(open(lang, encoding='utf-8'))
    d.update(zz if lang.endswith('zh_cn.json') else ee)
    json.dump(d, open(lang, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print('lang ok')
print('DONE')