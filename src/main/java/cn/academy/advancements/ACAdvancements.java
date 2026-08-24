package cn.academy.advancements;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ACAdvancements {

    public static final ACTrigger AC_DEVELOPER = new ACTrigger("ac_developer");
    public static final ACTrigger AC_EXP_FULL = new ACTrigger("ac_exp_full");
    public static final ACTrigger AC_LEARNING_SKILL = new ACTrigger("ac_learning_skill");
    public static final ACTrigger AC_LEVEL_3 = new ACTrigger("ac_level_3");
    public static final ACTrigger AC_LEVEL_5 = new ACTrigger("ac_level_5");
    public static final ACTrigger AC_MATRIX = new ACTrigger("ac_matrix");
    public static final ACTrigger AC_NODE = new ACTrigger("ac_node");
    public static final ACTrigger AC_OVERLOAD = new ACTrigger("ac_overload");
    public static final ACTrigger CONVERT_CATEGORY = new ACTrigger("convert_category");
    public static final ACTrigger DEV_CATEGORY = new ACTrigger("dev_category");
    public static final ACTrigger GETTING_FACTOR = new ACTrigger("getting_factor");
    public static final ACTrigger GETTING_PHASE = new ACTrigger("getting_phase");
    public static final ACTrigger OPEN_MISAKA_CLOUD = new ACTrigger("open_misaka_cloud");
    public static final ACTrigger PHASE_GENERATOR = new ACTrigger("phase_generator");
    public static final ACTrigger TERMINAL_INSTALLED = new ACTrigger("terminal_installed");

    private ACAdvancements() {}

    public static void init() {
        CriteriaTriggers.register(AC_DEVELOPER);
        CriteriaTriggers.register(AC_EXP_FULL);
        CriteriaTriggers.register(AC_LEARNING_SKILL);
        CriteriaTriggers.register(AC_LEVEL_3);
        CriteriaTriggers.register(AC_LEVEL_5);
        CriteriaTriggers.register(AC_MATRIX);
        CriteriaTriggers.register(AC_NODE);
        CriteriaTriggers.register(AC_OVERLOAD);
        CriteriaTriggers.register(CONVERT_CATEGORY);
        CriteriaTriggers.register(DEV_CATEGORY);
        CriteriaTriggers.register(GETTING_FACTOR);
        CriteriaTriggers.register(GETTING_PHASE);
        CriteriaTriggers.register(OPEN_MISAKA_CLOUD);
        CriteriaTriggers.register(PHASE_GENERATOR);
        CriteriaTriggers.register(TERMINAL_INSTALLED);
    }

    public static void trigger(Player player, ACTrigger trigger) {
        if (player instanceof ServerPlayer sp) {
            trigger.trigger(sp);
        }
    }
}
