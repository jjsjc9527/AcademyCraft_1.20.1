package cn.academy.block.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public abstract class TechUIMenu extends AbstractContainerMenu {

    public static final int INV_X = 8;
    public static final int INV_Y = 105;
    public static final int HOTBAR_Y = 163;

    private final BlockPos pos;
    private int resyncTimer = 0;

    private boolean slotsActive = true;

    protected TechUIMenu(@Nullable MenuType<?> type, int id, BlockPos pos) {
        super(type, id);
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setSlotsActive(boolean active) {
        this.slotsActive = active;
    }

    public boolean areSlotsActive() {
        return slotsActive;
    }

    protected abstract int machineSlotCount();

    protected void addMachineSlot(IItemHandler handler, int index, int x, int y) {
        addSlot(new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean isActive() {
                return slotsActive;
            }
        });
    }

    protected void addPlayerInventory(Inventory playerInv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(pagedSlot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(pagedSlot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    protected void addPlayerHotbar(Inventory playerInv) {
        for (int col = 0; col < 9; col++) {
            addSlot(pagedSlot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    private Slot pagedSlot(Inventory inv, int index, int x, int y) {
        return new Slot(inv, index, x, y) {
            @Override
            public boolean isActive() {
                return slotsActive;
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return true;
        }
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (resyncTimer >= 0 && ++resyncTimer >= 2) {
            resyncTimer = -1;
            broadcastFullState();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            int machineEnd = machineSlotCount();
            int invEnd = slots.size();

            if (index < machineEnd) {
                if (!moveItemStackTo(stack, machineEnd, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stack, 0, machineEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
