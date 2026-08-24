package cn.academy.ability.vanilla.vecmanip.advanced;

import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.develop.condition.DevConditionNever;

public class AbsoluteAbility extends Skill {

    public static final AbsoluteAbility INSTANCE = new AbsoluteAbility();

    private AbsoluteAbility() {

        super("absolute_ability", LearningHelper.ADVANCED_TREE_LEVEL);

        this.tab = SkillTab.ADVANCED;

        this.canControl = false;

        addDevCondition(new DevConditionNever());
    }

    @Override
    public boolean hasAura() {
        return true;
    }
}
