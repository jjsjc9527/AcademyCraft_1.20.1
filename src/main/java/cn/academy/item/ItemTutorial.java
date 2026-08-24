package cn.academy.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemTutorial extends Item {

    public ItemTutorial(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide) {
            openScreen();
        } else {

            cn.academy.advancements.ACAdvancements.trigger(
                    player, cn.academy.advancements.ACAdvancements.OPEN_MISAKA_CLOUD);
        }
        return InteractionResultHolder.success(stack);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    private static void openScreen() {
        net.minecraft.client.Minecraft.getInstance()
                .setScreen(new cn.academy.client.gui.GuiTutorial());
    }

}
