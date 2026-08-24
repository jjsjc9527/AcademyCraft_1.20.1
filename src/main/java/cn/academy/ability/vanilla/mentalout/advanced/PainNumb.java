package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.ability.vanilla.mentalout.passiveskill.PainCutoff;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.world.entity.player.Player;

public class PainNumb extends MentalAdvSkill {

    public static final PainNumb INSTANCE = new PainNumb();

    private PainNumb() {
        super("pain_numb", PainCutoff.INSTANCE, true);
    }

    public static boolean isLearned(Player player) {
        if (!EntityData.isReady(player)) {
            return false;
        }
        AbilityData data = AbilityData.get(player);
        return data != null && data.isSkillLearned(INSTANCE);
    }

    public static float takeOver(Player player, float amount) {
        if (!isLearned(player)) {
            return 0f;
        }
        AbilityData data = AbilityData.get(player);
        float exp = data.getSkillExp(INSTANCE);

        float reduction = MathUtils.clampf(0f, 1f,
                AbilityConfig.stat("pain_numb", "reduction", exp));
        if (reduction <= 0f) {
            return 0f;
        }
        CPData cp = CPData.get(player);
        if (cp == null) {
            return 0f;
        }
        float cost = AbilityConfig.stat("pain_numb", "cp_per_hit", exp)
                + AbilityConfig.stat("pain_numb", "cp_per_damage", exp) * Math.max(0f, amount);

        if (!cp.perform(0f, cost)) {
            return 0f;
        }
        return reduction;
    }
}
