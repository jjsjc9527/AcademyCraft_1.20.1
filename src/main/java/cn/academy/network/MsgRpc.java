package cn.academy.network;

import cn.lambdalib2.s11n.network.NetworkMessage;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgRpc {

    private final byte[] data;

    public MsgRpc(byte[] data) {
        this.data = data;
    }

    public static void encode(MsgRpc msg, FriendlyByteBuf buf) {
        buf.writeByteArray(msg.data);
    }

    public static MsgRpc decode(FriendlyByteBuf buf) {
        return new MsgRpc(buf.readByteArray());
    }

    public static void handle(MsgRpc msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            LogicalSide side = ctx.getDirection().getReceptionSide();
            Level level;

            net.minecraft.server.level.ServerPlayer sender = null;
            if (side == LogicalSide.SERVER) {
                sender = ctx.getSender();
                if (sender == null) {
                    return;
                }
                level = sender.level();
            } else {

                level = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientNetAccess::clientLevel);
            }
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(msg.data));
            NetworkMessage.dispatch(buf, side, level, sender);
        });
        ctx.setPacketHandled(true);
    }
}
