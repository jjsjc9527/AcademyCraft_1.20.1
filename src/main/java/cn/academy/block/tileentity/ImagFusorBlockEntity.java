package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.ACFluids;
import cn.academy.ACItems;
import cn.academy.block.block.ImagFusorBlock;
import cn.academy.block.container.ImagFusorMenu;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.block.IWirelessReceiver;
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

public class ImagFusorBlockEntity extends BlockEntity implements IWirelessReceiver, MenuProvider {

    public static final int SLOT_INPUT = 0, SLOT_OUTPUT = 1,
            SLOT_IMAG_INPUT = 2, SLOT_ENERGY_INPUT = 3, SLOT_IMAG_OUTPUT = 4;

    public static final double MAX_ENERGY = 2000;
    public static final double BANDWIDTH = 50;
    public static final int TANK_SIZE = 8000;
    public static final int PER_UNIT = 1000;
    public static final double WORK_SPEED = 1.0 / 120;
    public static final double CONSUME_PER_TICK = 12;

    private final ItemStackHandler items = new ItemStackHandler(5) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {

                case SLOT_INPUT -> ImagFusorRecipes.INSTANCE.getRecipe(stack) != null;
                case SLOT_OUTPUT -> false;
                case SLOT_IMAG_INPUT -> ACItems.MATTER_UNIT.get().is(stack, MatterUnitItem.MAT_PHASE_LIQUID);
                case SLOT_IMAG_OUTPUT -> stack.getItem() instanceof MatterUnitItem;
                default -> stack.getItem() instanceof ImagEnergyItem;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank tank = new FluidTank(TANK_SIZE, fs -> fs.getFluid() == ACFluids.IMAGPROJ.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final LazyOptional<IFluidHandler> tankCap = LazyOptional.of(() -> tank);

    private double energy = 0;
    private IFRecipe currentRecipe;
    private double workProgress;
    private int checkCooldown = 10;
    private boolean lastWorking = false;

    public ImagFusorBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.IMAG_FUSOR.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public int getLiquidAmount() {
        return tank.getFluidAmount();
    }

    public int getTankSize() {
        return tank.getCapacity();
    }

    public double getWorkProgress() {
        return isWorking() ? workProgress : 0.0;
    }

    public IFRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    public boolean isWorking() {
        return currentRecipe != null;
    }

    @Override
    public double getRequiredEnergy() {
        return MAX_ENERGY - energy;
    }

    @Override
    public double injectEnergy(double amt) {
        double give = Math.min(amt, MAX_ENERGY - energy);
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
        return BANDWIDTH;
    }

    public double getEnergy() {
        return energy;
    }

    public double getMaxEnergy() {
        return MAX_ENERGY;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        if (!isWorking()) {
            if (--checkCooldown <= 0) {
                checkCooldown = 10;
                ItemStack in = items.getStackInSlot(SLOT_INPUT);
                if (!in.isEmpty()) {
                    IFRecipe recipe = ImagFusorRecipes.INSTANCE.getRecipe(in);
                    if (recipe != null) startWorking(recipe);
                }
            }
        }
        if (isWorking()) {
            updateWork();
        }

        sinkLiquid();
        chargeFromSlot();

        boolean w = isWorking() && !isActionBlocked();
        if (w != lastWorking) {
            lastWorking = w;
            BlockState st = getBlockState();
            if (st.getBlock() instanceof ImagFusorBlock) {
                level.setBlock(getBlockPos(), st.setValue(ImagFusorBlock.WORKING, w), 3);
            }
        }
    }

    public void clientTick() {
        if (level == null || !level.isClientSide) return;
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> cn.academy.client.sound.MachineSounds.updateWorkSound(
                        this, isClientWorking(), cn.academy.ACSounds.IMAG_FUSOR_WORK.get(),
                        be -> ((ImagFusorBlockEntity) be).isClientWorking()));
    }

    private boolean isClientWorking() {
        BlockState st = getBlockState();
        return st.getBlock() instanceof ImagFusorBlock && st.getValue(ImagFusorBlock.WORKING);
    }

    private void startWorking(IFRecipe recipe) {
        currentRecipe = recipe;
        workProgress = 0.0;
        setChanged();
    }

    private void updateWork() {
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);

        if (in.isEmpty()
                || currentRecipe.consumeType.getItem() != in.getItem()
                || pullEnergy(CONSUME_PER_TICK) != CONSUME_PER_TICK
                || getLiquidAmount() < currentRecipe.consumeLiquid
                || (!out.isEmpty() && out.getItem() != currentRecipe.output.getItem())) {
            abortWorking();
            return;
        }

        if (!isActionBlocked()) {
            workProgress += WORK_SPEED;
            if (workProgress >= 1.0) {
                endWorking();
            }
            setChanged();
        }
    }

