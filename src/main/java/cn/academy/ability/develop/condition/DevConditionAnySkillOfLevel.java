package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityLocalization;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class DevConditionAnySkillOfLevel implements IDevCondition {

    private final int level;
    private final ResourceLocation icon;

    public DevConditionAnySkillOfLevel(int level) {
        this.level = level;
        this.icon = new ResourceLocation("academy", "textures/abilities/condition/any" + level + ".png");
    }

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        if (!data.hasCategory()) return false;
        for (Skill s : data.getCategory().getSkillsOfLevel(level)) {
            if (data.isSkillLearned(s)) return true;
        }
        return false;
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return icon;
    }

    @Nullable
    @Override
    public String getHintText() {

        return AbilityLocalization.instance.local("anyskill", level);
    }
}
