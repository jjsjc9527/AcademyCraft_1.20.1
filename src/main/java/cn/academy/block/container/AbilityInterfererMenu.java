package cn.academy.block.container;

import cn.academy.ACMenus;
import cn.academy.block.tileentity.AbilityInterfererBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class AbilityInterfererMenu extends TechUIMenu {

    private final ContainerData data;

    public AbilityInterfererMenu(int id, Inventory playerInv, AbilityInterfererBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getItems(), new ContainerData() {
            @Override
            public int get(int index) {
                return (int) be.getEnergy();
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 1;
            }
        });
    }

    public AbilityInterfererMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, clientInv(), new SimpleContainerData(1));
    }

    private AbilityInterfererMenu(int id, Inventory playerInv, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ACMenus.ABILITY_INTERFERER.get(), id, pos);
        this.data = data;
        addMachineSlot(items, AbilityInterfererBlockEntity.SLOT_BATTERY, 141, 25);
        addPlayerHotbar(playerInv);
        addDataSlots(data);
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return AbilityInterfererBlockEntity.isSlotValid(slot, stack);
            }
        };
    }

    public int getEnergy() {
        return data.get(0);
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}
