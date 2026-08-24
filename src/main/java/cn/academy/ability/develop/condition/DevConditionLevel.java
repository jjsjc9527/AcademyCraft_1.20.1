package cn.academy.ability.develop.condition;

import cn.academy.ability.Skill;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class DevConditionLevel implements IDevCondition {

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return data.getLevel() >= skill.getLevel();
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return null;
    }

    @Nullable
    @Override
    public String getHintText() {
        return null;
    }

    @Override
    public boolean shouldDisplay() {
        return false;
    }
}
