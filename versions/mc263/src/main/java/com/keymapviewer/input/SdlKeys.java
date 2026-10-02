package com.keymapviewer.input;

/**
 * MC 26.3 的按键编码桥接层。
 *
 * <p>26.3 起 Minecraft 用 <b>SDL</b> 取代了 GLFW（版本清单里 {@code lwjgl-glfw} 已被
 * {@code lwjgl-sdl} 替换），按键的整数编码空间随之改名换值：</p>
 *
 * <ul>
 *   <li><b>MC 原版</b>：{@code InputConstants.KEY_*} = <b>SDL 扫描码</b>（SDL_Scancode），
 *       表示物理键位，US 布局下 A=4、SPACE=44、F1=58、LCTRL=224。
 *       {@code KeyEvent.key()}、{@code InputConstants.Type.KEYBOARD} 与
 *       {@code KeyMapping(String,int,Category)} 的第二参都用这一空间。</li>
 *   <li><b>MaLiLib 0.30</b>：<b>同样是 SDL 扫描码</b>——{@code KeybindMulti} 内部字段就叫
 *       {@code scanCodes}，{@code getKeyCode(KeyMapping)} 直接取
 *       {@code InputConstants.Key.getValue()}，{@code isKeyDown(int)} 也直接转交
 *       {@code InputConstants.isKeyDown(int)}，序列化走 {@code getStorageStringForScanCode}。
 *       鼠标键则统一为「MC 鼠标键号 - 100」：左=1-100=-99、中=2-100=-98、右=3-100=-97
 *       （即 {@code MOUSE_LEFT}/{@code MOUSE_MIDDLE}/{@code MOUSE_RIGHT} 等常量）。</li>
 *   <li><b>本模组规范空间</b>：沿用 <b>GLFW 键码</b>（32..348，字母为大写 ASCII；鼠标为
 *       -100..-93，左键=-100）。因为 {@code common} 的键盘视图布局、键帽标签、冲突表与导出格式
 *       全都建立在它之上，改动会波及全部 6 个版本模块。</li>
 * </ul>
 *
 * <p>所以本类只做「边界归一化」：Provider 读到的键先译成 GLFW 规范码，写回时再译回目标空间；
 * {@code common} 与其它版本模块完全不需要改动。</p>
 *
 * <p>鼠标另有一套编号：26.3 的鼠标键号是 SDL 约定（1=左 2=中 3=右），
 * 而本模组语言文件沿用旧约定（mouse.1=左键、mouse.2=右键、mouse.3=中键），
 * 故 2/3 需要互换，见 {@link #mouseOrdinalFromVanilla} 等。</p>
 *
 * <p>注意：MaLiLib 自带的 {@code fi.dy.masa.malilib.util.input.KeyCodes}（{@code KEY_A=97}、
 * 特殊键 {@code 0x40000000|扫描码}）是 <b>SDL 逻辑键码（SDLK）</b>空间，用于按键名/字符串显示，
 * <b>不是</b> keybind 存储用的空间，切勿混用。</p>
 */
public final class SdlKeys {

