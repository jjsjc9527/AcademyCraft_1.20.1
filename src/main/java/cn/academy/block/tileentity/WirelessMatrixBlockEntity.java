package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.ACItems;
import cn.academy.block.container.WirelessMatrixMenu;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.event.energy.LinkNodeEvent;
import cn.academy.item.MatrixCoreItem;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.WorldUtils;
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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WirelessMatrixBlockEntity extends BlockEntity implements IWirelessMatrix, MenuProvider {

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 3) return stack.getItem() instanceof MatrixCoreItem;
            return stack.getItem() == ACItems.CONSTRAINT_PLATE.get();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                onInventoryChanged();
            }
        }
    };

    private final LazyOptional<IItemHandler> itemsCap = LazyOptional.of(() -> items);

    private String placerName = "";

    private boolean clientWorking = false;

    private int linkTimer = 0;

    public WirelessMatrixBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.WIRELESS_MATRIX.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public int getPlateCount() {
        int count = 0;
        for (int i = 0; i < 3; ++i) {
            if (!items.getStackInSlot(i).isEmpty()) count++;
        }
        return count;
    }

    public int getCoreLevel() {
        return MatrixCoreItem.levelOf(items.getStackInSlot(3));
    }

    public boolean isWorking() {
        return getCoreLevel() > 0 && getPlateCount() == 3;
    }

    @Override
    public int getCapacity() {
        return isWorking() ? 8 * getCoreLevel() : 0;
    }

    @Override
    public double getBandwidth() {
        int L = getCoreLevel();
        return isWorking() ? (double) L * L * 60 : 0;
    }

    @Override
    public double getRange() {
        return isWorking() ? 24 * Math.sqrt(getCoreLevel()) : 0;
    }

    public String getPlacerName() {
        return placerName;
    }

    public void setPlacer(Player player) {
        placerName = player.getName().getString();
        setChanged();
    }

    private void onInventoryChanged() {
        boolean w = isWorking();
        if (w != clientWorking) {
            clientWorking = w;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isRenderShields() {
        return clientWorking;
    }

    public void serverTick() {

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
        return Component.translatable("block.academy.matrix");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WirelessMatrixMenu(id, inv, this);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemsCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemsCap.invalidate();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("working", isWorking());
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        clientWorking = tag.getBoolean("working");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            clientWorking = pkt.getTag().getBoolean("working");
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("items"));
        placerName = tag.getString("placer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", items.serializeNBT());
        tag.putString("placer", placerName);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(2.0, 3.0, 2.0);
    }
}
