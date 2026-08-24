package cn.academy.config;

import cn.lambdalib2.util.MathUtils;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AbilityConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> INIT_CP, ADD_CP, INIT_OVERLOAD, ADD_OVERLOAD;
    private static final ForgeConfigSpec.DoubleValue CP_RECOVER_SPEED, OVL_RECOVER_SPEED, MAXCP_INCR, MAXO_INCR;
    private static final ForgeConfigSpec.IntValue CP_RECOVER_CD, OVL_RECOVER_CD;

    private static final ForgeConfigSpec.DoubleValue COIN_HIT, COIN_CHARGE;

    private static final ForgeConfigSpec.IntValue BRAIN_CP, BRAIN_ADV_CP, BRAIN_ADV_OVL;
    private static final ForgeConfigSpec.DoubleValue MIND_MULT, MIND_CALC_CAP;
    private static final ForgeConfigSpec.DoubleValue ESPER_REC, ESPER_REC_SCARCE, ESPER_SCARCE_LINE,
            ESPER_DMG_CP, ESPER_BURST;

    private static final ForgeConfigSpec.BooleanValue IMPRESSION_NO_DARKNESS;

    private static final ForgeConfigSpec.BooleanValue PIERCE_ENABLED;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PIERCE_BLACKLIST;
    private static final ForgeConfigSpec.BooleanValue PIERCE_BYPASS_CAP;
    private static final ForgeConfigSpec.BooleanValue PIERCE_BREAK_IFRAME;

    private static final ForgeConfigSpec.DoubleValue ABYSS_CP_MULT;

    private static final Map<String, ForgeConfigSpec.DoubleValue> SK = new HashMap<>();

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment(" AcademyCraft 能力系统数值。技能的消耗/冷却/伤害改完【立刻】生效;"
                        + "CP 与过载的【上限】是存在玩家存档里的,改完要重新登录或升级才会按新值算"
                        + "(游戏内配置界面的「应用到服务器」会自动帮在线玩家刷新)。").push("ability");

        b.comment(" CP(超能力值)与过载。下面的列表都是 6 个数,从左到右对应等级 LV0~LV5。").push("cp");
        INIT_CP = b.comment(" 各等级一开始的 CP 上限。")
                .defineList("init_cp", List.of(1800, 1800, 2800, 4000, 5800, 8000), AbilityConfig::nonNeg);
        ADD_CP = b.comment(" 升到这个等级时,CP 上限再额外加多少。")
                .defineList("add_cp", List.of(0, 900, 1000, 1500, 1700, 12000), AbilityConfig::nonNeg);
        INIT_OVERLOAD = b.comment(" 各等级一开始的过载上限。")
                .defineList("init_overload", List.of(100, 100, 150, 240, 350, 500), AbilityConfig::nonNeg);
        ADD_OVERLOAD = b.comment(" 升到这个等级时,过载上限再额外加多少。")
                .defineList("add_overload", List.of(0, 40, 70, 80, 100, 500), AbilityConfig::nonNeg);
        CP_RECOVER_SPEED = b.comment(" CP 回复速度倍数(1.0=正常,2.0=快一倍)。")
                .defineInRange("cp_recover_speed", 1.0, 0.0, 1000.0);
        CP_RECOVER_CD = b.comment(" 用过 CP 后,要停多久才开始回复(20=1 秒)。")
                .defineInRange("cp_recover_cooldown_ticks", 15, 0, 12000);
        OVL_RECOVER_SPEED = b.comment(" 过载消退速度倍数(1.0=正常)。")
                .defineInRange("overload_recover_speed", 1.0, 0.0, 1000.0);
        OVL_RECOVER_CD = b.comment(" 攒了过载后,要停多久才开始消退(20=1 秒)。")
                .defineInRange("overload_recover_cooldown_ticks", 32, 0, 12000);
        MAXCP_INCR = b.comment(" 越用越强:消耗 CP 时,CP 上限悄悄变大的幅度(0=不成长)。")
                .defineInRange("maxcp_incr_rate", 0.0025, 0.0, 10.0);
        MAXO_INCR = b.comment(" 越用越强:攒过载时,过载上限悄悄变大的幅度(0=不成长)。")
                .defineInRange("maxoverload_incr_rate", 0.0058, 0.0, 10.0);
        b.pop();

        b.comment(" 超电磁炮的抛硬币玩法。").push("coin");
        COIN_HIT = b.comment(" 硬币快落回手里的瞬间按键才算接住:进度超过这个数才能开炮(调大=更难)。")
                .defineInRange("qte_hit_threshold", 0.7, 0.0, 1.0);
        COIN_CHARGE = b.comment(" 进度低于这个数时,屏幕上显示「蓄力中」。")
                .defineInRange("qte_charge_threshold", 0.6, 0.0, 1.0);
        b.pop();

        b.comment(" 三个通用被动课程学会后的加成。").push("generic");
        BRAIN_CP = b.comment(" 大脑训练课程:学会后 CP 上限加多少。")
                .defineInRange("brain_course_add_cp", 1000, 1, Integer.MAX_VALUE);
        BRAIN_ADV_CP = b.comment(" 高级大脑训练课程:学会后 CP 上限加多少。")
                .defineInRange("brain_course_advanced_add_cp", 1500, 1, Integer.MAX_VALUE);
        BRAIN_ADV_OVL = b.comment(" 高级大脑训练课程:学会后过载上限加多少。")
                .defineInRange("brain_course_advanced_add_overload", 100, 1, Integer.MAX_VALUE);
        MIND_MULT = b.comment(" 思维修养课程:学会后 CP 回复速度乘多少(1.2=快 20%)。")
                .defineInRange("mind_course_recover_mult", 1.2, 0.0, 100.0);
        MIND_CALC_CAP = b.comment(" 思维计算约束课程:学会后,施放技能【每秒】最多消耗最大 CP 的这个比例(0.05=5%)。")
                .defineInRange("mind_calc_course_cap_ratio", 0.05, 0.0, 1.0);

        ESPER_REC = b.comment(" 超能力者的自我修养:学会后 CP 回复速度乘多少(2.0 = 额外提升 100%)。")
                .defineInRange("esper_recover_mult", 2.0, 0.0, 100.0);
        ESPER_REC_SCARCE = b.comment(" 超能力者的自我修养:达成 Lv5 且计算力【不充裕】时,CP 回复速度改乘多少(4.0 = 额外提高 300%)。")
                .defineInRange("esper_recover_mult_scarce", 4.0, 0.0, 100.0);
        ESPER_SCARCE_LINE = b.comment(" 超能力者的自我修养:当前 CP 低于最大值的这个比例就算【不充裕】(0.5 = 50%)。")
                .defineInRange("esper_scarce_line", 0.5, 0.0, 1.0);
        ESPER_DMG_CP = b.comment(" 超能力者的自我修养:Lv5 且【充裕】时,技能伤害额外附加「当前 CP」的这个比例(0.001 = 0.1%)。")
                .defineInRange("esper_damage_cp_ratio", 0.001, 0.0, 10.0);
        ESPER_BURST = b.comment(" 超能力者的自我修养:Lv5 且【不充裕】时,每 0.5 秒额外回复「最大 CP」的这个比例(0.08 = 8%)。")
                .defineInRange("esper_burst_ratio", 0.08, 0.0, 10.0);
        b.pop();

        b.comment(" 各技能的消耗与冷却。每项两个数:_lv1=技能刚学会时,_lvmax=技能练满时,",
                        " 中间随熟练度平滑变化。是每发扣还是持续扣、冷却多长,看每个技能自己那行说明。")
                .push("skill");

        skill(b, "electric_arc", " 电弧(电击使):每发的消耗;冷却单位 tick;damage=每次命中的伤害",
                30, 70, 18, 11, 15.0, 5.0, 4, 8);
        skill(b, "railgun", " 超电磁炮(电击使):每发的消耗;冷却单位 tick;damage=命中伤害",
                200, 450, 180, 120, 300.0, 160.0, 60, 110);

        b.comment(" 落雷(电击使):每发的消耗;冷却单位 tick;",
                        " damage=被雷直接劈中的伤害;aoe_damage=落点周围溅射的伤害").push("thunder_bolt");
        def(b, "thunder_bolt", "exp_rate", 2.0, 0.4);
        def(b, "thunder_bolt", "cp", 280, 420);
        def(b, "thunder_bolt", "overload", 50, 27);
        def(b, "thunder_bolt", "cooldown", 120, 50);
        def(b, "thunder_bolt", "damage", 10, 25);
        def(b, "thunder_bolt", "aoe_damage", 6, 15);
        b.pop();
        skill(b, "thunder_clap", " 雷云(电击使):cp=蓄力期间每 tick 扣;overload=起手一次性扣并钉住;"
                        + "cooldown=每蓄力 1 tick 折算多少冷却(实际冷却 = 蓄力的 tick 数 × 它,蓄越久冷却越长);"
                        + "damage=基础伤害,蓄满还会再乘一个 1.0~1.2 的加成",
                18, 25, 390, 252, 10.0, 6.0, 36, 72);
        skill(b, "body_intensify", " 身体强化(电击使):cp=持续期间每 tick 扣;overload=起手一次性扣并钉住;冷却单位 tick",
                20, 15, 200, 120, 900.0, 600.0);
        skill(b, "charging", " 充能(电击使):cp=充能期间每 tick 扣;overload=起手一次性扣并钉住;无冷却",
                3, 7, 65, 48, null, null);

        b.comment(
                " 电磁引导(电击使):cp=飞行期间每 tick 扣;overload=起手一次性扣并钉住;无冷却。",
                " 两个长按阈值(tick,20=1 秒)。它们【故意不一样】,别图省事调成同一个数:",
                " charge_time = 【没开着的时候】按住多久算蓄力 → 开重力牵引;短于它松手 = 开锚点/甩。",
                "   这一档你是在有意长按,所以要跟手。误判的后果也轻(开成重力而不是锚点)。",
                " recharge_time = 【已经开着的时候】按住多久算再蓄力 → 换目标重新牵引;短于它松手 = 取消。",
                "   这一档的主操作是【单点取消】,所以门槛要高,不然普通单击会被当成长按、",
                "   把正在生效的效果又触发一遍(2026-07-31 修的就是这个 bug)。",
                "   人手单击常在 100~250ms(2~5 tick)抖动,阈值 3 时 175ms 的单击有一半概率误判、",
                "     200ms 以上必然误判。这一项别低于 6,推荐保持 10(0.5 秒)。")
                .push("mag_movement");
        def(b, "mag_movement", "exp_rate", 2.0, 0.4);
        def(b, "mag_movement", "cp", 15, 8);
        def(b, "mag_movement", "overload", 60, 30);

        def(b, "mag_movement", "charge_time", 4, 4);
        def(b, "mag_movement", "recharge_time", 10, 10);
        b.pop();

        b.comment(
                "  铁砂控制(电击使):开关式,再按一次技能键 / CP 耗尽 / 过载爆表才结束。",
                "  cp、overload = 开启瞬间扣一次;cp_tick、overload_tick = 开启期间每 tick 扣;",
                "  cooldown = 关掉之后的冷却(tick,20 = 1 秒)。以上四项随熟练度插值。",
                "  ",
                "  视觉参数(windup 到 whip_budget)的 _lv1 与 _lvmax 必须填相同的值。",
                "  这些数客户端也要读来画粒子,而客户端拿不到熟练度,两端只能各读各的配置;",
                "  填成不一样会导致别人看到的和你自己看到的不一致。",
                "  ",
                "  ===== 取砂与砂云 =====",
                "  windup = 前摇(tick):开启后砂云从无到满要多久。",
                "  radius = 取砂半径(格):在身周多大范围内挑地面格起砂。",
                "  streams = 同时最多几条砂流。stream_interval = 每隔几 tick 起一条新的。",
                "  stream_ticks = 一条砂流从地面爬到砂云要几 tick,越小越急。",
                "  stream_density = 砂流每格撒几片。调小会从一条线退成一串珠子。",
                "  wobble / turns = 砂流绕主轴的摆幅(格)与总圈数,决定它有多弯。",
                "  cloud_height = 砂云中心在眼位上方几格。cloud_radius / cloud_thick = 云的半径与半厚。",
                "  cloud_density = 砂云每 tick 撒几片。直接决定帧数,嫌卡先调它,再调 streams。",
                "  guard_radius / guard_half_h = 防御档砂盾离身体多远、上下多高。",
                "  guard_density = 砂盾每 tick 撒几片,同样吃帧数。",
                "  ",
                "  ===== 砂鞭(进攻档)=====",
                "  whip_density = 砂鞭每格撒几片。",
                "  whip_chase = 鞭梢每 tick 朝目标追多少(0~1)。越小越甩、越大越贴。",
                "     它同时是能不能打中的闸门:追得太慢会永远够不着移动中的目标。",
                "  whip_spin = 鞭子绕轴旋转的基准速度,实际值随攻击频率放大。",
                "  whip_sag = 鞭子向下垂多少(占云到目标落差的比例)。调 0 会让鞭子大半落在视野外。",
                "  whip_hit = 每次结算伤害时在目标身上炸几片砂。调 0 就没有命中反馈。",
                "  whip_coil = 规则螺旋占多少权重。0 = 纯随机乱窜,1 = 匀速螺旋。",
                "  whip_turns = 砂鞭全长绕多少度,只在 whip_coil 大于 0 时起作用。",
                "  whip_wobble = 砂鞭乱窜的幅度上限(格)。实际横向跨度约为本值的 0.45 倍。",
                "  whip_tail_bias = 撒砂往鞭梢堆的偏置。1 = 沿全长均摊,越大越集中在末端。",
                "  whip_trail = 留多大比例的砂去画从砂云过来的引线(0~1)。调 0 彻底断开。",
                "  whip_tip_spread = 鞭梢吐砂的散布半径(格)。小 = 紧实一束,大 = 散开的砂雾。",
                "  whip_budget = 全部砂鞭合计每 tick 最多撒几片,按当前鞭数均分。嫌卡调小它。",
                "     它只让每条鞭子画得细一点,不会只画前几条。",
                "  ",
                "  ===== 鞭梢的环绕与贯穿 =====",
                "  orbit_near / orbit_far = 鞭梢绕目标转的水平半径范围(格)。",
                "  orbit_low / orbit_high = 鞭梢的高度范围(目标上方多少格),恒在上方,不会钻地。",
                "     每次贯穿之后在这四个数框出的范围内重掷一组,所以每一轮的轨道大小与高低都不同;",
                "     半径越大转得越快。贯穿冲程 = 2 × 水平半径,调大它们那一下会扎得更远。",
                "  orbit_blend = 换挡的平滑时长,单位是 whip_cd 的倍数。调 0 变成硬切。",
                "  whip_pierce = 一次贯穿动作持续几 tick。调大穿得慢看得清,调小抽得干脆。",
                "  ",
                "  ===== 被矢量操作挡下 =====",
                "  deflect_ticks = 被矢量偏移弹开后,飞开多久才绕回来继续打(tick)。",
                "  deflect_dist = 被弹开时鞭梢最远飞出去多少格。",
                "     被偏移的砂鞭不会消散,期间不结算伤害,时间一到自动恢复追击,然后可能再次被弹开。",
                "     被矢量反射则是归属易主、调头打施法者自己、打到就散,那条路没有可调参数。",
                "  ",
                "  ===== 电弧 =====",
                "  arc_interval = 砂云或砂盾每隔几 tick 起一道弧。",
                "  arc_whip_interval = 每条鞭子每隔几 tick 起一道。",
                "  arc_life = 每道弧活几 tick。arc_len = 每道弧多长(格)。",
                "     寿命短加间隔短 = 同时只有一两道但一直噼啪;两个都调大会变成一团静止的电。",
                "  ",
                "  ===== 判定参数(只有服务端读,可以随熟练度插值)=====",
                "  whip_damage = 进攻档每一鞭的伤害。",
                "  whip_cd = 同一目标两次伤害的最小间隔(tick)。",
                "  whip_range = 分裂出新鞭子的半径(格)。不限数量,一只敌对一条鞭。",
                "  whip_release = 已经咬住的目标跑出这么远才松开、缩回砂云。",
                "  whip_touch = 判定盒往外放多少格才算碰到。",
                "     伤害由碰到触发,不是范围内定时结算:追击途中不掉血,目标躲得开就打不中,",
                "     所以实际 DPS 低于伤害除以 whip_cd 的理论值。",
                "     打不打其他玩家的开关不在本文件,见 config/academy-craft.toml 的 [generic] attackPlayer,",
                "     也就是数据终端 - 设置 - 开启PvP (玩家伤害)。创造、观察者、同队友军一律不锁。",
                "  guard_reduce = 防御档减伤比例。这道减伤在护甲之前结算,与护甲相乘。",
                "     减伤不分来源方向,所以砂盾造型也是全向的。")
                .push("iron_sand");
        def(b, "iron_sand", "exp_rate", 2.0, 0.4);
        def(b, "iron_sand", "cp", 260, 180);
        def(b, "iron_sand", "cp_tick", 3.2, 1.6);
        def(b, "iron_sand", "overload", 40, 26);
        def(b, "iron_sand", "overload_tick", 0.35, 0.18);
        def(b, "iron_sand", "cooldown", 120, 60);
        def(b, "iron_sand", "windup", 30, 30);
        def(b, "iron_sand", "radius", 8, 8);
        def(b, "iron_sand", "streams", 5, 5);
        def(b, "iron_sand", "stream_interval", 4, 4);
        def(b, "iron_sand", "stream_ticks", 26, 26);
        def(b, "iron_sand", "stream_density", 12, 12);
        def(b, "iron_sand", "wobble", 1.5, 1.5);
        def(b, "iron_sand", "turns", 2.0, 2.0);
        def(b, "iron_sand", "cloud_height", 5.7, 5.7);
        def(b, "iron_sand", "cloud_radius", 2.2, 2.2);
        def(b, "iron_sand", "cloud_thick", 0.75, 0.75);
        def(b, "iron_sand", "cloud_density", 16, 16);
        def(b, "iron_sand", "guard_radius", 1.15, 1.15);
        def(b, "iron_sand", "guard_half_h", 1.15, 1.15);
        def(b, "iron_sand", "guard_density", 3, 3);
        def(b, "iron_sand", "whip_density", 24, 24);
        def(b, "iron_sand", "whip_chase", 0.28, 0.28);
        def(b, "iron_sand", "whip_spin", 0.22, 0.22);
        def(b, "iron_sand", "whip_sag", 0.45, 0.45);
        def(b, "iron_sand", "whip_hit", 14, 14);
        def(b, "iron_sand", "whip_turns", 720, 720);
        def(b, "iron_sand", "whip_coil", 0, 0);
        def(b, "iron_sand", "orbit_near", 5.0, 5.0);
        def(b, "iron_sand", "orbit_far", 8.0, 8.0);
        def(b, "iron_sand", "orbit_low", 5.0, 5.0);
        def(b, "iron_sand", "orbit_high", 8.0, 8.0);
        def(b, "iron_sand", "orbit_blend", 0.7, 0.7);
        def(b, "iron_sand", "whip_pierce", 5, 5);
        def(b, "iron_sand", "whip_tail_bias", 3.0, 3.0);
        def(b, "iron_sand", "whip_trail", 0.15, 0.15);
        def(b, "iron_sand", "whip_tip_spread", 0.30, 0.30);
        def(b, "iron_sand", "deflect_ticks", 20, 20);
        def(b, "iron_sand", "deflect_dist", 9.0, 9.0);
        def(b, "iron_sand", "whip_wobble", 5.5, 5.5);
        def(b, "iron_sand", "arc_interval", 3, 3);
        def(b, "iron_sand", "arc_whip_interval", 4, 4);
        def(b, "iron_sand", "arc_life", 3, 3);
        def(b, "iron_sand", "arc_len", 1.1, 1.1);
        def(b, "iron_sand", "whip_budget", 240, 240);

        def(b, "iron_sand", "whip_damage", 12, 40);
        def(b, "iron_sand", "whip_cd", 12, 5);
        def(b, "iron_sand", "whip_range", 8, 18);
        def(b, "iron_sand", "whip_release", 30, 30);
        def(b, "iron_sand", "whip_touch", 0.35, 0.35);
        def(b, "iron_sand", "guard_reduce", 0.08, 0.80);
        b.pop();
        skill(b, "threatening_teleport", " 危险传送(传送使):每发的消耗;冷却单位 tick;damage=命中伤害",
                35, 100, 18, 10, 30.0, 15.0, 3, 6);
        skill(b, "penetrate_teleport", " 穿透传送(传送使):cp=每格距离扣多少(总消耗=距离×它);过载=每发;冷却单位 tick", 14, 9, 80, 50, 50.0, 30.0);
        skill(b, "location_teleport", " 定位传送(传送使):cp=基础值(实际=基础×距离系数,跨维再×2);过载=每发;冷却单位 tick", 200, 150, 240, 240, 30.0, 20.0);
        skill(b, "mark_teleport", " 标记传送(传送使):cp=每格距离扣多少(总消耗=距离×它);过载=每发;冷却单位 tick", 12, 4, 40, 20, 30.0, 0.0);

        b.comment(
                " 转移传送(传送使):锁定目标再把它传送走。",
                " cp / overload = 每发扣一次,锁定挂机不消耗。cooldown = 冷却(tick)。",
                " damage = 把目标转移走时它受到的伤害。",
                " needle_damage = 钢针围刺每根钢针的伤害。伤害逐根叠加,手上有几根就扎几根,",
                "    总伤害 = 每根 × 根数(一组 64 根约 192 到 384)。嫌强嫌弱都改这里。")
                .push("shift_tp");
        def(b, "shift_tp", "exp_rate", 2.0, 0.4);
        def(b, "shift_tp", "cp", 260, 320);
        def(b, "shift_tp", "overload", 40, 30);
        def(b, "shift_tp", "cooldown", 100, 60);
        def(b, "shift_tp", "damage", 15, 35);
        def(b, "shift_tp", "needle_damage", 3, 6);
        b.pop();

        b.comment(
                " 高速闪现(传送使):进入闪现模式,之后每次按键瞬移一段。",
                " cp = 每次闪现扣。overload = 开启时扣一次,开启期间钉住不回落。",
                " cp_start = 开启瞬间额外扣的 CP。distance = 每次闪现多远(格)。",
                " cooldown = 退出闪现模式后的冷却(tick)。",
                " 闪现模式不限时,只在 CP 不够再闪一次、过载、再按一次技能键或关闭超能力时结束。")
                .push("flashing");
        def(b, "flashing", "exp_rate", 2.0, 0.4);
        def(b, "flashing", "cp", 13, 6);
        def(b, "flashing", "overload", 250, 180);
        def(b, "flashing", "cooldown", 900, 400);
        def(b, "flashing", "cp_start", 80, 60);
        def(b, "flashing", "distance", 12, 18);
        b.pop();
        skill(b, "dir_shock", " 定向冲力(矢量操作):每拳的消耗;冷却单位 tick(只有打中才进冷却);damage=命中伤害",
                50, 100, 18, 12, 60.0, 20.0, 7, 15);
        skill(b, "ground_shock", " 踏击导向(矢量操作):每次跺地的消耗;冷却单位 tick;damage=命中伤害",
                80, 150, 15, 10, 80.0, 40.0, 4, 6);
        skill(b, "dir_blast", " 集束冲击(矢量操作):每拳的消耗;冷却单位 tick(打空也进冷却);damage=命中伤害",
                160, 200, 50, 30, 80.0, 50.0, 10, 25);
        skill(b, "vec_accel", " 矢量加速(矢量操作):每次弹射的消耗;冷却单位 tick", 120, 80, 30, 15, 80.0, 50.0);

        b.comment(
                " 风之翼(矢量操作):开关式飞行。",
                " cp / overload 是每 tick 的消耗,不是每次施放,飞一秒等于 20 份。",
                " 两个都填 0 时技能会整个跳过消耗结算,CP 也照常回复。默认就是 0,开着飞不花钱。",
                " cooldown = 关闭之后的冷却(tick)。charge_time = 起飞前要蓄多久(tick)。",
                " speed = 飞行速度(格每 tick)。熟练度不到 45% 时只有它的七成。")
                .push("storm_wing");
        def(b, "storm_wing", "exp_rate", 2.0, 0.4);
        def(b, "storm_wing", "cp", 0, 0);
        def(b, "storm_wing", "overload", 0, 0);
        def(b, "storm_wing", "cooldown", 30, 10);
        def(b, "storm_wing", "charge_time", 70, 30);
        def(b, "storm_wing", "speed", 2.0, 3.0);

        b.pop();

        b.comment(
                " 矢量偏移(矢量操作,含矢量反射两种模式):开关式,没有冷却。",
                " 开着不花钱,代价按弹开了几发算。",
                " cp / overload = 开启瞬间扣一次。",
                " 投射物的实际消耗还要乘它的难度系数(雪球 0.1,药水 1.4,其余 1.0)。",
                " ray_cp / ray_overload = 挡下一发射线类攻击的价,再乘该技能声明的难度系数",
                "    (1.0 相当于一发超电磁炮),所以挡电弧比挡超电磁炮便宜得多。",
                " melee_cp / melee_overload = 挡下近战时每 1 点伤害要付的 CP 与过载。",
                " melee_reflect_ratio = 近战被弹回去时打回多少比例的伤害。",
                "    只要成功反弹自己就完全不受伤,这个倍率只管打回多少。",
                " reflect_budget = 每 tick 的计费上限:一 tick 内最多按单发价乘本值收费。",
                "    这道闸只管消耗、不管功能,超出的照样弹开、照样免伤,只是不再扣钱。",
                " reflect_speed = 把投射物弹出去时重新给足的初速度(3.0 相当于满蓄力弓)。")
                .push("vec_deviation");
        def(b, "vec_deviation", "exp_rate", 2.0, 0.4);
        def(b, "vec_deviation", "cp", 300, 160);
        def(b, "vec_deviation", "overload", 20, 12);

        def(b, "vec_deviation", "reflect_speed", 3.0, 3.0);

        def(b, "vec_deviation", "ray_cp", 800, 500);
        def(b, "vec_deviation", "ray_overload", 60, 40);

        def(b, "vec_deviation", "melee_cp", 20, 15);
        def(b, "vec_deviation", "melee_overload", 1.5, 1.0);

        def(b, "vec_deviation", "block_cp", 10, 6);
        def(b, "vec_deviation", "block_overload", 0.8, 0.5);

        def(b, "vec_deviation", "scatter_range", 8, 16);

        def(b, "vec_deviation", "sonic_lock_range", 100, 100);

        def(b, "vec_deviation", "melee_reflect_ratio", 0.6, 1.2);

        def(b, "vec_deviation", "reflect_budget", 5.0, 5.0);
        b.pop();

        b.comment(
                " 电浆炮(矢量操作,Lv5):头顶凝出一颗等离子球,松手掷向瞄准点炸开。",
                " cp = 蓄力期间每 tick 扣,蓄满一次共 cp 乘 charge_time。",
                " overload = 按下那一刻扣一次,整个施放期间钉住不回落。",
                " cooldown = 冷却(tick)。charge_time = 要蓄多少 tick。speed = 球每 tick 飞多远(格)。",
                " radius = 作用半径(格):爆心这么远之内的实体挨 damage 伤害,同时也是爆炸威力。",
                " damage = 作用半径内每个实体挨的伤害,并会清掉无敌帧好让紧接着的爆炸也打满。",
                " power_max = 超蓄倍率上限。蓄满后继续按着,每多蓄一个 charge_time 涨一倍,",
                "    伤害、radius、飞行速度都按倍率放大,CP 也照倍率多烧,涨到顶就不再扣。",
                " explosion_radius_cap = 爆炸半径的硬上限。别随便调大:vanilla 爆炸的开销约是半径的",
                "    三次方,半径 15 尚可,60 以上服务端基本会卡死。超出上限只截爆炸,",
                "    伤害与判定范围不受此限。方块破坏还受「技能破坏方块」总闸管,关掉就只剩音效和粒子。")
                .push("plasma_cannon");
        def(b, "plasma_cannon", "exp_rate", 2.0, 0.4);
        def(b, "plasma_cannon", "cp", 18, 25);
        def(b, "plasma_cannon", "overload", 100, 50);
        def(b, "plasma_cannon", "cooldown", 1000, 600);
        def(b, "plasma_cannon", "charge_time", 60, 30);
        def(b, "plasma_cannon", "damage", 80, 150);

        def(b, "plasma_cannon", "radius", 10.0, 10.0);
        def(b, "plasma_cannon", "speed", 1.0, 1.0);

        def(b, "plasma_cannon", "power_max", 3.0, 3.0);
        def(b, "plasma_cannon", "explosion_radius_cap", 30.0, 30.0);
        b.pop();

        b.comment(
                " 黑白双翼(矢量操作·进阶):风之翼的进阶形态。开着时常驻一层「矢量偏移:反射」,",
                "   技能预设被暂时拦截,只剩三个鼠标键 —— 中键切黑/白翼、左键开关重力压制场、按住右键进尖锐形态。",
                " cp / overload 是**每 tick**的挂机消耗(开着不动也在烧),两个都填 0 就完全免费。",
                " cooldown = 关闭之后的冷却(tick)。",
                " crush_* = 左键的**重力压制场**(开关式:按一下开、再按一下关):",
                "   range 半径(格,默认 24)、damage 每次窒息伤害(默认 25)、hurt_every 每几 tick 一次(默认 2 = 0.1 秒)、",
                "   gravity 每 tick 往下叠多少速度(**加速度**,不是定值)、max_fall 终端速度上限(格/tick)、",
                "   slow 每 tick 水平速度**保留**的比例(越小越难动,默认 0.35)、",
                "   slow_amp 附带的缓慢等级(默认 4 ⇒ 只剩 25% 速度;填到 6 以上会把速度打成负数)、",
                "   scan_every 每几 tick 重扫一次范围、cp/overload 是**每 tick**的消耗。",
                "   伤害走 `academy:asphyxiation`:**无视护甲/附魔/抗性提升/无敌帧**,25 点就是实打实 25 点。",
                "   判据是「range 格内**所有生物**」(玩家那部分走 PvP/队伍闸)—— 村民、动物、你自己的狼都会被压。",
                "   **DPS = damage × 20 / hurt_every**:默认 25/2 tick = **250**,而且是对范围内每一个目标。",
                "   hurt_every 调到 10 以下时:本技能**不需要**清无敌帧(asphyxiation 自带 bypasses_cooldown),",
                "     但 vanilla 仍会把目标的 `lastHurt` 顶在 damage 上 ⇒ **别人的攻击要打过这个数才有效果**,",
                "     且只结算超出的部分。范围内的怪会变得很难被队友/你自己的普攻打动。",
                "   **特效的生成频率与伤害频率无关**(固定 0.5 秒一条,见 CrushFieldFx.FOE_EVERY)。",
                "   scan_every 是**性能旋钮不是玩法数值**:24 格的判定盒是 4×4 区块的全高度遍历,",
                "     调小更跟手但更费;调大省性能,代价是新进范围的生物最多晚这么多 tick 才吃到压制。",
                " fly_* = **飞行**(黑翼白翼都有)。飞行**不是常驻**的:平时正常走路,",
                "   **双击空格**进入飞行(同创造模式那两下)、**再双击**急降并退出。",
                "   speed 飞行速度(格/tick,默认 2.1→5.4 = 比风之翼快 50%)、",
                "   dash_speed / dash_ticks 是**起飞喷射**的上升速度与持续 tick(相乘 ≈ 冲多高)、",
                "   drop_speed 是**急降**的下降速度(落地即止,期间不摔伤)。",
                "   手感是**二值**的:按住立刻全速、松手立刻停 —— 没有加速过程也没有滑行",
                "     (vanilla 的推进与惯性全部绕开了,速度由技能每 tick 直接给)。",
                "   方向**跟视线**:抬头按前进就是爬升、低头就是俯冲;空格/shift 另给垂直升降。",
                "   speed **别调过 10**:vanilla 服务端的「moved too quickly」判定会把你拉回来",
                "     (默认 5.4 还有约 46% 余量)。想更快得先动服务端那道校验,不是改这里。",
                " sharp_mul = 按住右键(尖锐形态)时羽翼伤害的倍率。默认 1.5 = 用户要的「额外提高 50%」。",
                "   它**同时乘**压制场(crush_damage)和右键压制(press_damage)。",
                " gust_aim_wide = 右键压制的准星判定在原版那 0.3 膨胀之外再放宽多少格。给 0 = 与拓宽前一致。",
                " press_* = 按住右键时,准星有敌人则双翼各伸一条风把他缠成一个球、一路往下压:",
                "   range 锁定射程、damage 每次伤害(默认 15)、hurt_every 每几 tick 一次(默认 2 = 0.1 秒)、",
                "   gravity 每 tick 往下叠多少速度(**加速度**,不是定值)、max_fall 终端速度上限(格/tick)、",
                "   fall_mul 被压期间落地的摔落伤害倍率(默认 2 = 翻倍)、cp/overload 是**每 tick**的消耗。",
                "   hurt_every 调到 10 以下时,本技能会**清掉目标的无敌帧**才打得出这个频率;",
                "     那一瞬间别人的攻击也能打进去(无敌帧是全局的)。不想要就把 hurt_every 调回 10 以上。",
                "   **DPS = damage × 20 / hurt_every**:默认 15/2 tick = **150**,而不是老版的 30。")
                .push("dual_wing");
        def(b, "dual_wing", "exp_rate", 2.0, 0.4);
        def(b, "dual_wing", "cp", 3, 2);
        def(b, "dual_wing", "overload", 0, 0);
        def(b, "dual_wing", "cooldown", 200, 100);

        def(b, "dual_wing", "gust_aim_wide", 1.5, 1.5);
        def(b, "dual_wing", "sharp_mul", 1.5, 1.5);

        def(b, "dual_wing", "crush_range", 24.0, 24.0);
        def(b, "dual_wing", "crush_damage", 25, 25);

        def(b, "dual_wing", "crush_hurt_every", 2, 2);

        def(b, "dual_wing", "crush_gravity", 0.14, 0.14);
        def(b, "dual_wing", "crush_max_fall", 3.0, 3.0);

        def(b, "dual_wing", "crush_slow", 0.35, 0.35);
        def(b, "dual_wing", "crush_slow_amp", 4, 4);
        def(b, "dual_wing", "crush_scan_every", 5, 5);
        def(b, "dual_wing", "crush_cp", 20, 14);
        def(b, "dual_wing", "crush_overload", 10, 7);

        def(b, "dual_wing", "fly_speed", 2.1, 5.4);

        def(b, "dual_wing", "fly_dash_speed", 0.9, 0.9);
        def(b, "dual_wing", "fly_dash_ticks", 5, 5);

        def(b, "dual_wing", "fly_drop_speed", 1.4, 1.4);

        def(b, "dual_wing", "press_range", 20.0, 20.0);
        def(b, "dual_wing", "press_damage", 15, 15);

        def(b, "dual_wing", "press_hurt_every", 2, 2);

        def(b, "dual_wing", "press_gravity", 0.14, 0.14);

        def(b, "dual_wing", "press_max_fall", 3.0, 3.0);
        def(b, "dual_wing", "press_fall_mul", 2.0, 2.0);
        def(b, "dual_wing", "press_cp", 12, 8);
        def(b, "dual_wing", "press_overload", 6, 4);

        def(b, "dual_wing", "guard_range", 30.0, 30.0);

        def(b, "dual_wing", "guard_cp_boost", 0.5, 0.5);

        def(b, "dual_wing", "guard_block_cp", 12, 8);
        def(b, "dual_wing", "guard_heal_cp", 8, 5);
        def(b, "dual_wing", "guard_overload", 1.5, 1.0);

        def(b, "dual_wing", "guard_cooldown", 20, 20);

        def(b, "dual_wing", "life_vector_max", 999, 999);
        def(b, "dual_wing", "life_vector_cp", 12, 8);

        def(b, "dual_wing", "immortal_cp", 1.0, 1.0);

        def(b, "dual_wing", "feather_range", 30.0, 30.0);
        def(b, "dual_wing", "feather_height", 10.0, 10.0);
        def(b, "dual_wing", "feather_life", 600, 600);
        def(b, "dual_wing", "feather_damage", 100, 100);

        def(b, "dual_wing", "feather_hurt_every", 2, 2);

        def(b, "dual_wing", "feather_per_batch", 12, 12);
        def(b, "dual_wing", "feather_spawn_every", 2, 2);

        def(b, "dual_wing", "feather_cp", 24, 17);
        def(b, "dual_wing", "feather_overload", 12, 8);

        def(b, "dual_wing", "feather_empower_spawn_mul", 2.0, 2.0);
        def(b, "dual_wing", "feather_empower_fall_mul", 2.0, 2.0);

        def(b, "dual_wing", "feather_aim_delay", 60, 60);

        def(b, "dual_wing", "feather_launch_speed", 1.2, 1.2);

        def(b, "dual_wing", "feather_aim_range", 36.0, 36.0);

        def(b, "dual_wing", "feather_shot_cost", 200, 200);
        b.pop();

        b.pop();

        b.comment(
                " 原子崩坏(meltdowner)各技能的数值。")
                .push("meltdowner");
        b.comment(
                " 电子光束(原子崩坏,Lv1):按一下凝出能量球,蓄能后射出细光柱打向准星。",
                " 第一次施放会开一个 burst_window 长的窗口,窗口内可以随便连点;每多放一发,",
                " 下一发的 cp 按 cp_step 线性累加,窗口到点才统一进冷却并重置倍率。",
                " cp / cooldown / burst_window 单位 tick。cp_step = 每多放一发的 cp 倍率增量。",
                " damage = 命中生物的伤害。射程固定 15 格。",
                " overload = 起手一次性扣的过载,整个施放期间钉住不回落。")
                .push("electron_bomb");
        def(b, "electron_bomb", "exp_rate", 2.0, 0.4);
        def(b, "electron_bomb", "cp", 30, 20);
        def(b, "electron_bomb", "overload", 0, 0);
        def(b, "electron_bomb", "cooldown", 20, 20);
        def(b, "electron_bomb", "damage", 6, 12);
        def(b, "electron_bomb", "burst_window", 50, 50);
        def(b, "electron_bomb", "cp_step", 0.15, 0.15);
        b.pop();
        b.comment(
                " 光盾(原子崩坏,Lv2):按住展开挡在身前的漩涡圆盘,只挡正面正负 60 度。",
                " cp = 每 tick 的维持消耗,不是每次施放。overload = 起手一次性过载,之后钉住不回落。",
                " max_time = 最长维持多少 tick。",
                " block_cp / block_overload = 挡下攻击时每 1 点伤害要付的 CP 与过载。",
                "    非光束格挡、光束折弯、正面撞伤三条路同一口径,付不起就是这一下没挡住",
                "    (伤害照常落下,技能不会被顶断)。挡一发超电磁炮约 480 CP、24 过载。",
                " touch_damage = 正面 3 格内的活物每 tick 被撞掉多少血,按同一口径付费。",
                " cooldown_mult = 冷却倍率,冷却等于撑了多少 tick 乘它,撑越久冷却越长,",
                "    熟练度越高倍率越低。收盾还会给自己缓慢 II 五秒。")
                .push("light_shield");
        def(b, "light_shield", "exp_rate", 2.0, 0.4);
        def(b, "light_shield", "cp", 9, 4);
        def(b, "light_shield", "overload", 110, 60);
        def(b, "light_shield", "max_time", 120, 180);
        def(b, "light_shield", "block_cp", 8, 3);
        def(b, "light_shield", "block_overload", 0.4, 0.15);
        def(b, "light_shield", "touch_damage", 2, 6);
        def(b, "light_shield", "cooldown_mult", 2.0, 1.0);
        b.pop();
        b.comment(
                " 电子弹散射(原子崩坏,Lv2):按住蓄力,身周不断凝出电子球;松手,每颗球射一道细光柱。",
                " ball_delay = 起手等多少 tick 出第一颗。ball_interval = 之后每隔多少 tick 一颗,",
                "    熟练度越高越快。charge_max = 最多凝到第几 tick 为止,满蓄约 13 到 20 颗。",
                " CP 在松手那一刻一次性结算,第 i 颗(从 0 数)单价 = cp 乘 (1 + cp_step 乘 i)。",
                "    默认满蓄一次约 270 到 430 CP。CP 不够不作废整次施放:扣得动几颗就射几颗。",
                " overload = 起手一次性扣,整个施放期间钉住不回落。",
                " 球生成在自身周围的均匀球面上,不只在身前。",
                " 准星附近有没有敌人,决定光柱往哪射:",
                "    aim_range = 从眼睛沿视线找多远(格,撞方块即止);",
                "    aim_radius = 离视线多近才算准星附近(格,垂距),取最贴准星的那个,隔墙的不算;",
                "    spread = 锁定目标后每颗球的随机偏差角度(度);",
                "    没找到目标时,每颗球沿自己相对眼位的方向射出去。",
                " damage = 每一道光柱命中生物的伤害,射程固定 15 格。齐射会清掉无敌帧,",
                "    否则只有第一道扣血。满蓄一次对单体约 65 到 180 点。",
                " hold_ticks / hold_damage = 按住不放到第几 tick 就自伤多少并强制结算。",
                "    蓄力期不烧 CP,这是唯一阻止无限期挂机蓄力的闸,别调成 0。",
                " cooldown = 冷却(tick),默认 0 即没有冷却,收敛靠 CP 递增与起手过载。")
                .push("scatter_bomb");
        def(b, "scatter_bomb", "exp_rate", 2.0, 0.4);
        def(b, "scatter_bomb", "cp", 12, 10);
        def(b, "scatter_bomb", "overload", 80, 60);
        def(b, "scatter_bomb", "cooldown", 0, 0);
        def(b, "scatter_bomb", "damage", 5, 9);
        def(b, "scatter_bomb", "cp_step", 0.12, 0.12);
        def(b, "scatter_bomb", "ball_delay", 4, 4);
        def(b, "scatter_bomb", "ball_interval", 6, 4);
        def(b, "scatter_bomb", "charge_max", 80, 80);
        def(b, "scatter_bomb", "spread", 12.5, 12.5);
        def(b, "scatter_bomb", "aim_range", 20, 20);
        def(b, "scatter_bomb", "aim_radius", 4.0, 4.0);
        def(b, "scatter_bomb", "hold_ticks", 200, 200);
        def(b, "scatter_bomb", "hold_damage", 6, 6);
        b.pop();
        b.comment(
                " 粒机波形炮(原子崩坏,Lv3):按住蓄力,松手放一道贯穿式粗光柱。",
                " cp = 蓄力期间每 tick 扣,不是每发。overload = 按下那一刻扣一次并在蓄力期钉住不回落。",
                " 蓄力 20 tick 才算数(不足松手即取消),40 tick 封顶;蓄力倍率 0.8 到 1.2。",
                " damage / energy / cooldown 三个都乘这个倍率,蓄得越久越强,但冷却也越长。",
                " damage = 束内实体挨的伤害。energy = 凿方块的总能量,比超电磁炮浅但判定更粗。",
                " range = 柱状判定半径(格)。length = 光柱长度(格)。cooldown = 冷却基数(tick)。",
                " reflect_damage = 被矢量偏移弹开后,二次射线打中生物的伤害。",
                "    光盾的折弯不走这条,那是同一道光拐弯继续跑,伤害仍是 damage。")
                .push("meltdowner");
        def(b, "meltdowner", "exp_rate", 2.0, 0.4);
        def(b, "meltdowner", "cp", 10, 15);
        def(b, "meltdowner", "overload", 200, 170);
        def(b, "meltdowner", "cooldown", 300, 140);
        def(b, "meltdowner", "damage", 18, 50);
        def(b, "meltdowner", "energy", 300, 700);
        def(b, "meltdowner", "range", 2.0, 3.0);
        def(b, "meltdowner", "length", 30, 30);
        def(b, "meltdowner", "reflect_damage", 10, 25);
        b.pop();
        b.comment(
                " 散射光束雨(原子崩坏,Lv4,被动):学会之后,你自己的任何原子崩坏系光束",
                " 打中空中的扩散支援半导体(西尔巴恩)时,那道光就在命中点散开成一片光束雨。",
                " 电子光束、电子弹散射、粒机波形炮都能引爆。媒介是必需的:打中半导体才散。",
                " 获取媒介:金属处理机 ETCH,晶圆一个换西尔巴恩一个。",
                " range = 散射光的射程(格)。它同时是伤害锥的射程和特效长度,",
                "    两者共用一个数,免得看见的和打到的对不上。",
                " hit_radius = 光束离半导体多近才算打中(格,垂距)。调大更好瞄,调小更考验准头。",
                " flicker_interval = 每隔多少 tick 闪一次并重打一遍判定(5 = 每秒 4 次)。",
                " flicker_ticks = 一共闪多久,默认一次散射共 10 下。",
                "    每一遍都会清掉目标的无敌帧,否则 20 tick 无敌会把后面所有闪烁全吃掉。",
                " ray_damage = 每一道光命中一次的伤害。命中道数随距离锐减:",
                "    贴脸约 10 道,3 格约 4 道,5 格约 1.7 道,20 格约 0.1 道。",
                "    所以一整发默认约:贴脸 320 到 650,3 格 130 到 270,20 格 3 到 7。",
                "    贴脸猛是设计使然,嫌离谱就调低本值。",
                " 扇面不在这里配:水平半角 50 到 60 度,竖直半角是它的一半,每次散 25 到 30 道。")
                .push("ray_barrage");
        def(b, "ray_barrage", "exp_rate", 2.0, 0.4);
        def(b, "ray_barrage", "ray_damage", 3, 6);
        def(b, "ray_barrage", "range", 20, 20);
        def(b, "ray_barrage", "hit_radius", 0.8, 0.8);
        def(b, "ray_barrage", "flicker_interval", 5, 5);
        def(b, "ray_barrage", "flicker_ticks", 30, 30);
        b.pop();
        b.comment(
                " 突击喷射(原子崩坏,Lv4):按住蓄力瞄准,绿色涟漪从脚边往外推,按多久冲多远;",
                " 松手把电子当火箭喷出去,靠反作用力冲过去,途中撞到的活物统统挨一下。",
                " charge_speed = 蓄力期每 tick 往外涨多少格,熟练度越高蓄得越快。",
                " distance = 蓄满时的最远瞄准距离(格)。默认满蓄要 10 到 15 tick,约半秒。",
                " charge_min = 至少蓄几 tick 才算数,不够就是取消,一分钱不收也不进冷却。",
                "    别调成 0,那会退回点一下原地放空的手感。",
                " cp / overload = 松手那一刻一次性扣,蓄力阶段不烧,只是撑不起就断开。",
                " cooldown = 冷却(tick)。",
                " damage = 冲刺途中撞到的每个活物挨的伤害。每人整次冲刺只挨一下,",
                "    按身体扫过谁算,不是每 tick 重打。",
                "    实际落点会沿脚位到瞄准点的线段逐段试放包围盒,撞墙就停在墙前,不会把人塞进方块。",
                " shield_ticks = 冲刺开始后多少 tick 内完全免伤,连击退一起免。填 0 则盾只剩好看。")
                .push("jet_engine");
        def(b, "jet_engine", "exp_rate", 2.0, 0.4);
        def(b, "jet_engine", "cp", 170, 140);
        def(b, "jet_engine", "overload", 60, 50);
        def(b, "jet_engine", "cooldown", 60, 30);
        def(b, "jet_engine", "damage", 7, 20);
        def(b, "jet_engine", "distance", 12, 12);
        def(b, "jet_engine", "charge_speed", 0.8, 1.2);
        def(b, "jet_engine", "charge_min", 5, 5);
        def(b, "jet_engine", "shield_ticks", 15, 15);
        b.pop();
        b.comment(
                " 巡航光束炮(原子崩坏,Lv5):开关式。周身凝一团电子光点,附近出现目标就自动",
                " 抽一颗朝它射出去,并自动往那个方向丢一块西尔巴恩,于是后续光束打中它就炸成",
                " 一片光束雨,与被动散射光束雨串成链。点一下开、再点一下关,没有时限。",
                " cp = 每凝一颗光点扣。fire_cp = 每开一发扣。两条计价各自按 cp_step 递增,",
                "    两条计数互相独立、关掉技能归零。默认发到第 10 发约两倍、第 20 发约四倍,",
                "    收敛全靠它,别把 cp_step 调成 0。",
                " overload = 只由开火产生,而且熟练度超过 overload_exp 之后完全不涨。",
                " gen_interval = 每隔多少 tick 凝一颗光点,随熟练度变快。",
                " gen_max / gen_max_step = 同时最多挂几颗光点,等于基数加上熟练度除以 gen_max_step。",
                "    gen_max_step 别填 0,否则上限会爆成无穷(代码里有兜底)。",
                " fire_cd = 两发之间至少隔几 tick。它只在攒够存货时才真正当闸,",
                "    想让射速跟上就把 gen_interval 调小;嫌费就调大这两个之一。",
                " range = 索敌半径(格)。damage = 每发命中的伤害。同一目标会被连打几发,",
                "    已按规矩清掉无敌帧,否则只有第一发扣血。",
                " throw_ahead = 自动丢出的半导体落在束轴上、光点前方多少格。",
                "    它必须落在束轴上:若像手动投掷那样生成在眼位,而光束起点是眼位加光点偏移,",
                "    就只有光点恰在目标反方向时光束才擦得到它,一半几率贴脸炸、一半几率打不到。",
                "    贴脸炸不会伤到自己,这个数只影响散射锥从多远张开。",
                " cooldown = 关掉技能后的冷却(tick)。",
                " 索敌模式不在这里配:技能开着时按鼠标中键在只打敌对和所有人之间切。")
                .push("electron_missile");
        def(b, "electron_missile", "exp_rate", 2.0, 0.4);
        def(b, "electron_missile", "cp", 12, 5);
        def(b, "electron_missile", "fire_cp", 60, 25);
        def(b, "electron_missile", "cp_step", 0.08, 0.08);
        def(b, "electron_missile", "overload", 9, 4);
        def(b, "electron_missile", "overload_exp", 0.3, 0.3);
        def(b, "electron_missile", "cooldown", 700, 400);
        def(b, "electron_missile", "damage", 10, 18);
        def(b, "electron_missile", "range", 8, 13);
        def(b, "electron_missile", "gen_interval", 10, 4);
        def(b, "electron_missile", "gen_max", 8, 8);
        def(b, "electron_missile", "gen_max_step", 0.08, 0.08);
        def(b, "electron_missile", "fire_cd", 5, 5);
        def(b, "electron_missile", "throw_ahead", 3.0, 3.0);
        b.pop();
        b.pop();

        b.comment(
                " 印象操作(心理掌握,Lv1):准星锁定一只生物,扭曲它对【你】的认知。",
                " 本来敌对你的,变得不再针对你;本来友善的,变得只针对你。",
                " 效果**只在它和你之间**成立:对其他玩家它的态度一点不变。",
                " cp / overload = 每次施放扣一次。cooldown = 冷却(tick,20 = 1 秒)。",
                " duration = 效果持续几 tick。默认 160(8 秒)到 600(30 秒),随熟练度变长。",
                " range = 准星锁定距离(格)。隔着方块锁不到。",
                " ",
                " 下面三项**只作用于「本来根本不会打人的生物」**(牛、羊、猪、鸡这类):",
                " 它们被翻成敌对之后,vanilla 没有给它们任何攻击手段,所以由本 mod 代为驱动。",
                " 僵尸、骷髅、狼、铁傀儡这些自带近战 AI 的生物**不吃这三项**,它们照自己的伤害打。",
                " hostile_damage = 代驱动的那一下打多少伤害。",
                " hostile_interval = 两次之间至少隔几 tick(20 = 1 秒)。",
                " chase_speed = 追击移动速度倍率(1.0 = 该生物的正常速度)。",
                " 这三项与熟练度无关 → 两个端点必须填一样,否则同一只牛在不同人手里打出不同伤害。")
                .push("impression");
        def(b, "impression", "exp_rate", 2.0, 0.4);
        def(b, "impression", "cp", 120, 80);
        def(b, "impression", "overload", 40, 25);
        def(b, "impression", "cooldown", 200, 100);

        def(b, "impression", "duration", 160, 600);
        def(b, "impression", "range", 12, 20);
        def(b, "impression", "hostile_damage", 2, 2);
        def(b, "impression", "hostile_interval", 20, 20);
        def(b, "impression", "chase_speed", 1.1, 1.1);

        IMPRESSION_NO_DARKNESS = b.comment(
                        " 允许是否让监守者的黑暗受阵营限制。")
                .define("block_warden_darkness", true);
        b.pop();

        b.comment(
                " 痛觉剔除(心理掌握,Lv1,被动):减轻【你自己】受到的痛觉。",
                " reduction = 减伤比例。默认 0.08(8%)到 0.5(50%),随熟练度变多。",
                " 它与护甲、抗性提升、保护附魔都是**相乘**的(减伤挂在护甲之后那一层):",
                "   0.5 配一身钻甲(≈80%)总减伤到 90%,生存力翻倍。往上调之前先把这条算进去。",
                " 掉出世界 / `/kill` 那一组伤害**不减**(vanilla 里它们连无敌帧都绕过,",
                "   不是「痛」而是判定死亡的手段)。这条写死在代码里,不受本节影响。",
                " ",
                " 熟练度靠挨打练,下面两项管它涨多快:",
                " exp_per_hit  = 每受到一次伤害涨多少熟练度(1.0 = 从零直接练满)。默认 0.001 = 0.1%。",
                " exp_interval = 两次涨熟练度之间至少隔几 tick(20 = 1 秒)。",
                "   这个节流**不能填 0**:岩浆/窒息/凋零是**每 tick** 都造成一次伤害的,",
                "     不节流泡一次岩浆就能把熟练度拉满。",
                " 这两项与熟练度无关 → 两个端点必须填一样,否则会变成「越练越快」的正反馈。")
                .push("pain_cutoff");
        def(b, "pain_cutoff", "exp_rate", 2.0, 0.4);
        def(b, "pain_cutoff", "reduction", 0.08, 0.5);
        def(b, "pain_cutoff", "exp_per_hit", 0.001, 0.001);
        def(b, "pain_cutoff", "exp_interval", 20, 20);
        b.pop();

        b.comment(
                " 呆然自失(心理掌握,Lv1):准星锁定一只生物,让它在一段时间里感觉不到时间流逝。",
                " 效果 = 那只生物的 tick 整个停住:AI 不跑、动画定格在那一帧、不受重力、",
                "   身上的状态效果与燃烧计时也一并冻结。**但它照样能被打**(无敌帧由 mod 代为流逝)。",
                " duration = 定格几 tick。默认 160(8 秒)到 600(30 秒),随熟练度变长。",
                " range = 准星锁定距离(格)。隔着方块锁不到。",
                " cp / overload = 每次施放扣一次。cooldown = 冷却(tick,20 = 1 秒)。",
                " 这三项是移植方定的初值(原版没有本系,没有「与原版一致」这回事)——",
                "   照印象操作大约贵 1.6 倍。觉得强/弱就在这里调。",
                " duration 与 cooldown 的关系值得留意:满熟练度时定格 30 秒、冷却 10 秒,",
                "   意味着一只怪可以被永久锁死。不想要这个就把 cooldown 抬到 duration 以上。")
                .push("daze");
        def(b, "daze", "exp_rate", 2.0, 0.4);
        def(b, "daze", "cp", 200, 140);
        def(b, "daze", "overload", 60, 40);
        def(b, "daze", "cooldown", 400, 200);

        def(b, "daze", "duration", 160, 600);
        def(b, "daze", "range", 12, 20);
        b.pop();

        b.comment(
                " 气绝昏倒(心理掌握,Lv2):准星锁定一只生物或玩家,让它无法呼吸并昏倒在地。",
                " 效果 = 【无法操控自身】+ 【周期性窒息伤害】+ 【黑暗 debuff】+ 模型趴在地上。",
                " **它不时停**(与呆然自失的区别):目标照常 tick —— 动画照播、状态效果照倒计时、",
                "   重力与无敌帧照常,只是自己动不了(生物 AI 全停,玩家移动/背包/技能键全失效)。",
                "   两个技能可以同时挂在一个目标身上,各走各的 —— **定格期间窒息照样掉血**",
                "   (窒息是生理过程,不看当事人的主观时间)。叠加只会更惨,不会互相削弱。",
                " ",
                " duration = 持续几 tick。用户拍板**固定 8 秒(160)**,两个端点填成一样 ——",
                "   熟练度只让伤害变密,不延长时间。要改成随熟练度变长,把第二个数调大即可。",
                " damage = 每次窒息掉多少血(2 = 1 颗心)。",
                " interval = 两次窒息相隔几 tick。默认 20(1 秒)到 10(0.5 秒),随熟练度变快。",
                "   于是总伤害 = duration / interval × damage:默认 8 次共 16 点,练满 16 次共 32 点。",
                " **这伤害无视护甲、抗性提升、保护附魔、无敌帧,以及本 mod 的痛觉剔除**",
                "   (用户拍板「无视抗性和伤害减免」)。所以 damage 看着小,实际比同数值的普通伤害狠得多 ——",
                "   往上调之前先把这条算进去。它不无视创造模式/观察者(那是另一回事)。",
                " darkness = 附带的黑暗 debuff 时长(tick),0 = 不给。效果与监守者给的完全一样。",
                "   黑暗**只对玩家目标有意义**(它是屏幕变暗,生物没有屏幕)。",
                " range = 准星锁定距离(格)。隔着方块锁不到。",
                " cp / overload = 每次施放扣一次。cooldown = 冷却(tick,20 = 1 秒)。",
                " 这几项是移植方定的初值(原版没有本系)—— 照呆然自失大约贵 1.5 倍。")
                .push("faint");
        def(b, "faint", "exp_rate", 2.0, 0.4);
        def(b, "faint", "cp", 300, 220);
        def(b, "faint", "overload", 90, 60);
        def(b, "faint", "cooldown", 600, 300);

        def(b, "faint", "duration", 160, 160);
        def(b, "faint", "damage", 2, 2);

        def(b, "faint", "interval", 20, 10);

        def(b, "faint", "darkness", 160, 160);
        def(b, "faint", "range", 12, 20);
        b.pop();

        b.comment(
                " 强迫自控(心理掌握,Lv3):夺走目标一部分身体权限,对它下达命令。",
                " 操作:学会后**鼠标中键**多出一个按钮 —— 短按切换命令,长按呼出轮盘;",
                "   再用技能键下达(跟随一步:选人即可;其余两步:先选人,再选要打的生物 / 要去的方块)。",
                " 命令:攻击 / 移动 / 跟随 / 巡逻 / 不动 / 停止 / 恢复。",
                "   巡逻是在【下令时它站的位置】与目的地之间来回走。",
                " 【不动】**不受 duration 约束** —— 它立刻打断目标的一切行为并原地站定,",
                "   一直持续到你对它施放【停止】或【恢复】为止(重启游戏也还在)。别指望它自己到期。",
                " 【不动】还是**所有命令的根**:临时派它去打一架 / 搬个位置,干完自动站回原来那条命令,",
                "   一路弹到底就回到不动。想让它彻底自由,用【停止】。",
                " 【停止】只清掉命令本身,别的效果不碰;【恢复】才是解除你给它的一切",
                "   (强迫自控 + 呆然自失 + 气绝昏倒 + 印象操作 + 认知篡改…)。",
                "   这两条都不写状态,所以 duration 对它们没意义。",
                " ",
                " duration = 命令持续几 tick(到点自动解除)。默认 600(30 秒)到 1200(60 秒)。",
                "   **只管【移动 / 跟随 / 巡逻】三条。【攻击】与【不动】不吃它**:",
                "     攻击一直打到**目标倒下**(2026-08-05 用户拍板删掉兜底时长 —— 目标血厚时",
                "     原先会打到一半突然收手),不动一直到你对它下【停止】。这两条都有自己的完成条件,",
                "     再套一个钟只会在完成之前打断;要提前收手就下【停止】。",
                " fail_chance = **抗命概率** —— 技能描述里那句「但命令有时候会失效」就是它。",
                "   默认 0.35(35%)降到 0.05(5%),越熟练越听话。CP 照扣、冷却照走,只是命令没下去。",
                "   填 0 = 永远听话,这会让技能描述与实际不符;要改先想清楚。",
                "   想「100% 服从」有正道:学 Lv4 被动【思维操作】(前置=本技能练满),",
                "     它会把这个概率对该玩家按死成 0 —— 本项不用动。",
                " wheel_hold = 中键按住几 tick 才算「长按」(呼轮盘)。默认 8(≈0.4 秒)。",
                "   **别往小了调**:人手单击约 100~200ms,阈值落进那个区间就会偶发地把单击",
                "     误判成长按(电磁牵引那条老账:3 tick 实为 150~200ms,175ms 恰好一半概率误判)。",
                "   与熟练度无关 → 两个端点必须填一样。",
                " range = 选人 / 选目标 / 选方块的距离(格)。",
                " attack_reject = 连续这么多 tick【我们指的目标没粘住】就跳过观察窗直接接管。默认 40(2 秒)。",
                "   有些生物的目标系统会结构性地拒绝某个目标(监守者对创造玩家 / 另一只监守者 /",
                "   盔甲架 / 无敌实体一律不收),那时候等它「自己动手」纯属浪费时间。",
                " dragon_charge_time / dragon_breath_time = **末影龙专用**。它的攻击是",
                "   「冲过去」和「喷龙息」交替循环,这两项是每一段最多允许跑多久(tick)。",
                "   到点只是**换下一段**,不是放弃 —— 用来兜「目标跑掉了还在冲空气」",
                "   和「隔着山永远对不准」这两种卡死。默认 200(10 秒)/ 100(5 秒)。",
                "   调小 = 两种打法切得更勤;调大 = 每一段更完整,但卡住时也拖得更久。",
                "   两项都与熟练度无关 → 端点必须填一样。",
                " cp / overload = 每次下达扣一次(抗命也扣)。cooldown = 冷却(tick)。")
                .push("forced_control");
        def(b, "forced_control", "exp_rate", 2.0, 0.4);
        def(b, "forced_control", "cp", 260, 180);
        def(b, "forced_control", "overload", 80, 50);
        def(b, "forced_control", "cooldown", 100, 40);
        def(b, "forced_control", "duration", 600, 1200);
        def(b, "forced_control", "fail_chance", 0.35, 0.05);
        def(b, "forced_control", "wheel_hold", 8, 8);
        def(b, "forced_control", "range", 16, 24);

        def(b, "forced_control", "attack_damage", 3, 3);

        def(b, "forced_control", "attack_reject", 40, 40);

        def(b, "forced_control", "dragon_charge_time", 200, 200);
        def(b, "forced_control", "dragon_breath_time", 100, 100);
        b.pop();

        b.comment(" 思维操作(心理掌握,Lv4 被动):完全操作目标的身体,对下达的命令 100% 服从。",
                " 效果只有一条 —— 把上面【强迫自控】的 fail_chance(抗命概率)对该玩家按死成 0。",
                "   那是个 0/1 的开关,没有可调的数,所以这一段里只有熟练度这一项。",
                " ",
                " exp_ratio = **跟着强迫自控涨**:强迫自控每涨一次熟练度,本技能按这个倍率跟着涨一次。",
                "   强迫自控每下达一次命令涨 0.004(抗命也算),默认 0.5 倍即每次 0.002 → 约 500 次练满。",
                "   本技能没有别的经验来源;**强迫自控练满之后照样跟着涨** ——",
                "     那时它是这一系里唯一还在涨的技能。",
                "   与熟练度无关 → 两个端点必须填一样。填 0 = 永远停在 0%(效果不受影响)。")
                .push("mind_manip");
        def(b, "mind_manip", "exp_rate", 2.0, 0.4);
        def(b, "mind_manip", "exp_ratio", 0.5, 0.5);
        b.pop();

        b.comment(" 广域传播(心理掌握,Lv5 被动):所有主动技能都可通过【遥控器】对大范围的所有生物下达。",
                " 学会之后遥控器才能用:手持按 V 开编排界面(6 个编号 × 6 格),右击放出当前编号。",
                " 格子从左往右按【时序】执行:起始打【人群】;某一格是强迫自控则它之后的格子改打【准星目标】。",
                " ",
                " range_cap / count_cap = 界面里那两个数(范围 / 影响人数)的**上限**,随本技能熟练度成长。",
                "   名字**不能叫 range_max** —— 游戏内配置界面按【键名】翻译、不按路径,",
                "     而 range_max 已经被能力干涉器占着(显示成「最大干涉范围」)。",
                "   下限是死的(范围 2 / 人数 1),写在 RemoteData.MIN_RANGE / MIN_COUNT,不进配置 ——",
                "   它只是「别把数调到 0 让技能看起来坏了」的护栏,没有可调的意义。",
                "   玩家存的数**照原样存**,不随熟练度改;每次放出时才按当时的上限钳一次。",
                "     所以练回去(换能力清零)也不会把编排改坏,练上来又能立刻用上大数。",
                " aim_range = 准星那一道的距离(格)。整条时序**只瞄一次**、共用同一个准星目标 ——",
                "   逐格重瞄的话玩家那几毫秒的视角抖动会让前后两格打到不同的东西。",
                " cost_ratio = 每个目标按该技能**单发价**的这个倍率收 CP/过载。",
                "   **默认 0.2 = 减 80%**(用户拍板):打 5 个也才一份手放的钱,遥控器就是比手放划算。",
                "   1.0 = 打 5 个收 5 份;调高 = 惩罚无脑拉满人数。",
                "   与熟练度无关 → 两个端点必须填一样。",
                "   收费按「wideAccepts 过滤之后」的实际目标数算 —— 收了钱打不着是最难查的一类账。",
                " exp_bonus = 走遥控器时熟练度的倍率。**默认 1.5 = 多给 50%**(用户拍板)。",
                "   乘的是各技能【单发那一次】的量,不乘目标数 —— 否则范围拉满就成了刷熟练度机器。",
                "   与熟练度无关 → 两个端点必须填一样。",
                " cooldown = 右击一次之后遥控器的冷却(tick,20 = 1 秒);走的是 vanilla 物品冷却,有转圈动画。",
                " 快速切计划条:主手拿着遥控器时**按一下中键**呼出,滚轮预选,**再按一下**确定。",
                "   (2026-08-03 由长按改成短按,原先的 quick_hold 阈值项一并删掉 —— 不再有阈值。)",
                "   主手或副手拿着遥控器时,中键会被本 mod 吃掉 —— 免得创造模式下顺手把手里的",
                "     遥控器换成准星那个方块 / 生物蛋。",
                "   拿着遥控器期间强迫自控的中键(切命令 / 呼轮盘)会自动让路,换回别的物品即恢复。",
                " ",
                " exp_ratio = **跟着思维操作涨**:思维操作每涨一次熟练度,本技能按这个倍率跟着涨一次。",
                "   整条链是 强迫自控 → 思维操作 → 广域传播,三段各有各的倍率、互不影响。",
                "   默认 0.5:强迫自控每次 0.004 → 思维操作 0.002 → 本技能 0.001 → 约 1000 次练满。",
                "   与熟练度无关 → 两个端点必须填一样。填 0 = 永远停在 0%(那道门不受影响)。",
                "   用遥控器放出去的技能,**各自涨各自的熟练度**(与单发同量,且不乘目标数)——",
                "     所以编排里带强迫自控时,这条链照样在涨。",
                " ",
                " cam_budget_ms / cam_range = 遥控器界面里「我的人」那些**实时画面**的两个旋钮。",
                " cam_budget_ms = 每帧最多花多少毫秒在画面上,用完就停、下一帧接着往下轮。",
                "   **它直接换主视图的帧率**:调大 = 卡片里更流畅、主视图更卡,反之亦然。默认 8。",
                "   用预算而不是「每帧几路」,是因为一路画面的开销随场景差好几倍",
                "     (对着一面墙很便宜,对着开阔地形很贵),写死路数在某一头必然是错的。",
                "   调画面分辨率没用 —— 开销在几何与实体,不在像素。",
                " cam_range = 超过这么多格就只显示「不在信号范围内」。默认 64。",
                "   **这是正确性限制,不是性能限制**:画面借的是「从你这儿算出来的」可见区块集合",
                "     (不借的话主视图会闪烁,见踩坑 174),盟友离得越远、隔的墙越多,画面上破洞越多。",
                "     调大 = 远处的卡片开始出现破洞。",
                "   **它同时也是【代为操作】能不能连上的距离**(2026-08-08):",
                "     用户口径「看不到应该也连不上」⇒ 两处共用这一个数,调它会一起变。",
                "     跨维度一律算超出(坐标不可比)。",
                " 两项都与熟练度无关 → 端点必须填一样。")
                .push("wide_cast");
        def(b, "wide_cast", "exp_rate", 2.0, 0.4);
        def(b, "wide_cast", "exp_ratio", 0.5, 0.5);

        def(b, "wide_cast", "range_cap", 8, 50);
        def(b, "wide_cast", "count_cap", 3, 18);
        def(b, "wide_cast", "aim_range", 24, 48);

        def(b, "wide_cast", "cost_ratio", 0.2, 0.2);
        def(b, "wide_cast", "exp_bonus", 1.5, 1.5);
        def(b, "wide_cast", "cooldown", 60, 20);

        def(b, "wide_cast", "cam_budget_ms", 8, 8);
        def(b, "wide_cast", "cam_range", 64, 64);

        b.pop();

        b.comment(" 认知篡改(心理掌握,Lv5 **进阶**):永久改变目标敌我阵营。印象操作的进阶形态。",
                " **只能在遥控器里配置**(进不了按键预设),按遥控器当前编号的【影响人数】分流:",
                "   · 影响人数 = 1 → 那一只从此把**施法者**当自己人(永久不再把他当目标)。",
                "   · 影响人数 > 1 → 对人群里的每一个目标,把它**周围**(半径 = 遥控器的【影响范围】)",
                "     的生物改写成「视它为敌」。众叛亲离。",
                " ",
                " **永久** = 没有 duration 这一项。解除的唯一途径是强迫自控的【恢复】",
                "   (只清你自己打上去的那几条,别人的动不了)。",
                " cp / overload = **单发价**;走遥控器时再乘 wide_cast.cost_ratio(默认 0.2),",
                "   并按实际目标数收 —— 与其它进遥控器的技能同一套口径。",
                "   本技能没有单发形态,所以这两个数只会经由遥控器那条路被用到。",
                "   **这一段不能省**:找不到配置键会静默返 0 = 免费无限放(项目里栽过一整个系)。",
                " **这里没有 cooldown** —— 遥控器那条路不给单个技能设冷却",
                "   (冷却是遥控器物品自己的 wide_cast.cooldown)。定义了也没人读,就不定义。",
                " cp/overload 随熟练度下降是本项目通例。")
                .push("cognition_tamper");
        def(b, "cognition_tamper", "exp_rate", 2.0, 0.4);
        def(b, "cognition_tamper", "cp", 400, 260);
        def(b, "cognition_tamper", "overload", 140, 90);
        b.pop();

        b.comment(
                " 痛觉无感(心理掌握·进阶,Lv5,被动)—— 痛觉剔除的进阶形态。",
                " 「受到的任意伤害减免 99.99%」。学会之后它**直接顶替**痛觉剔除,两者不叠加。",
                " 与痛觉剔除共用同一个减伤落点,所以那边写死的两条排除**同样适用**:",
                "   · 虚空 / `/kill` 这类绕过无敌帧的伤害不减(不然掉进虚空就永远死不了);",
                "   · 气绝昏倒的窒息不减(那是用户拍板「无视抗性和伤害减免」的)。",
                " ",
                " reduction = 减免比例。0.9999 = 99.99%。",
                "   两个端点填一样 = 与熟练度无关(用户给的是一个定值)。",
                "   填 1.0 就是**彻底免疫**,填 0 等于关掉本技能(那时会退回痛觉剔除那一档)。",
                " ",
                " 每次挨打的计算力开销 = cp_per_hit + cp_per_damage × 这一下的伤害量。",
                " cp_per_hit    = 每次固定收多少。",
                " cp_per_damage = 每 1 点伤害再收多少。",
                "   之所以要有这一项:只按次收固定价的话,一次 1000 点的重击和一次擦伤同价,",
                "     而 99.99% 减免会把前者完全抹平 —— 那不叫「消耗一些计算力」,叫花零钱买无敌。",
                "     **想要纯按次计价就把它填 0**。",
                " **不产生过载**(用户拍板),所以本段没有 overload 这一项 —— 不是漏了。",
                " **计算力不够时退回痛觉剔除的减伤,并且一点都不扣** ——",
                "   进阶形态的底线是「不差于基础形态」,否则会出现「学了反而更脆」。",
                " 开销随熟练度下降是本项目通例。")
                .push("pain_numb");
        def(b, "pain_numb", "exp_rate", 2.0, 0.4);
        def(b, "pain_numb", "reduction", 0.9999, 0.9999);
        def(b, "pain_numb", "cp_per_hit", 200, 120);
        def(b, "pain_numb", "cp_per_damage", 60, 40);
        b.pop();

        b.comment(
                " 自我失去(心理掌握·进阶,Lv5)—— 呆然自失的进阶形态,**只能经由遥控器施放**。",
                " 「令目标完全无法操作自身」:目标的世界照常在走(怪照样打他、火照样烧),",
                "   而他既动不了、也看不见 —— 视野整屏涂黑。",
                " 与呆然自失是两码事:那个是【时间对他停了】(世界定格、他不掉血),",
                "   这个是【他从自己身上被摘出去了】。两者可以同时挂在一个目标身上,各自倒计时、互不干涉。",
                " ",
                " duration = 持续几 tick。默认 100(5 秒)到 300(15 秒)。",
                "   这是本系最强的控制,时长别照着呆然自失抄 —— 那个期间目标是无敌的活靶子,",
                "     而这个期间他会实打实地被打死。",
                " cp / overload = **单发价**;走遥控器时再乘 wide_cast.cost_ratio(默认 0.2),",
                "   并按实际目标数收 —— 与其它进遥控器的技能同一套口径。",
                "   本技能没有单发形态,所以这两个数只会经由遥控器那条路被用到。",
                "   **这一段不能省**:找不到配置键会静默返 0 = 免费无限放(项目里栽过一整个系)。",
                " **这里没有 cooldown** —— 遥控器那条路不给单个技能设冷却",
                "   (冷却是遥控器物品自己的 wide_cast.cooldown)。定义了也没人读,就不定义。")
                .push("self_loss");
        def(b, "self_loss", "exp_rate", 2.0, 0.4);
        def(b, "self_loss", "duration", 100, 300);
        def(b, "self_loss", "cp", 450, 300);
        def(b, "self_loss", "overload", 150, 100);
        b.pop();

        b.comment(
                " 脑部施压(心理掌握·进阶,Lv5)—— 气绝昏倒的进阶形态,**只能经由遥控器施放**。",
                " 「操控目标的脑积水,可让其增加分泌或减少分泌」——",
                "   放进遥控器格子时会**再选一次**是哪一种(与强迫自控选命令是同一套第二段选择器)。",
                " ",
                " duration = 持续几 tick,**两种模式共用**。默认 160(8 秒)到 360(18 秒)。",
                "   用户给的成长口径只提到时长(「熟练度越高持续时间越长,最长 18 秒」),",
                "     所以下面四个数的两个端点**故意填成一样** —— 伤害与频率不随熟练度变。",
                " ",
                " ── 增加分泌:三个 debuff(黑暗/虚弱/失明)+ 高频低伤 ──",
                " inc_damage   = 每次掉多少血。默认 5。",
                " inc_interval = 几 tick 掉一次。默认 5(= 0.25 秒)。",
                "   **比 vanilla 的无敌帧(10 tick)还密** —— 打得进去是因为窒息伤害类型",
                "     `academy:asphyxiation` 被登记进了 `bypasses_cooldown`。",
                "     把这个数改小到 1 也仍然有效;改大过 10 则与无敌帧无关了。",
                " ",
                " ── 减少分泌:无 debuff,低频高伤 ──",
                " dec_damage   = 每次掉多少血。默认 10。",
                " dec_interval = 几 tick 掉一次。默认 20(= 1 秒)。",
                " ",
                " 照默认值算:增加分泌 20 点/秒 + 三个 debuff,减少分泌 10 点/秒 ——",
                "   **增加分泌在每个维度上都更强**。这是用户 2026-08-04 拍板的数",
                "   (「就按这个数来,我之后自己调」),不是平衡过的结果。想让两支各有取舍,",
                "   把 inc_damage 调低、或把 dec_damage 调高即可。",
                " ",
                " cp / overload = **单发价**;走遥控器时再乘 wide_cast.cost_ratio,并按实际目标数收。",
                "   本技能没有单发形态,这两个数只会经由遥控器那条路被用到。",
                "   **这一段不能省**:找不到配置键会静默返 0 = 免费无限放(项目里栽过一整个系)。",
                " **这里没有 cooldown** —— 遥控器那条路不给单个技能设冷却",
                "   (冷却是遥控器物品自己的 wide_cast.cooldown)。")
                .push("brain_pressure");
        def(b, "brain_pressure", "exp_rate", 2.0, 0.4);
        def(b, "brain_pressure", "duration", 160, 360);
        def(b, "brain_pressure", "inc_damage", 5, 5);
        def(b, "brain_pressure", "inc_interval", 5, 5);
        def(b, "brain_pressure", "dec_damage", 10, 10);
        def(b, "brain_pressure", "dec_interval", 20, 20);
        def(b, "brain_pressure", "cp", 400, 260);
        def(b, "brain_pressure", "overload", 130, 85);
        b.pop();

        b.push("pierce");
        PIERCE_ENABLED = b.define("enabled", true);
        PIERCE_BLACKLIST = b.comment(
                        " 白名单:列入其中的生物不受本节功能影响,其受到的伤害完全按照原版",
                        " 以及该生物自身的设定结算。",
                        " 格式(每行一项,用英文引号括起):",
                        "   \"模组ID:生物ID\"  —— 指定单个生物;",
                        "   \"模组ID:*\"       —— 指定该模组的全部生物。",
                        " 示例:[\"mowziesmobs:wroughtnaut\", \"someboss:*\"]",
                        " 留空表示不排除任何生物。")
                .defineList("blacklist", List.of(), AbilityConfig::isEntityFilter);
        PIERCE_BYPASS_CAP = b.define("bypass_damage_cap", true);

        PIERCE_BREAK_IFRAME = b.define("break_custom_iframe", true);
        b.pop();

        b.comment(" 跨越深渊(矢量操作进阶技能;需要安装附属才会出现)。").push("abyss_stride");
        ABYSS_CP_MULT = b.comment(" 学会之后,计算力上限乘以这个倍数。",
                        " 与白翼守护场的加成是【相乘】的:15 × 1.5 = 22.5 倍。",
                        " 填 1.0 等于关掉这项加成。")
                .defineInRange("cp_multiplier", 15.0, 1.0, 1000.0);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private static boolean isEntityFilter(Object o) {
        if (!(o instanceof String s) || s.isEmpty()) {
            return false;
        }
        int i = s.indexOf(':');
        return i > 0 && i < s.length() - 1;
    }

    private static void skill(ForgeConfigSpec.Builder b, String name, String cn,
                              double cpA, double cpB, double olA, double olB, Double cdA, Double cdB) {
        b.comment(cn).push(name);
        def(b, name, "exp_rate", 2.0, 0.4);
        def(b, name, "cp", cpA, cpB);
        def(b, name, "overload", olA, olB);
        if (cdA != null) def(b, name, "cooldown", cdA, cdB);
        b.pop();
    }

    private static void skill(ForgeConfigSpec.Builder b, String name, String cn,
                              double cpA, double cpB, double olA, double olB, Double cdA, Double cdB,
                              double dmgA, double dmgB) {
        b.comment(cn).push(name);
        def(b, name, "exp_rate", 2.0, 0.4);
        def(b, name, "cp", cpA, cpB);
        def(b, name, "overload", olA, olB);
        if (cdA != null) def(b, name, "cooldown", cdA, cdB);
        def(b, name, "damage", dmgA, dmgB);
        b.pop();
    }

    private static void def(ForgeConfigSpec.Builder b, String skill, String stat, double a, double bv) {
        SK.put(skill + "." + stat + ".a",
                b.defineInRange(stat + "_lv1", a, -1.0e7, 1.0e7));
        SK.put(skill + "." + stat + ".b",
                b.defineInRange(stat + "_lvmax", bv, -1.0e7, 1.0e7));
    }

    private static boolean nonNeg(Object o) {
        return o instanceof Integer i && i >= 0;
    }

    private AbilityConfig() {}

    public static int initCp(int level) { return listGet(INIT_CP.get(), level); }
    public static int addCp(int level) { return listGet(ADD_CP.get(), level); }
    public static int initOverload(int level) { return listGet(INIT_OVERLOAD.get(), level); }
    public static int addOverload(int level) { return listGet(ADD_OVERLOAD.get(), level); }

    public static float cpRecoverSpeed() { return CP_RECOVER_SPEED.get().floatValue(); }
    public static int cpRecoverCooldown() { return CP_RECOVER_CD.get(); }
    public static float overloadRecoverSpeed() { return OVL_RECOVER_SPEED.get().floatValue(); }
    public static int overloadRecoverCooldown() { return OVL_RECOVER_CD.get(); }
    public static float maxCpIncrRate() { return MAXCP_INCR.get().floatValue(); }
    public static float maxOverloadIncrRate() { return MAXO_INCR.get().floatValue(); }

    public static double coinHitThreshold() { return COIN_HIT.get(); }
    public static double coinChargeThreshold() { return COIN_CHARGE.get(); }

    public static int brainCourseCp() { return BRAIN_CP.get(); }
    public static int brainCourseAdvCp() { return BRAIN_ADV_CP.get(); }
    public static int brainCourseAdvOverload() { return BRAIN_ADV_OVL.get(); }
    public static float mindCourseMult() { return MIND_MULT.get().floatValue(); }
    public static float mindCalcCourseCap() { return MIND_CALC_CAP.get().floatValue(); }

    public static float esperRecoverMult() { return ESPER_REC.get().floatValue(); }
    public static float esperRecoverMultScarce() { return ESPER_REC_SCARCE.get().floatValue(); }
    public static float esperScarceLine() { return ESPER_SCARCE_LINE.get().floatValue(); }
    public static float esperDamageCpRatio() { return ESPER_DMG_CP.get().floatValue(); }
    public static float esperBurstRatio() { return ESPER_BURST.get().floatValue(); }

    public static boolean impressionBlocksWardenDarkness() { return IMPRESSION_NO_DARKNESS.get(); }

    public static boolean pierceEnabled() { return PIERCE_ENABLED.get(); }

    public static List<? extends String> pierceBlacklist() { return PIERCE_BLACKLIST.get(); }

    public static boolean pierceBypassCap() { return PIERCE_BYPASS_CAP.get(); }

    public static boolean pierceBreakIFrame() { return PIERCE_BREAK_IFRAME.get(); }

    public static double abyssCpMultiplier() { return ABYSS_CP_MULT.get(); }

    public static float cp(String skill, float exp) { return skLerp(skill, "cp", exp); }
    public static float overload(String skill, float exp) { return skLerp(skill, "overload", exp); }
    public static float cooldown(String skill, float exp) { return skLerp(skill, "cooldown", exp); }

    public static float stat(String skill, String name, float exp) { return skLerp(skill, name, exp); }

    public static float statOr(String skill, String stat, float exp, float fallback) {
        ForgeConfigSpec.DoubleValue a = SK.get(skill + "." + stat + ".a");
        ForgeConfigSpec.DoubleValue bv = SK.get(skill + "." + stat + ".b");
        if (a == null || bv == null) return fallback;
        return MathUtils.lerpf(a.get().floatValue(), bv.get().floatValue(), exp);
    }

    private static float skLerp(String skill, String stat, float exp) {
        ForgeConfigSpec.DoubleValue a = SK.get(skill + "." + stat + ".a");
        ForgeConfigSpec.DoubleValue bv = SK.get(skill + "." + stat + ".b");
        if (a == null || bv == null) return 0f;
        return MathUtils.lerpf(a.get().floatValue(), bv.get().floatValue(), exp);
    }

    private static int listGet(List<? extends Integer> list, int idx) {
        return (list != null && idx >= 0 && idx < list.size()) ? list.get(idx) : 0;
    }
}
