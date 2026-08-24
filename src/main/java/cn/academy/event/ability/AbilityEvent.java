package cn.academy.event.ability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class AbilityEvent extends Event {
    public final Player player;

    public AbilityEvent(Player _player) {
        player = _player;
    }
}
