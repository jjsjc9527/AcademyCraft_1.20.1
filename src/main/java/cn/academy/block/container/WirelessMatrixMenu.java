package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.ACMenus;
import cn.academy.block.tileentity.WirelessMatrixBlockEntity;
import cn.academy.item.MatrixCoreItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class WirelessMatrixMenu extends TechUIMenu {

    private static final int[][] MACHINE_SLOTS = {
            {0, 80, 11},
            {1, 55, 60},
            {2, 106, 60},
            {3, 80, 36},
    };

    public WirelessMatrixMenu(int id, Inventory playerInv, WirelessMatrixBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getItems());
    }

    public WirelessMatrixMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, clientInv());
    }

    private WirelessMatrixMenu(int id, Inventory playerInv, BlockPos pos, IItemHandler items) {
        super(ACMenus.WIRELESS_MATRIX.get(), id, pos);
        for (int[] s : MACHINE_SLOTS) {
            addMachineSlot(items, s[0], s[1], s[2]);
        }
        addPlayerInventory(playerInv);
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(4) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                if (slot == 3) return stack.getItem() instanceof MatrixCoreItem;
                return stack.getItem() == ACItems.CONSTRAINT_PLATE.get();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    @Override
    protected int machineSlotCount() {
        return MACHINE_SLOTS.length;
    }
}
