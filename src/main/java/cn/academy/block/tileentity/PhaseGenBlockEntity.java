package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.ACFluids;
import cn.academy.ACItems;
import cn.academy.block.block.PhaseGenBlock;
import cn.academy.block.container.PhaseGenMenu;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.item.ImagEnergyItem;
import cn.academy.item.MatterUnitItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PhaseGenBlockEntity extends BlockEntity implements IWirelessGenerator, MenuProvider {

    public static final int SLOT_LIQUID_IN = 0, SLOT_LIQUID_OUT = 1, SLOT_OUTPUT = 2;

    public static final double BUFFER_SIZE = 6000;
    public static final double BANDWIDTH = 50;
    public static final int TANK_SIZE = 8000;
    public static final int PER_UNIT = 1000;
    public static final int CONSUME_PER_TICK = 100;
    public static final double GEN_PER_MB = 0.5;

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_LIQUID_IN -> isPhaseLiquid(stack);
                case SLOT_LIQUID_OUT -> stack.getItem() instanceof MatterUnitItem;
                default -> stack.getItem() instanceof ImagEnergyItem;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank tank = new FluidTank(TANK_SIZE,
            fs -> fs.getFluid() == ACFluids.IMAGPROJ.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> tank);

    private double energy = 0;
    private int updateTicker = 0;

    public PhaseGenBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.PHASE_GEN.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public double getEnergy() {
        return energy;
    }

    public int getLiquidAmount() {
        return tank.getFluidAmount();
    }

    public int getTankSize() {
        return tank.getCapacity();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return tankCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankCap.invalidate();
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
        return BANDWIDTH;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        generate();
        sinkLiquid();
        tryChargeStack(items.getStackInSlot(SLOT_OUTPUT));

        if (++updateTicker >= 10) {
            updateTicker = 0;
            rebuildBlockState();
        }
    }

    private void generate() {
        double required = BUFFER_SIZE - energy;
        if (required <= 0) return;

        int maxDrain = (int) Math.min(CONSUME_PER_TICK, required / GEN_PER_MB);
        FluidStack fs = tank.drain(maxDrain, IFluidHandler.FluidAction.EXECUTE);
        if (fs.isEmpty()) return;

        energy = Math.min(BUFFER_SIZE, energy + fs.getAmount() * GEN_PER_MB);
        setChanged();
    }

    private void sinkLiquid() {
        ItemStack in = items.getStackInSlot(SLOT_LIQUID_IN);
        if (in.isEmpty() || !isPhaseLiquid(in)) return;
        if (!isOutputSlotAvailable()) return;
        if (tank.getCapacity() - tank.getFluidAmount() < PER_UNIT) return;

        tank.fill(new FluidStack(ACFluids.IMAGPROJ.get(), PER_UNIT), IFluidHandler.FluidAction.EXECUTE);
        in.shrink(1);

        ItemStack out = items.getStackInSlot(SLOT_LIQUID_OUT);
        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_LIQUID_OUT,
                    ACItems.MATTER_UNIT.get().create(MatterUnitItem.MAT_NONE));
        } else {
            out.grow(1);
        }
        setChanged();
    }

    private void tryChargeStack(ItemStack stack) {
        if (!IFItemManager.instance.isSupported(stack)) return;
        double cangive = Math.min(energy, getBandwidth());
        double ret = IFItemManager.instance.charge(stack, cangive);
        double moved = cangive - ret;
        if (moved != 0) {
            energy -= moved;
            setChanged();
        }
    }

    private void rebuildBlockState() {
        BlockState st = getBlockState();
        if (!(st.getBlock() instanceof PhaseGenBlock)) return;

        int lv = (int) Math.min(4, Math.round(4.0 * tank.getFluidAmount() / tank.getCapacity()));
        if (st.getValue(PhaseGenBlock.LIQUID) != lv) {
            level.setBlock(getBlockPos(), st.setValue(PhaseGenBlock.LIQUID, lv), 3);
        }
    }

    private boolean isPhaseLiquid(ItemStack stack) {
        return ACItems.MATTER_UNIT.get().is(stack, MatterUnitItem.MAT_PHASE_LIQUID);
    }

    private boolean isOutputSlotAvailable() {
        ItemStack stack = items.getStackInSlot(SLOT_LIQUID_OUT);
        return stack.isEmpty()
                || (ACItems.MATTER_UNIT.get().is(stack, MatterUnitItem.MAT_NONE)
                    && stack.getCount() < stack.getMaxStackSize());
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), s);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.phase_gen");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PhaseGenMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
        tank.readFromNBT(tag.getCompound("tank"));
        items.deserializeNBT(tag.getCompound("items"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.put("items", items.serializeNBT());
    }
}
