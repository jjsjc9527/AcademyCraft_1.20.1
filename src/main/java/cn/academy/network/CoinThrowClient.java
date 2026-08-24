package cn.academy.network;

import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.entity.EntityCoinThrowing;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CoinThrowClient {

    private CoinThrowClient() {}

    public static void spawn(int throwerId, int handOrdinal, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity e = mc.level.getEntity(throwerId);
        if (!(e instanceof Player thrower)) {
            return;
        }
        InteractionHand hand = handOrdinal == InteractionHand.OFF_HAND.ordinal()
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ACEffectEntities.spawn(new EntityCoinThrowing(thrower, stack, hand));
    }
}
