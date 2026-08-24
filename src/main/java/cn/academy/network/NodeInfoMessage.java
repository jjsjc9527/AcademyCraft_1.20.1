package cn.academy.network;

import cn.academy.block.tileentity.WirelessNodeBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NodeInfoMessage {

    public final String owner;
    public final String nodeName;
    public final String password;

    public NodeInfoMessage(String owner, String nodeName, String password) {
        this.owner = owner == null ? "" : owner;
        this.nodeName = nodeName == null ? "" : nodeName;
        this.password = password == null ? "" : password;
    }

    public static NodeInfoMessage gather(WirelessNodeBlockEntity be) {
        return new NodeInfoMessage(be.getPlacerName(), be.getNodeName(), be.getPassword());
    }

    public static void encode(NodeInfoMessage m, FriendlyByteBuf buf) {
        buf.writeUtf(m.owner, 64);
        buf.writeUtf(m.nodeName, 64);
        buf.writeUtf(m.password, 64);
    }

    public static NodeInfoMessage decode(FriendlyByteBuf buf) {
        return new NodeInfoMessage(buf.readUtf(64), buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(NodeInfoMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.academy.client.gui.NodeInfoClient.accept(m)));
        ctx.setPacketHandled(true);
    }
}
