package cn.academy.item;

import cn.academy.ACItems;
import cn.academy.ACSounds;
import cn.academy.entity.EntityCoinThrowing;
import cn.academy.event.CoinThrowEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CoinItem extends Item {

    private static final Map<String, EntityCoinThrowing> CLIENT = new HashMap<>(), SERVER = new HashMap<>();

    public CoinItem(Properties p) {
        super(p);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new CoinItem.Events());
        EntityCoinThrowing.Events.init();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (getPlayerCoin(player) != null) {
            return InteractionResultHolder.pass(stack);
        }

        EntityCoinThrowing coin = new EntityCoinThrowing(player, stack, hand);
        spawnBothSides(level, coin);
        broadcastToWatchers(player, hand, stack);

        player.playSound(ACSounds.ENTITY_FLIPCOIN.get(), 0.5f, 1.0f);
        setPlayerCoin(player, coin);

        MinecraftForge.EVENT_BUS.post(new CoinThrowEvent(player, coin, hand));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    private static void spawnBothSides(Level level, EntityCoinThrowing coin) {
        if (level.isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> cn.academy.client.render.entity.ACEffectEntities.spawn(coin));
        } else {
            level.addFreshEntity(coin);
        }
    }

    private static void broadcastToWatchers(Player player, InteractionHand hand, ItemStack stack) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {

            cn.academy.network.CoinThrowMessage.broadcast(sp, hand, stack.copy());
        }
    }

    @Nullable
    public static EntityCoinThrowing getPlayerCoin(Player player) {
        EntityCoinThrowing coin = getMap(player).get(player.getName().getString());
        return (coin != null && coin.isAlive()) ? coin : null;
    }

    public static void setPlayerCoin(Player player, EntityCoinThrowing coin) {
        getMap(player).put(player.getName().getString(), coin);
    }

    private static Map<String, EntityCoinThrowing> getMap(Player player) {
        return player.level().isClientSide ? CLIENT : SERVER;
    }

    public static class Events {

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            EntityCoinThrowing coin = getMap(player).get(player.getName().getString());
            if (coin != null && (!coin.isAlive() || coin.level() != player.level())) {
                getMap(player).remove(player.getName().getString());
            }
        }
    }
}
