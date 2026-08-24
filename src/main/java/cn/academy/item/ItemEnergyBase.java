package cn.academy.item;

import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemEnergyBase extends Item implements ImagEnergyItem {

    protected static final IFItemManager itemManager = IFItemManager.instance;

    public static final int MAX_DAMAGE = 13;

    public final double maxEnergy;
    public final double bandwidth;

    public ItemEnergyBase(double maxEnergy, double bandwidth) {
        super(new Item.Properties().durability(MAX_DAMAGE));
        this.maxEnergy = maxEnergy;
        this.bandwidth = bandwidth;
    }

    @Override
    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, net.minecraft.world.entity.player.Player player) {
        itemManager.setEnergy(stack, 0);
    }

    @Override
    public double getBandwidth() {
        return bandwidth;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(itemManager.getDescription(stack)));
    }

    public static void registerProperties(Item item) {
        net.minecraft.client.renderer.item.ItemProperties.register(item,
                new ResourceLocation("energy"),
                (stack, level, entity, seed) -> {
                    int damage = stack.getDamageValue();
                    if (damage < 3) return 1.0f;
                    if (damage > 10) return 0.0f;
                    return 0.5f;
                });
    }
}
