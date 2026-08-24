package cn.academy.block.container;

import cn.academy.ACMenus;
import cn.academy.block.tileentity.WirelessGeneratorBlockEntity;
import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class WirelessGeneratorMenu extends TechUIMenu {

    private static final int SLOT_X = 44, SLOT_Y = 81;

    private final ContainerData data;

    public WirelessGeneratorMenu(int id, Inventory playerInv, WirelessGeneratorBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getItems(), new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? (int) be.getEnergy() : be.getStatus();
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        });
    }

    public WirelessGeneratorMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, clientInv(), new SimpleContainerData(2));
    }

    private WirelessGeneratorMenu(int id, Inventory playerInv, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ACMenus.WIRELESS_GENERATOR.get(), id, pos);
        this.data = data;
        addMachineSlot(items, 0, SLOT_X, SLOT_Y);
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.getItem() instanceof ImagEnergyItem;
            }
        };
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getStatus() {
        return data.get(1);
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}
