package cn.academy.event;

import cn.academy.terminal.App;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class AppInstalledEvent extends Event {

    public final Player player;
    public final App app;

    public AppInstalledEvent(Player _player, App _app) {
        player = _player;
        app = _app;
    }

}
