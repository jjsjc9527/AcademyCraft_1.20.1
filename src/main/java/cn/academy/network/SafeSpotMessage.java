package cn.academy.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class SafeSpotMessage {

    private final double x;
    private final double y;
    private final double z;

    public SafeSpotMessage(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(SafeSpotMessage m, FriendlyByteBuf buf) {
        buf.writeDouble(m.x);
        buf.writeDouble(m.y);
        buf.writeDouble(m.z);
    }

    public static SafeSpotMessage decode(FriendlyByteBuf buf) {
        return new SafeSpotMessage(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static final double MAX_DRIFT_SQR = 64.0;

    public static void handle(SafeSpotMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer p = ctx.getSender();
            if (p == null) {
                return;
            }

            if (p.distanceToSqr(m.x, m.y, m.z) > MAX_DRIFT_SQR) {
                return;
            }
            cn.academy.util.ACRespawn.pushSafeSpot(p.getUUID(), m.x, m.y, m.z);
        });
        ctx.setPacketHandled(true);
    }

    public static void send(double x, double y, double z) {
        ACNetwork.CHANNEL.sendToServer(new SafeSpotMessage(x, y, z));
    }
}
