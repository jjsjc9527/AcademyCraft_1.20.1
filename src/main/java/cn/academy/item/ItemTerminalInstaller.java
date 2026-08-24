package cn.academy.item;

import cn.academy.network.TerminalInstallMessage;
import cn.academy.terminal.TerminalData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemTerminalInstaller extends Item {

    public ItemTerminalInstaller(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        TerminalData tData = TerminalData.get(player);
        if (tData.isTerminalInstalled()) {
            if (!world.isClientSide)
                player.sendSystemMessage(Component.translatable("terminal.academy.alrdy_installed"));
        } else {
            if (!world.isClientSide) {
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
                tData.install();
                if (player instanceof ServerPlayer sp) {
                    TerminalInstallMessage.sendTo(sp);
                }

                cn.academy.advancements.ACAdvancements.trigger(
                        player, cn.academy.advancements.ACAdvancements.TERMINAL_INSTALLED);
            }
        }
        return InteractionResultHolder.success(stack);
    }

}
