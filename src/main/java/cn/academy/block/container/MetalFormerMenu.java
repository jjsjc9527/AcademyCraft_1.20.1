package cn.academy.block.container;

import cn.academy.ACMenus;
import cn.academy.block.tileentity.MetalFormerBlockEntity;
import cn.academy.crafting.MetalFormerRecipes.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class MetalFormerMenu extends TechUIMenu {

    private final ContainerData data;

    public MetalFormerMenu(int id, Inventory playerInv, MetalFormerBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getItems(), new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) be.getEnergy();
                    case 1 -> (int) (be.getWorkProgress() * 1000);
                    default -> be.getMode().ordinal();
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 3;
            }
        });
    }

    public MetalFormerMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, clientInv(), new SimpleContainerData(3));
    }

    private MetalFormerMenu(int id, Inventory playerInv, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ACMenus.METAL_FORMER.get(), id, pos);
        this.data = data;
        addMachineSlot(items, MetalFormerBlockEntity.SLOT_IN, 15, 49);
        addMachineSlot(items, MetalFormerBlockEntity.SLOT_OUT, 145, 49);
        addMachineSlot(items, MetalFormerBlockEntity.SLOT_BATTERY, 44, 80);
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return MetalFormerBlockEntity.isSlotValid(slot, stack);
            }
        };
    }

    public int getEnergy() {
        return data.get(0);
    }

    public double getProgress() {
        return data.get(1) / 1000.0;
    }

    public Mode getMode() {
        return Mode.byOrdinal(data.get(2));
    }

    @Override
    protected int machineSlotCount() {
        return 3;
    }
}
