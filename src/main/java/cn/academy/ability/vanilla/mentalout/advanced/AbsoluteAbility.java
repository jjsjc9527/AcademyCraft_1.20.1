package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.ability.develop.condition.DevConditionNever;
import cn.academy.ability.vanilla.mentalout.passiveskill.WideCast;

public class AbsoluteAbility extends MentalAdvSkill {

    public static final AbsoluteAbility INSTANCE = new AbsoluteAbility();

    private AbsoluteAbility() {
        super("absolute_ability", WideCast.INSTANCE, true);

        addDevCondition(new DevConditionNever());
    }

    @Override
    public boolean hasAura() {
        return true;
    }
}
