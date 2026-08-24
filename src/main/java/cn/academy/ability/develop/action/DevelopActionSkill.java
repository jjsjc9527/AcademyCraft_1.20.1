package cn.academy.ability.develop.action;

import cn.academy.ability.Skill;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.datapart.AbilityData;
import net.minecraft.world.entity.player.Player;

public class DevelopActionSkill implements IDevelopAction {

    private final Skill skill;

    public DevelopActionSkill(Skill skill) {
        this.skill = skill;
    }

    public Skill getSkill() {
        return skill;
    }

    @Override
    public int getStimulations(Player player) {
        return skill.getLearningStims();
    }

    @Override
    public void onLearned(Player player) {
        AbilityData.get(player).learnSkill(skill);
    }

    @Override
    public boolean validate(Player player, IDeveloper developer) {
        return LearningHelper.canLearn(AbilityData.get(player), developer, skill);
    }
}
