package cn.academy.ability.vanilla.generic.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.CalcEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SkillBrainCourseAdvanced extends Skill {

    public SkillBrainCourseAdvanced() {
        super("brain_course_advanced", 4);
        this.canControl = false;
        this.tab = SkillTab.GENERIC;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcMaxCP(CalcEvent.MaxCP event) {
        if (learned(event.player)) {
            event.value += AbilityConfig.brainCourseAdvCp();
        }
    }

    @SubscribeEvent
    public void recalcMaxOverload(CalcEvent.MaxOverload event) {
        if (learned(event.player)) {
            event.value += AbilityConfig.brainCourseAdvOverload();
        }
    }

    private boolean learned(Player player) {
        return AbilityData.get(player).isSkillLearned(this);
    }
}
