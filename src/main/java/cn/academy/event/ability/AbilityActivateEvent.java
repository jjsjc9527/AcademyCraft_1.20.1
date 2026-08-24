package cn.academy.event.ability;

import net.minecraft.world.entity.player.Player;

public class AbilityActivateEvent extends AbilityEvent {

    public AbilityActivateEvent(Player _player) {
        super(_player);
    }
}
