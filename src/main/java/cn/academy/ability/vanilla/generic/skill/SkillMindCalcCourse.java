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
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

public class SkillMindCalcCourse extends Skill {

    private static final class Window {

        long second = Long.MIN_VALUE;
        float used;
    }

    private static final Map<Player, Window> WINDOW = new WeakHashMap<>();

    public SkillMindCalcCourse() {
        super("mind_calc_course", 4);
        this.canControl = false;
        this.tab = SkillTab.GENERIC;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void capCPCost(CalcEvent.SkillPerform evt) {
        if (evt.cp <= 0) {
            return;
        }

        if (!EntityData.isReady(evt.player)) {
            return;
        }
        AbilityData aData = AbilityData.get(evt.player);
        if (aData == null || !aData.isSkillLearned(this)) {
            return;
        }
        CPData cpData = CPData.get(evt.player);
        if (cpData == null) {
            return;
        }
        float cap = cpData.getMaxCP() * AbilityConfig.mindCalcCourseCap();
        if (cap <= 0) {
            return;
        }

        long second = evt.player.level().getGameTime() / 20L;
        Window w = WINDOW.computeIfAbsent(evt.player, p -> new Window());
        if (w.second != second) {
            w.second = second;
            w.used = 0;
        }

        float left = cap - w.used;
        evt.cp = left <= 0 ? 0 : Math.min(evt.cp, left);
        w.used += evt.cp;
    }
}
