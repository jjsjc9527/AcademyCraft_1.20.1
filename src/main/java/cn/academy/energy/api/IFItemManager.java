package cn.academy.energy.api;

import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class IFItemManager {

    public static final IFItemManager instance = new IFItemManager();

    private IFItemManager() {}

    public double getEnergy(ItemStack stack) {
        return stack.getOrCreateTag().getDouble("energy");
    }

    public double getMaxEnergy(ItemStack stack) {
        return ((ImagEnergyItem) stack.getItem()).getMaxEnergy();
    }

    public void setEnergy(ItemStack stack, double amt) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
        amt = Math.min(item.getMaxEnergy(), amt);
        stack.getOrCreateTag().putDouble("energy", amt);

        int approxDamage = (int) Math.round((1 - amt / getMaxEnergy(stack)) * stack.getMaxDamage());
        stack.setDamageValue(approxDamage);
    }

    public double charge(ItemStack stack, double amt) {
        return charge(stack, amt, false);
    }

    public double charge(ItemStack stack, double amt, boolean ignoreBandwidth) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
        double lim = ignoreBandwidth ? Double.MAX_VALUE : item.getBandwidth();
        double cur = getEnergy(stack);
        double spare = 0.0;
        if (amt + cur > item.getMaxEnergy()) {
            spare = cur + amt - item.getMaxEnergy();
            amt = item.getMaxEnergy() - cur;
        }

        double namt = Math.signum(amt) * Math.min(Math.abs(amt), lim);
        spare += amt - namt;

        setEnergy(stack, cur + namt);
        return spare;
    }

    public double pull(ItemStack stack, double amt, boolean ignoreBandwidth) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();

        double cur = getEnergy(stack);
        double give = Math.min(amt, cur);
        if (!ignoreBandwidth) {
            give = Math.min(give, item.getBandwidth());
        }
        setEnergy(stack, cur - give);

        return give;
    }

    public String getDescription(ItemStack stack) {
        return String.format("%.0f/%.0f IF", getEnergy(stack), getMaxEnergy(stack));
    }

    public boolean isSupported(ItemStack stack) {
        return stack.getItem() instanceof ImagEnergyItem;
    }

    public ItemStack createEmpty(Item item) {
        ItemStack ret = new ItemStack(item);
        charge(ret, 0, true);
        return ret;
    }

    public ItemStack createFull(Item item) {
        ItemStack ret = new ItemStack(item);
        charge(ret, Double.MAX_VALUE, true);
        return ret;
    }
}
