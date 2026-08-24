package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.ACMenus;
import cn.academy.block.tileentity.PhaseGenBlockEntity;
import cn.academy.energy.api.item.ImagEnergyItem;
import cn.academy.item.MatterUnitItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class PhaseGenMenu extends TechUIMenu {

    private static final int IN_X = 47, IN_Y = 12;
    private static final int LIQ_OUT_X = 114, LIQ_OUT_Y = 51;
    private static final int OUT_X = 44, OUT_Y = 80;

    private final ContainerData data;

    public PhaseGenMenu(int id, Inventory playerInv, PhaseGenBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getItems(), new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? (int) be.getEnergy() : be.getLiquidAmount();
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 2;
            }
        });
    }

    public PhaseGenMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, clientInv(), new SimpleContainerData(2));
    }

    private PhaseGenMenu(int id, Inventory playerInv, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ACMenus.PHASE_GEN.get(), id, pos);
        this.data = data;
        addMachineSlot(items, PhaseGenBlockEntity.SLOT_LIQUID_IN, IN_X, IN_Y);
        addMachineSlot(items, PhaseGenBlockEntity.SLOT_LIQUID_OUT, LIQ_OUT_X, LIQ_OUT_Y);
        addMachineSlot(items, PhaseGenBlockEntity.SLOT_OUTPUT, OUT_X, OUT_Y);
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return switch (slot) {
                    case PhaseGenBlockEntity.SLOT_LIQUID_IN ->
                            ACItems.MATTER_UNIT.get().is(stack, MatterUnitItem.MAT_PHASE_LIQUID);
                    case PhaseGenBlockEntity.SLOT_LIQUID_OUT -> stack.getItem() instanceof MatterUnitItem;
                    default -> stack.getItem() instanceof ImagEnergyItem;
                };
            }
        };
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getLiquid() {
        return data.get(1);
    }

    @Override
    protected int machineSlotCount() {
        return 3;
    }
}
