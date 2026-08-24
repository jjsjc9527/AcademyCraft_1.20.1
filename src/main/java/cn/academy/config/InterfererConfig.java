package cn.academy.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class InterfererConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.IntValue RANGE_MIN;
    private static final ForgeConfigSpec.IntValue RANGE_MAX;
    private static final ForgeConfigSpec.IntValue MAX_ENERGY;
    private static final ForgeConfigSpec.IntValue CHARGE_BANDWIDTH;
    private static final ForgeConfigSpec.IntValue ENERGY_BASE;
    private static final ForgeConfigSpec.IntValue ENERGY_INTERVAL;
    private static final ForgeConfigSpec.DoubleValue RANGE_COST_FACTOR;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CP_AMOUNT;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CP_INTERVAL;
    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> ENERGY_EXTRA;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment(" 能力干涉方块的数值。改完【重进世界】生效。",
                        " 下面的列表都是 5 个数,从左到右对应被干涉能力者的等级 LV1~LV5。")
                .push("ability_interferer");

        RANGE_MIN = b.comment(" 干涉范围最小能调到几格。").defineInRange("range_min", 10, 1, 10000);
        RANGE_MAX = b.comment(" 干涉范围最大能调到几格(界面里每次加减 10 格)。").defineInRange("range_max", 100, 1, 10000);
        MAX_ENERGY = b.comment(" 方块最多能存多少能量。").defineInRange("max_energy", 10000, 1, Integer.MAX_VALUE);
        CHARGE_BANDWIDTH = b.comment(" 充电速度上限:电池/无线网每 tick 最多充进来多少能量。")
                .defineInRange("charge_bandwidth", 50, 0, Integer.MAX_VALUE);

        ENERGY_BASE = b.comment(" 范围里每有一个(不在白名单的)能力者,每秒固定烧多少能量。")
                .defineInRange("energy_base_per_user", 10, 0, Integer.MAX_VALUE);
        ENERGY_INTERVAL = b.comment(" 多久扣一次能量(20=每秒一次)。")
                .defineInRange("energy_interval_ticks", 20, 1, 1200);
        RANGE_COST_FACTOR = b.comment(" 范围开得越大越费电:范围每比最小值多 10 格,耗电就多这个比例(0.5=多一半)。")
                .defineInRange("range_cost_factor_per_10", 0.5, 0.0, 1000.0);

        CP_AMOUNT = b.comment(" 每次从能力者身上抽掉多少 CP(按他的等级 LV1~LV5)。")
                .defineList("cp_amount", List.of(10, 50, 80, 100, 500), InterfererConfig::nonNegInt);
        CP_INTERVAL = b.comment(" 多久抽一次 CP(按他的等级)。数字越小抽得越勤:20=1秒,10=0.5秒,2=0.1秒。")
                .defineList("cp_interval_ticks", List.of(20, 16, 10, 6, 2), InterfererConfig::posInt);
        ENERGY_EXTRA = b.comment(" 等级越高的能力者越费电:每秒在基础消耗之外,再按他的等级多烧这些能量。")
                .defineList("energy_extra_per_level", List.of(5, 10, 30, 80, 100), InterfererConfig::nonNegInt);

        b.pop();
        SPEC = b.build();
    }

    private static boolean nonNegInt(Object o) {
        return o instanceof Integer i && i >= 0;
    }

    private static boolean posInt(Object o) {
        return o instanceof Integer i && i >= 1;
    }

    private InterfererConfig() {}

    public static int rangeMin() { return RANGE_MIN.get(); }
    public static int rangeMax() { return RANGE_MAX.get(); }
    public static int maxEnergy() { return MAX_ENERGY.get(); }
    public static int chargeBandwidth() { return CHARGE_BANDWIDTH.get(); }
    public static int energyBase() { return ENERGY_BASE.get(); }
    public static int energyInterval() { return ENERGY_INTERVAL.get(); }
    public static double rangeCostFactor() { return RANGE_COST_FACTOR.get(); }

    public static int cpAmount(int lvIdx) { return listGet(CP_AMOUNT.get(), lvIdx, 0); }
    public static int cpInterval(int lvIdx) { return Math.max(1, listGet(CP_INTERVAL.get(), lvIdx, 20)); }
    public static int energyExtra(int lvIdx) { return listGet(ENERGY_EXTRA.get(), lvIdx, 0); }

    private static int listGet(List<? extends Integer> list, int idx, int fallback) {
        return (list != null && idx >= 0 && idx < list.size()) ? list.get(idx) : fallback;
    }
}
