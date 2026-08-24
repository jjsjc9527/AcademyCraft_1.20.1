package cn.academy.event.ability;

import net.minecraft.world.entity.player.Player;

public class CategoryChangeEvent extends AbilityEvent {
    public CategoryChangeEvent(Player _player) {
        super(_player);
    }
}
