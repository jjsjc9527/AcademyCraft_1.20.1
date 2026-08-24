package cn.academy.ability.develop;

import cn.academy.ACItems;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@RegDataPart(Player.class)
public class PortableDevData extends DataPart<Player> implements IDeveloper {

    public static PortableDevData get(Player player) {
        return EntityData.get(player).getPart(PortableDevData.class);
    }

    @Nullable
    private ItemStack stack() {
        ItemStack stack = getEntity().getMainHandItem();
        return stack.getItem() == ACItems.DEVELOPER_PORTABLE.get() ? stack : null;
    }

    @Override
    public DeveloperType getDeveloperType() {
        return DeveloperType.PORTABLE;
    }

    @Override
    public boolean tryPullEnergy(double amount) {
        ItemStack stack = stack();
        if (stack == null) return false;
        IFItemManager m = IFItemManager.instance;
        if (m.getEnergy(stack) < amount) return false;
        return m.pull(stack, amount, true) == amount;
    }

    @Override
    public double getEnergy() {
        ItemStack stack = stack();
        return stack == null ? 0 : IFItemManager.instance.getEnergy(stack);
    }

    @Override
    public double getMaxEnergy() {
        ItemStack stack = stack();
        return stack == null ? 0 : IFItemManager.instance.getMaxEnergy(stack);
    }
}
