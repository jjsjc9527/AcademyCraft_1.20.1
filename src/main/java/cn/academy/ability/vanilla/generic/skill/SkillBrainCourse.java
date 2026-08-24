package cn.academy.ability.vanilla.generic.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.CalcEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SkillBrainCourse extends Skill {

    public SkillBrainCourse() {
        super("brain_course", 3);
        this.canControl = false;
        this.tab = SkillTab.GENERIC;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcMaxCP(CalcEvent.MaxCP event) {
        if (AbilityData.get(event.player).isSkillLearned(this)) {
            event.value += AbilityConfig.brainCourseCp();
        }
    }
}
