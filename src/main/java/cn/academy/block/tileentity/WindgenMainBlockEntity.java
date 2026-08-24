package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.ACItems;
import cn.academy.block.WindgenConsts;
import cn.academy.block.container.WindgenMainMenu;
import cn.academy.block.block.ACMultiBlock;
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
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WindgenMainBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == ACItems.WINDGEN_FAN.get();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sync();
        }
    };

    private boolean working = false;
    private boolean noObstacle = false;
    private int pillars = 0;
    private int obsTimer = 0;

    public WindgenMainBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.WINDGEN_MAIN.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public boolean isFanInstalled() {
        return !items.getStackInSlot(0).isEmpty();
    }

    public boolean isNoObstacle() { return noObstacle; }
    public boolean isWorking() { return working; }
    public int getPillars() { return pillars; }

    public void setWorking(boolean w) {
        if (w != working) {
            working = w;
            sync();
        }
    }

    public void setPillars(int p) {
        if (p != pillars) {
            pillars = p;
            sync();
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++obsTimer >= WindgenConsts.CHECK_INTERVAL) {
            obsTimer = 0;
            noObstacle = computeNoObstacle();
        }
    }

    private boolean computeNoObstacle() {
        BlockPos p = getBlockPos();
        Direction dir = getBlockState().getBlock() instanceof ACMultiBlock mb
                ? mb.facingOf(getBlockState()) : Direction.SOUTH;
        int R = WindgenConsts.OBSTACLE_RADIUS;
        for (int dx = -R; dx <= R; dx++) {
            for (int dy = -R; dy <= R; dy++) {
                if (dx == 0 && dy == 0) continue;
                BlockPos off = ACMultiBlock.rotate(new int[]{dx, dy, -1}, dir);
                if (!level.getBlockState(p.offset(off)).isAir()) {
                    return false;
                }
            }
        }
        return true;
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
        return Component.translatable("block.academy.windgen_main");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WindgenMainMenu(id, inv, this);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        saveAdditional(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("items"));

        if (tag.getBoolean("fan") && items.getStackInSlot(0).isEmpty()) {
            items.setStackInSlot(0, new ItemStack(ACItems.WINDGEN_FAN.get()));
        }
        working = tag.getBoolean("working");
        pillars = tag.getInt("pillars");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", items.serializeNBT());
        tag.putBoolean("working", working);
        tag.putInt("pillars", pillars);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(12.0);
    }

}
