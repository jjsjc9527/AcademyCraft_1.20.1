package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityLocalization;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.datapart.AbilityData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class DevConditionAdvancedTree implements IDevCondition {

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return LearningHelper.canUseAdvancedTree(developer, data);
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return DeveloperType.ADVANCED.texture;
    }

    @Nullable
    @Override
    public String getHintText() {
        return AbilityLocalization.instance.local("cond_advanced_tree",
                AbilityLocalization.instance.machineType(DeveloperType.ADVANCED),
                AbilityLocalization.instance.levelDesc(LearningHelper.ADVANCED_TREE_LEVEL));
    }
}
