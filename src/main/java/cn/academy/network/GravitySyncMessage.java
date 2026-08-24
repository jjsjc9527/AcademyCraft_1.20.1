package cn.academy.network;

import cn.academy.gravity.GravityClientHandler;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class GravitySyncMessage {

    private final int entityId;
    private final int dir3d;
    private final boolean animate;
    private final boolean init;

    public GravitySyncMessage(int entityId, Direction dir, boolean animate, boolean init) {
        this(entityId, dir.get3DDataValue(), animate, init);
    }

    private GravitySyncMessage(int entityId, int dir3d, boolean animate, boolean init) {
        this.entityId = entityId;
        this.dir3d = dir3d;
        this.animate = animate;
        this.init = init;
    }

    public static void encode(GravitySyncMessage m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entityId);
        buf.writeByte(m.dir3d);
        buf.writeBoolean(m.animate);
        buf.writeBoolean(m.init);
    }

    public static GravitySyncMessage decode(FriendlyByteBuf buf) {
        return new GravitySyncMessage(buf.readVarInt(), buf.readByte(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(GravitySyncMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> GravityClientHandler.apply(m.entityId, m.dir3d, m.animate, m.init)));
        ctx.setPacketHandled(true);
    }

    public static void sync(Entity entity, Direction dir, boolean animate) {
        sync(entity, dir, animate, false);
    }

    public static void sync(Entity entity, Direction dir, boolean animate, boolean init) {
        ACNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new GravitySyncMessage(entity.getId(), dir, animate, init));
    }

    public static void syncTo(ServerPlayer viewer, Entity target, Direction dir) {
        ACNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> viewer),
                new GravitySyncMessage(target.getId(), dir, false, true));
    }
}
