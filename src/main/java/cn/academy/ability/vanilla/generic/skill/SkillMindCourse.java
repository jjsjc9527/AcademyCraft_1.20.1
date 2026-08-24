package cn.academy.ability.vanilla.generic.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.CalcEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SkillMindCourse extends Skill {

    public SkillMindCourse() {

        super("mind_course", 4);
        this.canControl = false;
        this.tab = SkillTab.GENERIC;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcCPRecover(CalcEvent.CPRecoverSpeed evt) {
        if (AbilityData.get(evt.player).isSkillLearned(this)) {
            evt.value *= AbilityConfig.mindCourseMult();
        }
    }
}
