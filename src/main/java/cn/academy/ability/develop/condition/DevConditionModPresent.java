package cn.academy.ability.develop.condition;

import cn.academy.Resources;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

public class DevConditionModPresent implements IDevCondition {

    private final String modId;

    public DevConditionModPresent(String modId) {
        this.modId = modId;
    }

    public boolean present() {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return present();
    }

    @Nullable
    @Override
    public ResourceLocation getIcon() {
        return Resources.getTexture("gui/icon_key");
    }

    @Nullable
    @Override
    public String getHintText() {
        return Component.translatable("ac.ability.cond_mod_present", modId).getString();
    }
}
