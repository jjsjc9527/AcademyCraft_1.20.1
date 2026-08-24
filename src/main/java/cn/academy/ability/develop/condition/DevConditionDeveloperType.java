package cn.academy.ability.develop.condition;

import cn.academy.ability.AbilityLocalization;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class DevConditionDeveloperType implements IDevCondition {

    private final DeveloperType type;

    public DevConditionDeveloperType(DeveloperType type) {
        this.type = type;
    }

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return developer.getDeveloperType().ordinal() >= type.ordinal();
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return type.texture;
    }

    @Nullable
    @Override
    public String getHintText() {
        return AbilityLocalization.instance.machineType(type);
    }
}
