package cn.academy.event;

import cn.academy.tutorial.ACTutorial;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class TutorialActivatedEvent extends PlayerEvent {

    public final ACTutorial tutorial;

    public TutorialActivatedEvent(Player player, ACTutorial tutorial) {
        super(player);
        this.tutorial = tutorial;
    }

}
