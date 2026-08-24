package cn.academy.item;

import cn.academy.ability.develop.DeveloperType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Consumer;

public class ItemDeveloper extends ItemEnergyBase {

    public ItemDeveloper() {
        super(DeveloperType.PORTABLE.getEnergy(), DeveloperType.PORTABLE.getBandwidth());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> cn.academy.client.gui.developer.DeveloperPortableOpener.open());
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return cn.academy.client.render.item.DeveloperPortableRenderer.getInstance();
            }
        });
    }
}
