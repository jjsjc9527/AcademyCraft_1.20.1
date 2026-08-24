package cn.academy.event.ability;

import net.minecraft.world.entity.player.Player;

public class AbilityDeactivateEvent extends AbilityEvent {

    public AbilityDeactivateEvent(Player _player) {
        super(_player);
    }
}
