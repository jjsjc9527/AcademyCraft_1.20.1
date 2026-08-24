package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.ACBlocks;
import cn.academy.block.WindgenConsts;
import cn.academy.block.container.WindgenBaseMenu;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.item.ImagEnergyItem;
import cn.academy.event.energy.LinkUserEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WindgenBaseBlockEntity extends BlockEntity implements IWirelessGenerator, MenuProvider {

    public static final int COMP_BASE_ONLY = 0;
    public static final int COMP_NO_TOP = 1;
    public static final int COMP_COMPLETE_NOT_WORKING = 2;
    public static final int COMP_COMPLETE = 3;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof ImagEnergyItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private double energy = 0;
    private int completeness = COMP_BASE_ONLY;
    private int checkTimer = 0, linkTimer = 0;

    public WindgenBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.WINDGEN_BASE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public double getEnergy() {
        return energy;
    }

    public int getCompleteness() {
        return completeness;
    }

    @Override
    public double getProvidedEnergy(double req) {
        double give = Math.min(energy, req);
        energy -= give;
        setChanged();
        return give;
    }

    @Override
    public double getBandwidth() {
        return 50;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++checkTimer >= WindgenConsts.CHECK_INTERVAL) {
            checkTimer = 0;
            updateAndGenerate();
        }

        tryChargeStack(items.getStackInSlot(0));
    }

    private void tryChargeStack(ItemStack stack) {
        if (!IFItemManager.instance.isSupported(stack)) {
            return;
        }
        double cangive = Math.min(energy, getBandwidth());
        double ret = IFItemManager.instance.charge(stack, cangive);
        double moved = cangive - ret;
        if (moved != 0) {
            energy -= moved;
            setChanged();
        }
    }

    private void updateAndGenerate() {
        BlockPos p = getBlockPos();
        int pillars = 0;
        WindgenMainBlockEntity main = null;
        for (int y = p.getY() + 2; y < level.getMaxBuildHeight(); y++) {
            BlockPos up = new BlockPos(p.getX(), y, p.getZ());
            Block b = level.getBlockState(up).getBlock();
            if (b == ACBlocks.WINDGEN_PILLAR.get()) {
                pillars++;
                if (pillars > WindgenConsts.MAX_PILLARS) break;
            } else if (level.getBlockEntity(up) instanceof WindgenMainBlockEntity m) {
                main = m;
                break;
            } else {
                break;
            }
        }

        boolean structureOk = main != null
                && pillars >= WindgenConsts.MIN_PILLARS
                && pillars <= WindgenConsts.MAX_PILLARS;
        boolean working = structureOk && main.isFanInstalled() && main.isNoObstacle();

        completeness = structureOk ? (working ? COMP_COMPLETE : COMP_COMPLETE_NOT_WORKING)
                : (pillars > 0 ? COMP_NO_TOP : COMP_BASE_ONLY);

        if (main != null) {
            main.setWorking(working);
            main.setPillars(pillars);
        }

        if (working) {
            double perTick = WindgenConsts.heightFactor(main.getBlockPos().getY()) * WindgenConsts.MAX_GEN_SPEED;
            energy = Math.min(WindgenConsts.BUFFER_SIZE, energy + perTick * WindgenConsts.CHECK_INTERVAL);
            setChanged();
        }
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
        return Component.translatable("block.academy.windgen_base");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WindgenBaseMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
        items.deserializeNBT(tag.getCompound("items"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.put("items", items.serializeNBT());
    }

}
