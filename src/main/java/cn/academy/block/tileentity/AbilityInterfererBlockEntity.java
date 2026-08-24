package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.block.block.AbilityInterfererBlock;
import cn.academy.block.container.AbilityInterfererMenu;
import cn.academy.config.InterfererConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.item.ImagEnergyItem;
import cn.academy.network.InterfererInfoMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class AbilityInterfererBlockEntity extends BlockEntity implements IWirelessReceiver, MenuProvider {

    public static final int SLOT_BATTERY = 0;

    private static final int SYNC_INTERVAL = 20;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isSlotValid(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public static boolean isSlotValid(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && stack.getItem() instanceof ImagEnergyItem;
    }

    private double energy = 0;
    private boolean enabled = false;
    private double range;

    private final TreeSet<String> whitelist = new TreeSet<>();
    private String placerName = "";
    private int tick = 0;

    private boolean powered = true;

    public AbilityInterfererBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.ABILITY_INTERFERER.get(), pos, state);
        this.range = InterfererConfig.rangeMin();
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getRange() {
        return range;
    }

    public String[] getWhitelist() {
        return whitelist.toArray(new String[0]);
    }

    public double getEnergy() {
        return energy;
    }

    public double getMaxEnergy() {
        return InterfererConfig.maxEnergy();
    }

    public void setPlacer(Player p) {
        if (placerName.isEmpty()) {
            placerName = p.getName().getString();
            whitelist.add(placerName);
            setChanged();
        }
    }

    @Override
    public double getRequiredEnergy() {
        return InterfererConfig.maxEnergy() - energy;
    }

    @Override
    public double injectEnergy(double amt) {
        double give = Math.min(amt, InterfererConfig.maxEnergy() - energy);
        energy += give;
        setChanged();
        return amt - give;
    }

    @Override
    public double pullEnergy(double amt) {
        double a = Math.min(amt, energy);
        energy -= a;
        setChanged();
        return a;
    }

    @Override
    public double getBandwidth() {
        return InterfererConfig.chargeBandwidth();
    }

    public void setEnabled(boolean v) {
        enabled = v;
        updateBlockOn();
        setChanged();
    }

    public void setRange(double v) {
        range = Mth.clamp(v, InterfererConfig.rangeMin(), InterfererConfig.rangeMax());
        setChanged();
    }

    public void setWhitelist(Collection<String> names) {
        whitelist.clear();
        whitelist.addAll(names);
        setChanged();
    }

    private AABB testBB() {
        BlockPos p = getBlockPos();
        double x = p.getX() + 0.5, y = p.getY() + 0.5, z = p.getZ() + 0.5;
        return new AABB(x - range, y - range, z - range, x + range, y + range, z + range);
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;
        tick++;

        if (enabled) {
            List<Player> targets = level.getEntitiesOfClass(Player.class, testBB(), this::isTarget);

            if (tick % InterfererConfig.energyInterval() == 0) {
                double cost = 0;
                for (Player p : targets) cost += InterfererConfig.energyBase() + InterfererConfig.energyExtra(lvIndex(p));
                cost *= rangeMultiplier();
                if (cost <= 0) {
                    powered = true;
                } else if (energy >= cost) {
                    energy -= cost;
                    powered = true;
                    setChanged();
                } else {
                    powered = false;
                }
            }

            if (powered) {
                for (Player p : targets) {
                    int idx = lvIndex(p);
                    if (tick % InterfererConfig.cpInterval(idx) == 0) {
                        CPData.get(p).drainCP(InterfererConfig.cpAmount(idx));
                    }
                }
            }
        }

        chargeFromSlot();

        if (tick % SYNC_INTERVAL == 0) {
            InterfererInfoMessage.sendTracking(this);
        }
    }

    private boolean isTarget(Player p) {

        return cn.lambdalib2.datapart.EntityData.isReady(p)
                && !p.isSpectator()
                && !p.getAbilities().instabuild
                && AbilityData.get(p).hasCategory()
                && !whitelist.contains(p.getName().getString());
    }

    private static int lvIndex(Player p) {
        return Mth.clamp(AbilityData.get(p).getLevel(), 1, 5) - 1;
    }

    private double rangeMultiplier() {
        return 1.0 + InterfererConfig.rangeCostFactor() * ((range - InterfererConfig.rangeMin()) / 10.0);
    }

    private void chargeFromSlot() {
        ItemStack stack = items.getStackInSlot(SLOT_BATTERY);
        if (!IFItemManager.instance.isSupported(stack)) return;
        double want = Math.min(getMaxEnergy() - energy, getBandwidth());
        if (want <= 0) return;
        double gain = IFItemManager.instance.pull(stack, want, false);
        if (gain > 0) injectEnergy(gain);
    }

    private void updateBlockOn() {
        if (level == null) return;
        BlockState st = getBlockState();
        if (st.getBlock() instanceof AbilityInterfererBlock
                && st.getValue(AbilityInterfererBlock.ON) != enabled) {
            level.setBlock(getBlockPos(), st.setValue(AbilityInterfererBlock.ON, enabled), 3);
        }
    }

    public void dropContents() {
        if (level == null) return;
        ItemStack s = items.getStackInSlot(SLOT_BATTERY);
        if (!s.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level,
                    getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), s);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.ability_interferer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AbilityInterfererMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
        enabled = tag.getBoolean("enabled");
        range = tag.getFloat("range");
        if (range < InterfererConfig.rangeMin()) range = InterfererConfig.rangeMin();
        placerName = tag.getString("placer");
        whitelist.clear();
        ListTag list = tag.getList("whitelist", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            whitelist.add(list.getString(i));
        }
        items.deserializeNBT(tag.getCompound("items"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.putBoolean("enabled", enabled);
        tag.putFloat("range", (float) range);
        tag.putString("placer", placerName);
        ListTag list = new ListTag();
        for (String s : whitelist) list.add(StringTag.valueOf(s));
        tag.put("whitelist", list);
        tag.put("items", items.serializeNBT());
    }
}
