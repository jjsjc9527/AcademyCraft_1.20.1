package cn.academy.event.ability;

import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;

public class SkillLearnEvent extends AbilityEvent {
    public final Skill skill;

    public SkillLearnEvent(Player player, Skill _skill) {
        super(player);
        skill = _skill;
    }
}