    // ------------------------------------------------------------------
    // GLFW 键码常量（26.3 的编译/运行类路径上已无 lwjgl-glfw，故在此自带所需常量）
    // ------------------------------------------------------------------
    public static final int GLFW_KEY_SPACE = 32;
    public static final int GLFW_KEY_APOSTROPHE = 39;
    public static final int GLFW_KEY_COMMA = 44;
    public static final int GLFW_KEY_MINUS = 45;
    public static final int GLFW_KEY_PERIOD = 46;
    public static final int GLFW_KEY_SLASH = 47;
    public static final int GLFW_KEY_0 = 48;
    public static final int GLFW_KEY_9 = 57;
    public static final int GLFW_KEY_SEMICOLON = 59;
    public static final int GLFW_KEY_EQUAL = 61;
    public static final int GLFW_KEY_A = 65;
    public static final int GLFW_KEY_K = 75;
    public static final int GLFW_KEY_Z = 90;
    public static final int GLFW_KEY_LEFT_BRACKET = 91;
    public static final int GLFW_KEY_BACKSLASH = 92;
    public static final int GLFW_KEY_RIGHT_BRACKET = 93;
    public static final int GLFW_KEY_GRAVE_ACCENT = 96;
    public static final int GLFW_KEY_ESCAPE = 256;
    public static final int GLFW_KEY_ENTER = 257;
    public static final int GLFW_KEY_TAB = 258;
    public static final int GLFW_KEY_BACKSPACE = 259;
    public static final int GLFW_KEY_INSERT = 260;
    public static final int GLFW_KEY_DELETE = 261;
    public static final int GLFW_KEY_RIGHT = 262;
    public static final int GLFW_KEY_LEFT = 263;
    public static final int GLFW_KEY_DOWN = 264;
    public static final int GLFW_KEY_UP = 265;
    public static final int GLFW_KEY_PAGE_UP = 266;
    public static final int GLFW_KEY_PAGE_DOWN = 267;
    public static final int GLFW_KEY_HOME = 268;
    public static final int GLFW_KEY_END = 269;
    public static final int GLFW_KEY_CAPS_LOCK = 280;
    public static final int GLFW_KEY_SCROLL_LOCK = 281;
    public static final int GLFW_KEY_NUM_LOCK = 282;
    public static final int GLFW_KEY_PRINT_SCREEN = 283;
    public static final int GLFW_KEY_PAUSE = 284;
    public static final int GLFW_KEY_F1 = 290;
    public static final int GLFW_KEY_F2 = 291;
    public static final int GLFW_KEY_F3 = 292;
    public static final int GLFW_KEY_F4 = 293;
    public static final int GLFW_KEY_F5 = 294;
    public static final int GLFW_KEY_F6 = 295;
    public static final int GLFW_KEY_F7 = 296;
    public static final int GLFW_KEY_F8 = 297;
    public static final int GLFW_KEY_F9 = 298;
    public static final int GLFW_KEY_F10 = 299;
    public static final int GLFW_KEY_F11 = 300;
    public static final int GLFW_KEY_F12 = 301;
    public static final int GLFW_KEY_F13 = 302;
    public static final int GLFW_KEY_F24 = 313;
    public static final int GLFW_KEY_F25 = 314;
    public static final int GLFW_KEY_KP_0 = 320;
    public static final int GLFW_KEY_KP_9 = 329;
    public static final int GLFW_KEY_KP_DECIMAL = 330;
    public static final int GLFW_KEY_KP_DIVIDE = 331;
    public static final int GLFW_KEY_KP_MULTIPLY = 332;
    public static final int GLFW_KEY_KP_SUBTRACT = 333;
    public static final int GLFW_KEY_KP_ADD = 334;
    public static final int GLFW_KEY_KP_ENTER = 335;
    public static final int GLFW_KEY_KP_EQUAL = 336;
    public static final int GLFW_KEY_LEFT_SHIFT = 340;
    public static final int GLFW_KEY_LEFT_CONTROL = 341;
    public static final int GLFW_KEY_LEFT_ALT = 342;
    public static final int GLFW_KEY_LEFT_SUPER = 343;
    public static final int GLFW_KEY_RIGHT_SHIFT = 344;
    public static final int GLFW_KEY_RIGHT_CONTROL = 345;
    public static final int GLFW_KEY_RIGHT_ALT = 346;
    public static final int GLFW_KEY_RIGHT_SUPER = 347;
    public static final int GLFW_KEY_MENU = 348;

