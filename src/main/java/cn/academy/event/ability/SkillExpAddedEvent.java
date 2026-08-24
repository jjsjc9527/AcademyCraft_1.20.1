package cn.academy.event.ability;

import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;

public class SkillExpAddedEvent extends AbilityEvent {
    public final Skill skill;
    public final float amount;

    public SkillExpAddedEvent(Player player, Skill _skill, float _amount) {
        super(player);
        skill = _skill;
        amount = _amount;
    }
}
