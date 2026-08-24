package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.block.block.NodeType;
import cn.academy.block.block.WirelessNodeBlock;
import cn.academy.block.container.WirelessNodeMenu;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WirelessNodeBlockEntity extends BlockEntity implements IWirelessNode, MenuProvider {

    private static final IFItemManager itemManager = IFItemManager.instance;

    private final ItemStackHandler items = new ItemStackHandler(2) {
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
    private int updateTicker = 0;
    private String nodeName = "Unnamed";
    private String password = "";
    private String placerName = "";

    public WirelessNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.WIRELESS_NODE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public NodeType getNodeType() {
        return getBlockState().getBlock() instanceof WirelessNodeBlock b ? b.getType() : NodeType.BASIC;
    }

    @Override
    public double getMaxEnergy() {
        return getNodeType().maxEnergy;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double value) {
        energy = value;
        setChanged();
    }

    @Override
    public double getBandwidth() {
        return getNodeType().bandwidth;
    }

    @Override
    public int getCapacity() {
        return getNodeType().capacity;
    }

    @Override
    public double getRange() {
        return getNodeType().range;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String name) {
        nodeName = name;
        setChanged();
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String pass) {
        password = pass;
        setChanged();
    }

    public String getPlacerName() {
        return placerName;
    }

    public void setPlacer(Player player) {
        placerName = player.getName().getString();
        setChanged();
    }

    public int getLoad() {
        var conn = WirelessHelper.getNodeConnNonCreate(this);
        return conn == null ? 0 : conn.getLoad();
    }

    public boolean isLinked() {
        return WirelessHelper.isNodeLinked(this);
    }

    public void serverTick() {
        if (++updateTicker >= 10) {
            updateTicker = 0;
            rebuildBlockState();
        }
        updateChargeIn();
        updateChargeOut();
    }

    private void rebuildBlockState() {
        if (level == null || level.isClientSide) return;
        BlockState st = getBlockState();
        if (!(st.getBlock() instanceof WirelessNodeBlock)) return;

        int pct = (int) Math.min(4, Math.round(4 * getEnergy() / getMaxEnergy()));
        boolean linked = isLinked();

        if (st.getValue(WirelessNodeBlock.ENERGY) != pct
                || st.getValue(WirelessNodeBlock.CONNECTED) != linked) {
            level.setBlock(getBlockPos(), st
                    .setValue(WirelessNodeBlock.ENERGY, pct)
                    .setValue(WirelessNodeBlock.CONNECTED, linked), 3);
        }
    }

    private void updateChargeIn() {
        ItemStack stack = items.getStackInSlot(0);
        if (itemManager.isSupported(stack)) {
            double req = Math.min(getBandwidth(), getMaxEnergy() - energy);
            double pull = itemManager.pull(stack, req, false);
            if (pull != 0) {
                setEnergy(energy + pull);
            }
        }
    }

    private void updateChargeOut() {
        ItemStack stack = items.getStackInSlot(1);
        if (itemManager.isSupported(stack)) {
            double cur = getEnergy();
            if (cur > 0) {
                cur = Math.min(getBandwidth(), cur);
                double left = itemManager.charge(stack, cur);
                if (left != cur) {
                    setEnergy(getEnergy() - (cur - left));
                }
            }
        }
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) {
                Containers.dropItemStack(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), s);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WirelessNodeMenu(id, inv, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
        items.deserializeNBT(tag.getCompound("items"));
        if (tag.contains("nodeName")) nodeName = tag.getString("nodeName");
        password = tag.getString("password");
        placerName = tag.getString("placer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
        tag.put("items", items.serializeNBT());
        tag.putString("nodeName", nodeName);
        tag.putString("password", password);
        tag.putString("placer", placerName);
    }
}
