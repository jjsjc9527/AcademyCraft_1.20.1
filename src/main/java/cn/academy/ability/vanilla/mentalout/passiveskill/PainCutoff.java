package cn.academy.ability.vanilla.mentalout.passiveskill;

import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.mentalout.FaintState;
import cn.academy.ability.vanilla.mentalout.advanced.PainNumb;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import cn.academy.util.ACDefense;

public class PainCutoff extends Skill {

    public static final PainCutoff INSTANCE = new PainCutoff();

    private static final String EXP_AT = "mo_pain_expat";

    private PainCutoff() {
        super("pain_cutoff", 1);
        canControl = false;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    public static float reductionOf(Player player) {
        if (!EntityData.isReady(player)) {
            return 0f;
        }
        AbilityData data = AbilityData.get(player);
        if (data == null || !data.isSkillLearned(INSTANCE)) {
            return 0f;
        }
        return MathUtils.clampf(0f, 1f,
                AbilityConfig.stat("pain_cutoff", "reduction", data.getSkillExp(INSTANCE)));
    }

    public static class Events {

        @SubscribeEvent
        public void onLivingDamage(LivingDamageEvent event) {
            if (event.getEntity().level().isClientSide
                    || !(event.getEntity() instanceof Player player)
                    || event.getAmount() <= 0f

                    || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)

                    || event.getSource().is(FaintState.ASPHYXIATION)) {
                return;
            }

            float reduction = PainNumb.takeOver(player, event.getAmount());
            Skill credit = PainNumb.INSTANCE;
            if (reduction <= 0f) {
                reduction = reductionOf(player);
                credit = INSTANCE;
            }
            if (reduction <= 0f) {
                return;
            }
            ACDefense.reduce(event, event.getAmount() * (1f - reduction));
            gainExp(player, credit);
        }

        private void gainExp(Player player, Skill skill) {
            AbilityData data = AbilityData.get(player);
            long now = player.level().getGameTime();
            long last = player.getPersistentData().getLong(EXP_AT);
            int interval = (int) AbilityConfig.stat("pain_cutoff", "exp_interval", 0f);
            if (last != 0L && now >= last && now - last < interval) {
                return;
            }

            player.getPersistentData().putLong(EXP_AT, Math.max(1L, now));
            data.addSkillExp(skill, AbilityConfig.stat("pain_cutoff", "exp_per_hit", 0f));
        }
    }
}
