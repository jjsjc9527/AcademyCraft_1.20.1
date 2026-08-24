package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.event.energy.LinkUserEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

public class WirelessReceiverBlockEntity extends BlockEntity implements IWirelessReceiver {

    private static final double MAX_BUFFER = 5000;

    private double buffer = 0;
    private int linkTimer = 0;

    public WirelessReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.WIRELESS_RECEIVER.get(), pos, state);
    }

    @Override
    public double getRequiredEnergy() {
        return MAX_BUFFER - buffer;
    }

    @Override
    public double injectEnergy(double amt) {
        double space = MAX_BUFFER - buffer;
        double take = Math.min(amt, space);
        buffer += take;
        setChanged();
        return amt - take;
    }

    @Override
    public double pullEnergy(double amt) {
        double give = Math.min(amt, buffer);
        buffer -= give;
        setChanged();
        return give;
    }

    @Override
    public double getBandwidth() {
        return 50;
    }

    public double getBuffer() {
        return buffer;
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!WirelessHelper.isReceiverLinked(this)) {
            if (++linkTimer >= 20) {
                linkTimer = 0;
                tryLink();
            }
        }
    }

    private void tryLink() {
        List<IWirelessNode> nodes = WirelessHelper.getNodesInRange(level, getBlockPos());
        if (!nodes.isEmpty()) {
            MinecraftForge.EVENT_BUS.post(new LinkUserEvent(this, nodes.get(0)));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        buffer = tag.getDouble("buffer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("buffer", buffer);
    }
}
