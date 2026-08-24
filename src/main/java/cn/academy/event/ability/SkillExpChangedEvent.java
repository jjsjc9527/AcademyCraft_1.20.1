package cn.academy.event.ability;

import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;

public class SkillExpChangedEvent extends AbilityEvent {
    public final Skill skill;

    public SkillExpChangedEvent(Player player, Skill _skill) {
        super(player);
        skill = _skill;
    }
}
