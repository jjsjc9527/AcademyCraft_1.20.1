package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.ACMenus;
import cn.academy.block.tileentity.ImagFusorBlockEntity;
import cn.academy.crafting.ImagFusorRecipes;
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

public class ImagFusorMenu extends TechUIMenu {

    private final ContainerData data;

    public ImagFusorMenu(int id, Inventory playerInv, ImagFusorBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getItems(), new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) be.getEnergy();
                    case 1 -> be.getLiquidAmount();
                    case 2 -> (int) (be.getWorkProgress() * 1000);
                    default -> be.getCurrentRecipe() == null ? -1 : be.getCurrentRecipe().getID();
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 4;
            }
        });
    }

    public ImagFusorMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, clientInv(), new SimpleContainerData(4));
    }

    private ImagFusorMenu(int id, Inventory playerInv, BlockPos pos, IItemHandler items, ContainerData data) {
        super(ACMenus.IMAG_FUSOR.get(), id, pos);
        this.data = data;
        addMachineSlot(items, ImagFusorBlockEntity.SLOT_INPUT, 15, 49);
        addMachineSlot(items, ImagFusorBlockEntity.SLOT_OUTPUT, 145, 49);
        addMachineSlot(items, ImagFusorBlockEntity.SLOT_IMAG_INPUT, 15, 10);
        addMachineSlot(items, ImagFusorBlockEntity.SLOT_ENERGY_INPUT, 44, 80);
        addMachineSlot(items, ImagFusorBlockEntity.SLOT_IMAG_OUTPUT, 145, 10);
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(5) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return switch (slot) {
                    case ImagFusorBlockEntity.SLOT_INPUT -> ImagFusorRecipes.INSTANCE.getRecipe(stack) != null;
                    case ImagFusorBlockEntity.SLOT_OUTPUT -> false;
                    case ImagFusorBlockEntity.SLOT_IMAG_INPUT ->
                            ACItems.MATTER_UNIT.get().is(stack, MatterUnitItem.MAT_PHASE_LIQUID);
                    case ImagFusorBlockEntity.SLOT_IMAG_OUTPUT -> stack.getItem() instanceof MatterUnitItem;
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

    public double getProgress() {
        return data.get(2) / 1000.0;
    }

    public ImagFusorRecipes.IFRecipe getCurrentRecipe() {
        return ImagFusorRecipes.INSTANCE.byId(data.get(3));
    }

    @Override
    protected int machineSlotCount() {
        return 5;
    }
}
