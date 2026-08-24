package cn.academy.event.ability;

import net.minecraft.world.entity.player.Player;

public class LevelChangeEvent extends AbilityEvent {
    public LevelChangeEvent(Player p) {
        super(p);
    }
}
