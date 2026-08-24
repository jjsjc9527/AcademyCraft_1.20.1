package cn.lambdalib2.datapart;

import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.Debug;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

public abstract class DataPart<T extends Entity> {

    private static final String CH_SYNC = "itn_sync";
    private static final String CH_QUERY_INIT = "itn_query_init";

    EntityData<T> entityData;
    private boolean syncInit = false;

    boolean needNBTStorage = false;
    boolean needTick = false;
    boolean clientNeedSync = false;
    double serverSyncRange = 10.0;

    protected final void setTick(boolean state) {
        needTick = state;
    }

    protected final void setNBTStorage() {
        needNBTStorage = true;
    }

    protected final void setClientNeedSync() {
        clientNeedSync = true;
    }

    protected final void setServerSyncRange(double range) {
        serverSyncRange = range;
    }

    public final void sync() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writeSyncData(buf);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        sendMessage(CH_SYNC, (Object) data);
    }

    protected void writeSyncData(FriendlyByteBuf buf) {}

    protected void readSyncData(FriendlyByteBuf buf) {}

    protected void sendMessage(String channel, Object... params) {
        if (isClient()) {
            NetworkMessage.sendToServer(this, channel, params);
        } else {
            NetworkMessage.sendToTracking(getEntity(), this, channel, params);
        }
    }

    protected void sendToLocal(String channel, Object... params) {
        if (getEntity() instanceof Player p) {
            NetworkMessage.sendTo(p, this, channel, params);
        } else {
            throw new IllegalStateException("Not a DataPart of Player");
        }
    }

    @Listener(channel = CH_SYNC, side = {LogicalSide.CLIENT, LogicalSide.SERVER})
    public void onSyncReceived(byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        readSyncData(buf);
        onSynchronized();
    }

    @Listener(channel = CH_QUERY_INIT, side = LogicalSide.SERVER)
    public void onQueryInit() {
        sync();
    }

    public static boolean isReadOnlyQuery(String channel) {
        return CH_QUERY_INIT.equals(channel);
    }

    public void wake() {}

    public void tick() {}

    protected void onSynchronized() {}

    public void toNBT(net.minecraft.nbt.CompoundTag tag) {}

    public void fromNBT(net.minecraft.nbt.CompoundTag tag) {}

    public void onPlayerDead() {}

    protected boolean isClient() {
        return getEntity().level().isClientSide();
    }

    protected LogicalSide getSide() {
        return isClient() ? LogicalSide.CLIENT : LogicalSide.SERVER;
    }

    public T getEntity() {
        return entityData.getEntity();
    }

    public EntityData<T> getData() {
        return entityData;
    }

    protected void checkSide(LogicalSide side) {
        if (isClient() != side.isClient()) {
            throw new IllegalStateException("Invalid side, expected " + side);
        }
    }

    protected boolean checkSideSoft(LogicalSide side) {
        return isClient() == side.isClient();
    }

    protected void debug(Object message) {
        Debug.log(String.valueOf(message));
    }

    void callTick() {
        if (isClient() && clientNeedSync && !syncInit) {
            syncInit = true;
            sendMessage(CH_QUERY_INIT);
        }
        if (needTick) {
            tick();
        }
    }

}
