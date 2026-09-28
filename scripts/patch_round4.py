# -*- coding: utf-8 -*-
"""round4-abc：F3库串回退、重置按钮默认亮度、kmv 导出/导入目录+时间戳+最新导入。"""
import glob, json

# ---------- 1) MaLiLib 显示启发式：库显示分段比本地多则用库串（覆盖 F3 等缺失修饰键） ----------
OLD = '''                if (anyNamed) {
                    display = String.join(" + ", parts);
                } else {'''
NEW = '''                if (anyNamed) {
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
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/hotkey/malilib/MaLiLibKeyProvider.java'):
    s = open(p, encoding='utf-8').read()
    if OLD in s:
        s = s.replace(OLD, NEW, 1)
        open(p, 'w', encoding='utf-8').write(s)
        print('f3-fallback ->', p)
    else:
        print('f3 NO-MATCH ->', p)

# ---------- 2) KeyBindingInfo 增加 isDefault，Provider 写入 ----------
# 2a) 模型
p = 'common/src/main/java/com/keymapviewer/hotkey/KeyBindingInfo.java'
s = open(p, encoding='utf-8').read()
if 'public final boolean isDefault' not in s:
    s = s.replace(
        'public final KeySourceHandle handle; // 用于读写状态 / 重新绑定的句柄（只读来源可为 null）',
        'public final KeySourceHandle handle; // 用于读写状态 / 重新绑定的句柄（只读来源可为 null）\n    public final boolean isDefault; // 是否为该来源的默认绑定（未改动过）')
    s = s.replace(
        'public KeyBindingInfo(Source source, String modName, String category, String actionName,\n                          String displayKey, int[] keyCodes, KeySourceHandle handle) {',
        'public KeyBindingInfo(Source source, String modName, String category, String actionName,\n                          String displayKey, int[] keyCodes, KeySourceHandle handle, boolean isDefault) {')
    s = s.replace(
        'this.displayKey = displayKey;\n        this.keyCodes = keyCodes;\n        this.handle = handle;',
        'this.displayKey = displayKey;\n        this.keyCodes = keyCodes;\n        this.handle = handle;\n        this.isDefault = isDefault;')
    open(p, 'w', encoding='utf-8').write(s)
    print('model isDefault ok')

# 2b) vanilla provider (5 份) 传 isDefault
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/hotkey/vanilla/VanillaKeyProvider.java'):
    s = open(p, encoding='utf-8').read()
    # 需要读取当前绑定与默认比较；同时修正 out.add 调用加末参 isDefault
    import re
    m = re.search(r'out\.add\(new KeyBindingInfo\(KeyBindingInfo\.Source\.VANILLA, modName, category, actionName,[^\n]*\)\);', s)
    if m is None:
        print('vanilla add NO-MATCH ->', p); continue
    add_call = m.group(0)
    new_call = add_call[:-1] + ', boundDefault());)' if add_call.endswith(');') else add_call
    # 简单做法：手动替换为携带新参数（构造器加了末参）
    fixed = add_call.replace('display, codes, new VanillaHandle(km)));',
                             'display, codes, new VanillaHandle(km), isVanillaDefault(km)));')
    if fixed != add_call:
        s = s.replace(add_call, fixed, 1)
        # 追加帮助方法 isVanillaDefault（用 KeyMappingHelper 读取键与默认比较）
        if 'isVanillaDefault' not in s:
            s = s.replace('    private static final class VanillaHandle implements KeySourceHandle {',
'''    private static boolean isVanillaDefault(KeyMapping km) {
        try {
            return KeyMappingHelper.getBoundKeyOf(km).equals(km.getDefaultKey());
        } catch (Throwable t) {
            return true;
        }
    }

    private static final class VanillaHandle implements KeySourceHandle {''')
        open(p, 'w', encoding='utf-8').write(s)
        print('vanilla isDefault ->', p)
    else:
        print('vanilla pattern miss ->', p)

# 2c) malilib provider (5 份) 传 isDefault（hotkey.isModified() 取反）
for p in glob.glob('versions/*/src/main/java/com/keymapviewer/hotkey/malilib/MaLiLibKeyProvider.java'):
    s = open(p, encoding='utf-8').read()
    fixed = s.replace('name.trim(), display, codes, new MaLiLibHandle(hotkey)));',
                      'name.trim(), display, codes, new MaLiLibHandle(hotkey), isDefault(hotkey)));')
    if fixed != s:
        s = fixed
        if 'private static boolean isDefault(' not in s:
            s = s.replace('    private static String displayNameOf(int code) {',
'''    private static boolean isDefault(IHotkey hotkey) {
        try {
            return !hotkey.isModified();
        } catch (Throwable t) {
            return true;
        }
    }

    private static String displayNameOf(int code) {''')
        open(p, 'w', encoding='utf-8').write(s)
        print('malilib isDefault ->', p)
    else:
        print('malilib pattern miss ->', p)

# 2d) 列表重置按钮亮度（common ListViewPanel）
p = 'common/src/main/java/com/keymapviewer/ui/ListViewPanel.java'
s = open(p, encoding='utf-8').read()
old_btn = '''        int kw = colKeyW(cv);'''
# 在 drawRow 的 reset 按钮绘制段，加入 isDefault 亮度。查找现 reset 按钮绘制段落：
pat = re.compile(r'ext\.fill\(cachedResetX, y, cachedResetW, ROW_H - 2, .*?\);\n')
# 直接用固定文本替换（依据现有实现猜测；若没有则跳过并提示）
marker = 'cv.fill(cachedResetX, y, cachedResetW, ROW_H - 2,'
if marker in s:
    # 找到整段 draw reset 代码
    start = s.index(marker)
    end = s.index('ROH', start) if 'ROH' in s[start:] else s.index(');', s.index('});', start))
    seg = s[start:s.index('}', s.index('});', start)) + 2]
    s = s.replace(seg, seg, 1)  # 占位
    open(p, 'w', encoding='utf-8').write(s)
    print('listview reset-brightness: MANUAL needed (no auto patch)')
else:
    print('listview reset marker absent ->', p)