    private void endWorking() {
        if (isWorking()) {
            tank.drain(currentRecipe.consumeLiquid, IFluidHandler.FluidAction.EXECUTE);
            items.getStackInSlot(SLOT_INPUT).shrink(currentRecipe.consumeType.getCount());

            ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
            if (out.isEmpty()) {
                items.setStackInSlot(SLOT_OUTPUT, currentRecipe.output.copy());
            } else {
                out.grow(currentRecipe.output.getCount());
            }
        }
        workProgress = 0.0;
        currentRecipe = null;
        checkCooldown = 0;
        setChanged();
    }

    private void abortWorking() {
        workProgress = 0.0;
        currentRecipe = null;
        setChanged();
    }

    public boolean isActionBlocked() {
        if (!isWorking()) return true;
        ItemStack in = items.getStackInSlot(SLOT_INPUT);
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        if (in.getCount() < currentRecipe.consumeType.getCount()) return true;
        if (!out.isEmpty() && (out.getItem() != currentRecipe.output.getItem()
                || out.getCount() + currentRecipe.output.getCount() > out.getMaxStackSize())) return true;
        return currentRecipe.consumeLiquid > getLiquidAmount();
    }

    private void sinkLiquid() {
        ItemStack in = items.getStackInSlot(SLOT_IMAG_INPUT);
        if (in.isEmpty() || !ACItems.MATTER_UNIT.get().is(in, MatterUnitItem.MAT_PHASE_LIQUID)) return;

        ItemStack out = items.getStackInSlot(SLOT_IMAG_OUTPUT);
        boolean outOk = out.isEmpty()
                || (ACItems.MATTER_UNIT.get().is(out, MatterUnitItem.MAT_NONE)
                    && out.getCount() < out.getMaxStackSize());
        if (!outOk) return;
        if (getLiquidAmount() + PER_UNIT > TANK_SIZE) return;

        tank.fill(new FluidStack(ACFluids.IMAGPROJ.get(), PER_UNIT), IFluidHandler.FluidAction.EXECUTE);
        in.shrink(1);
        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_IMAG_OUTPUT, ACItems.MATTER_UNIT.get().create(MatterUnitItem.MAT_NONE));
        } else {
            out.grow(1);
        }
        setChanged();
    }

    private void chargeFromSlot() {
        ItemStack stack = items.getStackInSlot(SLOT_ENERGY_INPUT);
        if (!IFItemManager.instance.isSupported(stack)) return;
        double want = Math.min(getMaxEnergy() - energy, getBandwidth());
        if (want <= 0) return;
        double gain = IFItemManager.instance.pull(stack, want, false);
        if (gain > 0) injectEnergy(gain);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return tankCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankCap.invalidate();
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
        return Component.translatable("block.academy.imag_fusor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ImagFusorMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
        tank.readFromNBT(tag.getCompound("tank"));
        items.deserializeNBT(tag.getCompound("items"));
        int rcp = tag.contains("recipe") ? tag.getInt("recipe") : -1;
        currentRecipe = rcp < 0 ? null : ImagFusorRecipes.INSTANCE.byId(rcp);
        workProgress = tag.getDouble("progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.put("items", items.serializeNBT());
        tag.putInt("recipe", currentRecipe == null ? -1 : currentRecipe.getID());
        tag.putDouble("progress", workProgress);
    }
}
