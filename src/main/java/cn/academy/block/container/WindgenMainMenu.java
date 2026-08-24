package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.ACMenus;
import cn.academy.block.tileentity.WindgenMainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class WindgenMainMenu extends TechUIMenu {

    private static final int SLOT_X = 80, SLOT_Y = 9;

    public WindgenMainMenu(int id, Inventory playerInv, WindgenMainBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getItems());
    }

    public WindgenMainMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, clientInv());
    }

    private WindgenMainMenu(int id, Inventory playerInv, BlockPos pos, IItemHandler items) {
        super(ACMenus.WINDGEN_MAIN.get(), id, pos);
        addMachineSlot(items, 0, SLOT_X, SLOT_Y);
        addPlayerInventory(playerInv);
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.getItem() == ACItems.WINDGEN_FAN.get();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}
