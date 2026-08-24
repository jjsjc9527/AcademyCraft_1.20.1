package cn.academy.event;

import cn.academy.entity.EntityCoinThrowing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class CoinThrowEvent extends PlayerEvent {

    public final EntityCoinThrowing coin;

    public final InteractionHand hand;

    public CoinThrowEvent(Player player, EntityCoinThrowing coin, InteractionHand hand) {
        super(player);
        this.coin = coin;
        this.hand = hand;
    }
}
