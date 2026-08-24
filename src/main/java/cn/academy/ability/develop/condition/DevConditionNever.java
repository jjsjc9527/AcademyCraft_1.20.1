package cn.academy.ability.develop.condition;

import cn.academy.Resources;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class DevConditionNever implements IDevCondition {

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return false;
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return Resources.getTexture("gui/icon_key");
    }

    @Nullable
    @Override
    public String getHintText() {
        return Component.translatable("ac.ability.cond_never").getString();
    }
}