    // (SDL 扫描码, GLFW 键码) 成对表；两者都是「物理键位」空间，故为固定双射。
    private static final int[][] SCANCODE_TO_GLFW = {
        // 字母 A-Z
        {4, GLFW_KEY_A}, {5, 66}, {6, 67}, {7, 68}, {8, 69}, {9, 70}, {10, 71}, {11, 72},
        {12, 73}, {13, 74}, {14, GLFW_KEY_K}, {15, 76}, {16, 77}, {17, 78}, {18, 79}, {19, 80},
        {20, 81}, {21, 82}, {22, 83}, {23, 84}, {24, 85}, {25, 86}, {26, 87}, {27, 88},
        {28, 89}, {29, GLFW_KEY_Z},
        // 数字行 1..9,0
        {30, 49}, {31, 50}, {32, 51}, {33, 52}, {34, 53}, {35, 54}, {36, 55}, {37, 56},
        {38, 57}, {39, GLFW_KEY_0},
        // 主功能区
        {40, GLFW_KEY_ENTER}, {41, GLFW_KEY_ESCAPE}, {42, GLFW_KEY_BACKSPACE},
        {43, GLFW_KEY_TAB}, {44, GLFW_KEY_SPACE},
        // 标点
        {45, GLFW_KEY_MINUS}, {46, GLFW_KEY_EQUAL}, {47, GLFW_KEY_LEFT_BRACKET},
        {48, GLFW_KEY_RIGHT_BRACKET}, {49, GLFW_KEY_BACKSLASH},
        {51, GLFW_KEY_SEMICOLON}, {52, GLFW_KEY_APOSTROPHE}, {53, GLFW_KEY_GRAVE_ACCENT},
        {54, GLFW_KEY_COMMA}, {55, GLFW_KEY_PERIOD}, {56, GLFW_KEY_SLASH},
        {57, GLFW_KEY_CAPS_LOCK},
        // F1-F12
        {58, GLFW_KEY_F1}, {59, GLFW_KEY_F2}, {60, GLFW_KEY_F3}, {61, GLFW_KEY_F4},
        {62, GLFW_KEY_F5}, {63, GLFW_KEY_F6}, {64, GLFW_KEY_F7}, {65, GLFW_KEY_F8},
        {66, GLFW_KEY_F9}, {67, GLFW_KEY_F10}, {68, GLFW_KEY_F11}, {69, GLFW_KEY_F12},
        // 编辑区
        {70, GLFW_KEY_PRINT_SCREEN}, {71, GLFW_KEY_SCROLL_LOCK}, {72, GLFW_KEY_PAUSE},
        {73, GLFW_KEY_INSERT}, {74, GLFW_KEY_HOME}, {75, GLFW_KEY_PAGE_UP},
        {76, GLFW_KEY_DELETE}, {77, GLFW_KEY_END}, {78, GLFW_KEY_PAGE_DOWN},
        {79, GLFW_KEY_RIGHT}, {80, GLFW_KEY_LEFT}, {81, GLFW_KEY_DOWN}, {82, GLFW_KEY_UP},
        // 小键盘
        {83, GLFW_KEY_NUM_LOCK}, {84, GLFW_KEY_KP_DIVIDE}, {85, GLFW_KEY_KP_MULTIPLY},
        {86, GLFW_KEY_KP_SUBTRACT}, {87, GLFW_KEY_KP_ADD}, {88, GLFW_KEY_KP_ENTER},
        {89, 321}, {90, 322}, {91, 323}, {92, 324}, {93, 325}, {94, 326}, {95, 327},
        {96, 328}, {97, GLFW_KEY_KP_9}, {98, GLFW_KEY_KP_0}, {99, GLFW_KEY_KP_DECIMAL},
        // 菜单键 / 小键盘 =
        {101, GLFW_KEY_MENU}, {103, GLFW_KEY_KP_EQUAL},
        // F13-F24
        {104, GLFW_KEY_F13}, {105, 303}, {106, 304}, {107, 305}, {108, 306}, {109, 307},
        {110, 308}, {111, 309}, {112, 310}, {113, 311}, {114, 312}, {115, GLFW_KEY_F24},
        // 修饰键（左右分离）
        {224, GLFW_KEY_LEFT_CONTROL}, {225, GLFW_KEY_LEFT_SHIFT}, {226, GLFW_KEY_LEFT_ALT},
        {227, GLFW_KEY_LEFT_SUPER}, {228, GLFW_KEY_RIGHT_CONTROL}, {229, GLFW_KEY_RIGHT_SHIFT},
        {230, GLFW_KEY_RIGHT_ALT}, {231, GLFW_KEY_RIGHT_SUPER},
    };

    private static final java.util.Map<Integer, Integer> SCANCODE2GLFW = new java.util.HashMap<>();
    private static final java.util.Map<Integer, Integer> GLFW2SCANCODE = new java.util.HashMap<>();

    static {
        for (int[] pair : SCANCODE_TO_GLFW) {
            SCANCODE2GLFW.put(pair[0], pair[1]);
            GLFW2SCANCODE.put(pair[1], pair[0]);
        }
    }

    private SdlKeys() {
    }

