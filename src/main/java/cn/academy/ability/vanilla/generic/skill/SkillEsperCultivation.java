package cn.academy.ability.vanilla.generic.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.event.ability.CalcEvent;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SkillEsperCultivation extends Skill {

    private static final int BURST_PERIOD = 10;

    public SkillEsperCultivation() {
        super("esper_cultivation", 4);
        this.canControl = false;
        this.tab = SkillTab.GENERIC;
        MinecraftForge.EVENT_BUS.register(this);
    }

    private AbilityData learnedBy(Player player) {

        if (!EntityData.isReady(player)) {
            return null;
        }
        AbilityData aData = AbilityData.get(player);
        return (aData != null && aData.isSkillLearned(this)) ? aData : null;
    }

    private static boolean isAmple(CPData cpData) {
        return cpData.getCP() >= cpData.getMaxCP() * AbilityConfig.esperScarceLine();
    }

    @SubscribeEvent
    public void recalcCPRecover(CalcEvent.CPRecoverSpeed evt) {
        AbilityData aData = learnedBy(evt.player);
        if (aData == null) {
            return;
        }
        CPData cpData = CPData.get(evt.player);
        boolean scarceBoost = aData.isMaxLevel() && cpData != null && !isAmple(cpData);
        evt.value *= scarceBoost
                ? AbilityConfig.esperRecoverMultScarce()
                : AbilityConfig.esperRecoverMult();
    }

    @SubscribeEvent
    public void extraSkillDamage(CalcEvent.SkillAttack evt) {
        if (evt.value <= 0) {
            return;
        }
        AbilityData aData = learnedBy(evt.player);
        if (aData == null || !aData.isMaxLevel()) {
            return;
        }
        CPData cpData = CPData.get(evt.player);
        if (cpData == null || !isAmple(cpData)) {
            return;
        }
        evt.value += cpData.getCP() * AbilityConfig.esperDamageCpRatio();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = evt.player;
        if (player.level().getGameTime() % BURST_PERIOD != 0) {
            return;
        }
        AbilityData aData = learnedBy(player);
        if (aData == null || !aData.isMaxLevel()) {
            return;
        }
        CPData cpData = CPData.get(player);
        if (cpData == null || isAmple(cpData)) {
            return;
        }
        cpData.setCP(cpData.getCP() + cpData.getMaxCP() * AbilityConfig.esperBurstRatio());
    }
}
