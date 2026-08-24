package cn.academy.block.container;

import cn.academy.ACMenus;
import cn.academy.block.block.NodeType;
import cn.academy.block.block.WirelessNodeBlock;
import cn.academy.block.tileentity.WirelessNodeBlockEntity;
import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class WirelessNodeMenu extends TechUIMenu {

    private static final int IN_X = 44, IN_Y = 10;
    private static final int OUT_X = 44, OUT_Y = 80;

    private final ContainerData data;
    private final NodeType type;

    public WirelessNodeMenu(int id, Inventory playerInv, WirelessNodeBlockEntity be) {
        this(id, playerInv, be.getBlockPos(), be.getNodeType(), be.getItems(), new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> (int) be.getEnergy();
                    case 1 -> be.getLoad();
                    default -> be.isLinked() ? 1 : 0;
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

    public WirelessNodeMenu(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, pos, typeAt(playerInv, pos), clientInv(), new SimpleContainerData(3));
    }

    private WirelessNodeMenu(int id, Inventory playerInv, BlockPos pos, NodeType type,
                             IItemHandler items, ContainerData data) {
        super(ACMenus.WIRELESS_NODE.get(), id, pos);
        this.data = data;
        this.type = type;
        addMachineSlot(items, 0, IN_X, IN_Y);
        addMachineSlot(items, 1, OUT_X, OUT_Y);
        addPlayerInventory(playerInv);
        addDataSlots(data);
    }

    private static NodeType typeAt(Inventory playerInv, BlockPos pos) {
        return playerInv.player.level().getBlockState(pos).getBlock() instanceof WirelessNodeBlock b
                ? b.getType() : NodeType.BASIC;
    }

    public NodeType getNodeType() {
        return type;
    }

    private static ItemStackHandler clientInv() {
        return new ItemStackHandler(2) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.getItem() instanceof ImagEnergyItem;
            }
        };
    }

    public int getEnergy() {
        return data.get(0);
    }

    public int getLoad() {
        return data.get(1);
    }

    public boolean isLinked() {
        return data.get(2) != 0;
    }

    @Override
    protected int machineSlotCount() {
        return 2;
    }
}
