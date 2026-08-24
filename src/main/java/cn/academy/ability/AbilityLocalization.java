package cn.academy.ability;

import cn.academy.ability.develop.DeveloperType;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum AbilityLocalization {
    instance;

    public String levelDesc(int level) {
        return Component.translatable("ability.academy.level" + level).getString();
    }

    public String machineType(DeveloperType type) {
        return local("type_" + type.toString().toLowerCase(Locale.ROOT));
    }

    public String local(String key) {
        return Component.translatable("gui.academy.skill_tree." + key).getString();
    }

    public String local(String key, Object... args) {
        return Component.translatable("gui.academy.skill_tree." + key, args).getString();
    }
}
