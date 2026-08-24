package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.block.container.WirelessGeneratorMenu;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.item.ImagEnergyItem;
import cn.academy.event.energy.LinkUserEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessGeneratorBlockEntity extends BlockEntity implements IWirelessGenerator, MenuProvider {

    public static final int STATUS_STOPPED = 0;
    public static final int STATUS_WEAK = 1;
    public static final int STATUS_STRONG = 2;

    public static final double BUFFER_SIZE = 1000;
    private static final double MAX_GEN = 3.0;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof ImagEnergyItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private double energy = 0;
    private int linkTimer = 0;

    public WirelessGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.WIRELESS_GENERATOR.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public double getEnergy() {
        return energy;
    }

    public double getGeneration(double required) {
        double brightLev = canGenerate() ? 1.0 : 0.0;
        brightLev *= (level != null && level.isRaining()) ? 0.2 : 1.0;
        return Math.min(required, brightLev * MAX_GEN);
    }

    public int getStatus() {
        if (canGenerate()) {
            return (level != null && level.isRaining()) ? STATUS_WEAK : STATUS_STRONG;
        }
        return STATUS_STOPPED;
    }

    private boolean canGenerate() {
        if (level == null) return false;
        long time = level.getDayTime() % 24000;
        boolean isDay = time >= 0 && time <= 12500;
        return isDay && level.canSeeSky(getBlockPos().above());
    }

    @Override
    public double getProvidedEnergy(double req) {
        double give = Math.min(energy, req);
        energy -= give;
        setChanged();
        return give;
    }

    @Override
    public double getBandwidth() {
        return 50;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (energy < BUFFER_SIZE) {
            energy = Math.min(BUFFER_SIZE, energy + getGeneration(BUFFER_SIZE - energy));
            setChanged();
        }
        tryChargeStack(items.getStackInSlot(0));
    }

    private void tryChargeStack(ItemStack stack) {
        if (!IFItemManager.instance.isSupported(stack)) {
            return;
        }
        double cangive = Math.min(energy, getBandwidth());
        double ret = IFItemManager.instance.charge(stack, cangive);
        double moved = cangive - ret;
        if (moved != 0) {
            energy -= moved;
            setChanged();
        }
    }

    public void dropContents() {
        if (level == null) return;
        ItemStack s = items.getStackInSlot(0);
        if (!s.isEmpty()) {
            Containers.dropItemStack(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), s);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.wireless_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WirelessGeneratorMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
        items.deserializeNBT(tag.getCompound("items"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.put("items", items.serializeNBT());
    }
}
