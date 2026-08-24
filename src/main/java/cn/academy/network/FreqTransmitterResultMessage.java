package cn.academy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FreqTransmitterResultMessage {

    public interface Callback {
        void accept(boolean success, String text);
    }

    private static int nextReqId = 1;
    private static final Map<Integer, Callback> pending = new HashMap<>();

    public static int register(Callback cb) {
        int id = nextReqId++;
        pending.put(id, cb);
        return id;
    }

    public static void clearPending() {
        pending.clear();
    }

    private final int reqId;
    private final boolean success;
    private final String text;

    public FreqTransmitterResultMessage(int reqId, boolean success, String text) {
        this.reqId = reqId;
        this.success = success;
        this.text = text == null ? "" : text;
    }

    public static void encode(FreqTransmitterResultMessage m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.reqId);
        buf.writeBoolean(m.success);
        buf.writeUtf(m.text, 128);
    }

    public static FreqTransmitterResultMessage decode(FriendlyByteBuf buf) {
        return new FreqTransmitterResultMessage(buf.readVarInt(), buf.readBoolean(), buf.readUtf(128));
    }

    public static void handle(FreqTransmitterResultMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() ->

                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> deliver(m)));
        ctx.setPacketHandled(true);
    }

    private static void deliver(FreqTransmitterResultMessage m) {
        Callback cb = pending.remove(m.reqId);
        if (cb != null) {
            cb.accept(m.success, m.text);
        }
    }
}
