package cn.academy.ability.develop.condition;

import cn.academy.ability.Skill;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface IDevCondition {

    boolean accepts(AbilityData data, IDeveloper developer, Skill skill);

    @Nullable
    ResourceLocation getIcon();

    @Nullable
    String getHintText();

    default boolean shouldDisplay() {
        return true;
    }
}
