package cn.academy.ability.develop.action;

import cn.academy.ability.develop.IDeveloper;
import net.minecraft.world.entity.player.Player;

public interface IDevelopAction {

    int getStimulations(Player player);

    boolean validate(Player player, IDeveloper developer);

    void onLearned(Player player);
}
