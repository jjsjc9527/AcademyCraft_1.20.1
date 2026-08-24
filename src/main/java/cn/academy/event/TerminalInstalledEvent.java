package cn.academy.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class TerminalInstalledEvent extends Event {

    public final Player player;

    public TerminalInstalledEvent(Player _player) {
        player = _player;
    }

}
