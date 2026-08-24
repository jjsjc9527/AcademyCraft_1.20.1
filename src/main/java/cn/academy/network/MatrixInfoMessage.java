package cn.academy.network;

import cn.academy.block.tileentity.WirelessMatrixBlockEntity;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.impl.WirelessNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MatrixInfoMessage {

    public final boolean initialized;
    public final String ssid;
    public final String pass;
    public final String owner;
    public final int load;
    public final int capacity;
    public final double range;
    public final double bandwidth;

    public MatrixInfoMessage(boolean initialized, String ssid, String pass, String owner,
                             int load, int capacity, double range, double bandwidth) {
        this.initialized = initialized;
        this.ssid = ssid == null ? "" : ssid;
        this.pass = pass == null ? "" : pass;
        this.owner = owner == null ? "" : owner;
        this.load = load;
        this.capacity = capacity;
        this.range = range;
        this.bandwidth = bandwidth;
    }

    public static MatrixInfoMessage gather(WirelessMatrixBlockEntity be) {
        WirelessNet net = WirelessHelper.getWirelessNet(be);
        boolean init = net != null;
        return new MatrixInfoMessage(
                init,
                init ? net.getSSID() : "",
                init ? net.getPassword() : "",
                be.getPlacerName(),
                init ? net.getLoad() : 0,
                be.getCapacity(),
                be.getRange(),
                be.getBandwidth());
    }

    public static void encode(MatrixInfoMessage m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.initialized);
        buf.writeUtf(m.ssid, 64);
        buf.writeUtf(m.pass, 64);
        buf.writeUtf(m.owner, 64);
        buf.writeInt(m.load);
        buf.writeInt(m.capacity);
        buf.writeDouble(m.range);
        buf.writeDouble(m.bandwidth);
    }

    public static MatrixInfoMessage decode(FriendlyByteBuf buf) {
        return new MatrixInfoMessage(
                buf.readBoolean(),
                buf.readUtf(64), buf.readUtf(64), buf.readUtf(64),
                buf.readInt(), buf.readInt(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(MatrixInfoMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.academy.client.gui.MatrixInfoClient.accept(m)));
        ctx.setPacketHandled(true);
    }
}
