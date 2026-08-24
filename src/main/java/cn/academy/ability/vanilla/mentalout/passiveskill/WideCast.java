package cn.academy.ability.vanilla.mentalout.passiveskill;

import cn.academy.ability.Skill;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.SkillExpAddedEvent;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WideCast extends Skill {

    public static final WideCast INSTANCE = new WideCast();

    private WideCast() {
        super("wide_cast", 5);
        canControl = false;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ExpChain());
    }

    public static boolean unlocked(Player player) {
        if (player == null || !EntityData.isReady(player)) {
            return false;
        }
        AbilityData data = AbilityData.get(player);
        return data != null && data.isSkillLearned(INSTANCE);
    }

    public static class ExpChain {

        @SubscribeEvent
        public void onLearned(cn.academy.event.ability.SkillLearnEvent event) {
            if (event.skill != INSTANCE || event.player == null
                    || event.player.level().isClientSide) {
                return;
            }
            cn.academy.datapart.CPData cp = cn.academy.datapart.CPData.get(event.player);
            if (cp != null && cp.isActivated()) {

                cp.setActivateState(false, cn.academy.datapart.AbilityToggleSource.SYSTEM);
            }
        }

        @SubscribeEvent
        public void onMindManipExp(SkillExpAddedEvent event) {
            if (event.skill != MindManip.INSTANCE) {
                return;
            }
            Player player = event.player;
            if (player == null || player.level().isClientSide || !EntityData.isReady(player)) {
                return;
            }
            AbilityData data = AbilityData.get(player);

            if (data == null || !data.isSkillLearned(INSTANCE)) {
                return;
            }
            float ratio = AbilityConfig.stat("wide_cast", "exp_ratio", 0f);
            if (ratio <= 0f) {
                return;
            }
            data.addSkillExp(INSTANCE, event.amount * ratio);
        }
    }
}
