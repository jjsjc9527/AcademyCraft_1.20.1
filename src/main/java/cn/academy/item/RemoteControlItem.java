package cn.academy.item;

import cn.academy.ability.vanilla.mentalout.WideCastExecutor;
import cn.academy.ability.vanilla.mentalout.passiveskill.WideCast;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class RemoteControlItem extends Item {

    public RemoteControlItem(Properties properties) {
        super(properties);
    }

    public static boolean readyInMainHand(Player player) {
        return player != null
                && player.getMainHandItem().getItem() instanceof RemoteControlItem
                && WideCast.unlocked(player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!WideCast.unlocked(player)) {
            if (level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("gui.academy.remote_control.locked"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {

            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    cn.academy.ACSounds.MO_CONTROL.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

            WideCastExecutor.fire(sp, stack);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                java.util.List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.academy.remote_control.tip1")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return cn.academy.client.render.item.RemoteControlItemRenderer.getInstance();
            }
        });
    }
}