    /** SDL 扫描码 → GLFW 规范键码；未知键返回 -1。 */
    public static int scancodeToGlfw(int scancode) {
        Integer g = SCANCODE2GLFW.get(scancode);
        return g == null ? -1 : g;
    }

    /** GLFW 规范键码 → SDL 扫描码；未知键返回 -1。 */
    public static int glfwToScancode(int glfw) {
        Integer s = GLFW2SCANCODE.get(glfw);
        return s == null ? -1 : s;
    }

    /**
     * MaLiLib 的 keybind 键码 → GLFW 规范键码。
     * 键盘部分与 MC 原版同处 SDL 扫描码空间；鼠标键（负值）由
     * {@link #mouseOrdinalFromMalilib} 处理，此处返回 -1。
     */
    public static int malilibToGlfw(int malilibCode) {
        if (malilibCode < 0) {
            return -1;   // 鼠标键：-99…-92
        }
        return scancodeToGlfw(malilibCode);
    }

    /** GLFW 规范键码 → MaLiLib 的 keybind 键码（SDL 扫描码）；不支持返回 -1。 */
    public static int glfwToMalilib(int glfw) {
        return glfwToScancode(glfw);
    }

    // ------------------------------------------------------------------
    // 鼠标键：各空间编号约定不同，统一先转成“语言文件序号”1..8
    // （1=左键 2=右键 3=中键 4..8=侧键，见 lang 中 keymapviewer.mouse.N）
    // ------------------------------------------------------------------

    /**
     * MC 26.3 鼠标键号 → 语言文件序号。
     * SDL 约定 1=左 2=中 3=右；本模组语言文件是 1=左 2=右 3=中，故 2/3 互换。
     */
    public static int mouseOrdinalFromVanilla(int mcButton) {
        switch (mcButton) {
            case 1: return 1;   // 左
            case 2: return 3;   // 中
            case 3: return 2;   // 右
            default: return mcButton >= 4 && mcButton <= 8 ? mcButton : -1;
        }
    }

    /** 语言文件序号 1..8 → MC 26.3 的鼠标键号（SDL 约定）；越界返回 -1。 */
    public static int vanillaButtonFromMouseOrdinal(int ordinal) {
        switch (ordinal) {
            case 1: return 1;   // 左
            case 2: return 3;   // 右
            case 3: return 2;   // 中
            case 4: case 5: case 6: case 7: case 8:
                return ordinal;
            default: return -1;
        }
    }

    /**
     * MaLiLib 鼠标键常量 → 语言文件序号。
     * MaLiLib 用「MC 鼠标键号 - 100」：MOUSE_LEFT=-99、MOUSE_MIDDLE=-98、MOUSE_RIGHT=-97、
     * MOUSE_BACK=-96、MOUSE_FORWARD=-95、MOUSE_EXTRA_3..5=-94..-92；非鼠标键返回 -1。
     */
    public static int mouseOrdinalFromMalilib(int code) {
        switch (code) {
            case -99: return 1;   // LEFT
            case -98: return 3;   // MIDDLE → 中键
            case -97: return 2;   // RIGHT  → 右键
            case -96: return 4;   // BACK
            case -95: return 5;   // FORWARD
            case -94: return 6;   // EXTRA_3
            case -93: return 7;   // EXTRA_4
            case -92: return 8;   // EXTRA_5
            default:  return -1;
        }
    }

    /** 语言文件序号 1..8 → MaLiLib 鼠标键常量；越界返回 Integer.MIN_VALUE。 */
    public static int malilibCodeFromMouseOrdinal(int ordinal) {
        switch (ordinal) {
            case 1: return -99;
            case 2: return -97;
            case 3: return -98;
            case 4: return -96;
            case 5: return -95;
            case 6: return -94;
            case 7: return -93;
            case 8: return -92;
            default: return Integer.MIN_VALUE;
        }
    }

    /** 语言文件序号 1..8 → 本模组规范鼠标码 -100..-93（左键=-100）。 */
    public static int canonicalMouseCode(int ordinal) {
        return ordinal - 101;
    }

    /** 本模组规范鼠标码 -100..-93 → 语言文件序号 1..8；非鼠标码返回 -1。 */
    public static int mouseOrdinalFromCanonical(int canonical) {
        if (canonical >= -100 && canonical <= -93) {
            return canonical + 101;
        }
        return -1;
    }
}
