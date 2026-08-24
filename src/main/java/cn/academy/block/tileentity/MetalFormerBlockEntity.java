package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.block.container.MetalFormerMenu;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.crafting.MetalFormerRecipes.Mode;
import cn.academy.crafting.MetalFormerRecipes.RecipeObject;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetalFormerBlockEntity extends BlockEntity implements IWirelessReceiver, MenuProvider {

    public static final int SLOT_IN = 0, SLOT_OUT = 1, SLOT_BATTERY = 2;

    public static final double MAX_ENERGY = 3000;
    public static final double BANDWIDTH = 50;
    public static final int WORK_TICKS = 60;
    public static final double CONSUME_PER_TICK = 13.3;

    private static final int SEARCH_TICKS = 5;

    private final ItemStackHandler items = new ItemStackHandler(3) {
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
        return switch (slot) {

            case SLOT_IN -> MetalFormerRecipes.INSTANCE.isValidInput(stack);
            case SLOT_OUT -> false;
            default -> stack.getItem() instanceof ImagEnergyItem;
        };
    }

    private double energy = 0;
    private Mode mode = Mode.PLATE;
    private RecipeObject current;
    private int workCounter;

    private boolean clientWorking = false;
    private boolean lastWorking = false;

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.METAL_FORMER.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public Mode getMode() {
        return mode;
    }

    public RecipeObject getCurrentRecipe() {
        return current;
    }

    public boolean isWorkInProgress() {
        return current != null && !isActionBlocked();
    }

    public double getWorkProgress() {
        return isWorkInProgress() ? (double) workCounter / WORK_TICKS : 0;
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

    public void cycleMode(int delta) {
        int next = mode.ordinal() + delta;
        if (next >= Mode.values().length) next = 0;
        else if (next < 0) next = Mode.values().length - 1;
        mode = Mode.values()[next];

        current = null;
        workCounter = 0;
        setChanged();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        if (current != null) {

            if (!isActionBlocked() && pullEnergy(CONSUME_PER_TICK) == CONSUME_PER_TICK) {
                ++workCounter;
                if (workCounter == WORK_TICKS) {
                    finishJob();
                }
            } else {
                current = null;
                workCounter = 0;
            }
        } else {
            if (++workCounter >= SEARCH_TICKS) {
                current = MetalFormerRecipes.INSTANCE.getRecipe(items.getStackInSlot(SLOT_IN), mode);
                workCounter = 0;
            }
        }

        chargeFromSlot();

        boolean w = isWorkInProgress();
        if (w != lastWorking) {
            lastWorking = w;
            clientWorking = w;
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    private void finishJob() {
        ItemStack in = items.getStackInSlot(SLOT_IN);
        ItemStack out = items.getStackInSlot(SLOT_OUT);
        ItemStack result = current.getOutput();

        in.shrink(current.getInputCount());
        if (in.getCount() == 0) {
            items.setStackInSlot(SLOT_IN, ItemStack.EMPTY);
        }
        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_OUT, result);
        } else {
            out.grow(result.getCount());
        }
        current = null;
        workCounter = 0;
        setChanged();
    }

    public boolean isActionBlocked() {
        if (current == null) return true;
        ItemStack in = items.getStackInSlot(SLOT_IN);
        ItemStack out = items.getStackInSlot(SLOT_OUT);
        if (!current.accepts(in, mode)) return true;

        ItemStack result = current.getOutput();
        if (result.isEmpty()) return true;
        return !(out.isEmpty()
                || (out.getItem() == result.getItem()
                    && out.getCount() + result.getCount() <= out.getMaxStackSize()));
    }

    private void chargeFromSlot() {
        ItemStack stack = items.getStackInSlot(SLOT_BATTERY);
        if (!IFItemManager.instance.isSupported(stack)) return;
        double want = Math.min(getMaxEnergy() - energy, getBandwidth());
        if (want <= 0) return;
        double gain = IFItemManager.instance.pull(stack, want, false);
        if (gain > 0) injectEnergy(gain);
    }

    public void clientTick() {
        if (level == null || !level.isClientSide) return;

        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> cn.academy.client.sound.MachineSounds.updateWorkSound(
                        this, clientWorking, cn.academy.ACSounds.MACHINE_WORK.get(),
                        be -> ((MetalFormerBlockEntity) be).clientWorking));
    }

    public boolean isClientWorking() {
        return clientWorking;
    }

    private final LazyOptional<IItemHandler> handlerDown =
            LazyOptional.of(() -> new SidedItems(new int[]{SLOT_OUT, SLOT_BATTERY}, true));
    private final LazyOptional<IItemHandler> handlerUp =
            LazyOptional.of(() -> new SidedItems(new int[]{SLOT_IN}, false));
    private final LazyOptional<IItemHandler> handlerSide =
            LazyOptional.of(() -> new SidedItems(new int[]{SLOT_BATTERY}, false));
    private final LazyOptional<IItemHandler> handlerNull = LazyOptional.of(() -> items);

    private final class SidedItems implements IItemHandler {
        private final int[] slots;
        private final boolean canExtract;

        SidedItems(int[] slots, boolean canExtract) {
            this.slots = slots;
            this.canExtract = canExtract;
        }

        @Override
        public int getSlots() {
            return slots.length;
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slots[slot]);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return items.insertItem(slots[slot], stack, simulate);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return canExtract ? items.extractItem(slots[slot], amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slots[slot]);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return items.isItemValid(slots[slot], stack);
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) return handlerNull.cast();
            return switch (side) {
                case DOWN -> handlerDown.cast();
                case UP -> handlerUp.cast();
                default -> handlerSide.cast();
            };
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerDown.invalidate();
        handlerUp.invalidate();
        handlerSide.invalidate();
        handlerNull.invalidate();
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level, getBlockPos().getX(), getBlockPos().getY(),
                        getBlockPos().getZ(), s);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.academy.metal_former");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MetalFormerMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
        mode = Mode.byOrdinal(tag.getInt("mode"));
        items.deserializeNBT(tag.getCompound("items"));
        workCounter = tag.getInt("workCounter");
        int rcp = tag.contains("recipe") ? tag.getInt("recipe") : -1;
        current = rcp < 0 ? null : MetalFormerRecipes.INSTANCE.byId(rcp);
        clientWorking = tag.getBoolean("working");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.putInt("mode", mode.ordinal());
        tag.put("items", items.serializeNBT());
        tag.putInt("workCounter", workCounter);
        tag.putInt("recipe", current == null ? -1 : current.getID());
        tag.putBoolean("working", lastWorking);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("working", lastWorking);
        tag.putInt("mode", mode.ordinal());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            clientWorking = tag.getBoolean("working");
            mode = Mode.byOrdinal(tag.getInt("mode"));
        }
    }
}
