package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.block.block.DeveloperBlock;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.network.DeveloperActionMessage;
import cn.academy.network.DeveloperOpenMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class DeveloperBlockEntity extends BlockEntity implements IWirelessReceiver, IDeveloper {

    private double energy = 0;
    @Nullable
    private Player user;

    public DeveloperBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.DEVELOPER.get(), pos, state);
    }

    @Override
    public DeveloperType getDeveloperType() {
        return getBlockState().getBlock() instanceof DeveloperBlock b ? b.type : DeveloperType.NORMAL;
    }

    @Override
    public boolean tryPullEnergy(double amount) {
        if (energy < amount) return false;
        energy -= amount;
        setChanged();
        return true;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public double getMaxEnergy() {
        return getDeveloperType().getEnergy();
    }

    @Override
    public void onGuiClosed() {
        if (level != null && level.isClientSide) {
            DeveloperActionMessage.sendUnuse(getBlockPos());
        } else {
            unuse();
        }
    }

    @Override
    public double getRequiredEnergy() {
        return getMaxEnergy() - energy;
    }

    @Override
    public double injectEnergy(double amt) {
        double give = Math.min(amt, getMaxEnergy() - energy);
        energy += give;
        setChanged();
        return amt - give;
    }

    @Override
    public double pullEnergy(double amt) {
        double a = Math.min(amt, energy);
        energy -= a;
        setChanged();
        return a;
    }

    @Override
    public double getBandwidth() {
        return getDeveloperType().getBandwidth();
    }

    @Nullable
    public Player getUser() {
        return user;
    }

    public void use(Player player) {
        if (level == null || level.isClientSide) return;
        if (user != null) unuse();
        user = player;

        if (player instanceof ServerPlayer sp) {
            DeveloperOpenMessage.send(sp, getBlockPos());
        }
    }

    public void unuse(Player player) {
        if (user != null && user.equals(player)) unuse();
    }

    private void unuse() {
        user = null;
    }

    private static final int UPDATE_WAIT = 20;
    private int updateTicker = 0;

    public void serverTick() {
        if (level == null || level.isClientSide) return;

        if (++updateTicker >= UPDATE_WAIT) {
            updateTicker = 0;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }

        if (user != null && (user.isRemoved() || user.distanceToSqr(
                getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5) > 100)) {
            unuse();
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy = tag.getDouble("energy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("energy", energy);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putDouble("energy", energy);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(3);
    }
}
