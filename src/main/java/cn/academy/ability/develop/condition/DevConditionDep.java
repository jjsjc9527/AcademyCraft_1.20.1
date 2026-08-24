package cn.academy.ability.develop.condition;

import cn.academy.ability.Skill;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class DevConditionDep implements IDevCondition {

    public final Skill dependency;

    public final float requiredExp;

    public DevConditionDep(Skill dependency) {
        this(dependency, 0.0f);
    }

    public DevConditionDep(Skill dependency, float requiredExp) {
        this.dependency = dependency;
        this.requiredExp = requiredExp;
    }

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return data.isSkillLearned(dependency)
                && data.getSkillExp(dependency) >= requiredExp;
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return dependency.getHintIcon();
    }

    @Nullable
    @Override
    public String getHintText() {
        return dependency.getDisplayName() + String.format(": %.0f%%", requiredExp * 100);
    }
}
