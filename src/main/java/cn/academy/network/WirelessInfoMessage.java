package cn.academy.network;

import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.impl.NodeConn;
import cn.academy.energy.impl.WirelessNet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class WirelessInfoMessage {

    public record Entry(BlockPos pos, String name, boolean encrypted) {}

    public final boolean linked;
    public final Entry linkedNode;
    public final List<Entry> avail;

    public WirelessInfoMessage(boolean linked, Entry linkedNode, List<Entry> avail) {
        this.linked = linked;
        this.linkedNode = linkedNode;
        this.avail = avail;
    }

    private static Entry of(IWirelessNode node) {
        BlockPos p = ((BlockEntity) node).getBlockPos();
        return new Entry(p, node.getNodeName(), !node.getPassword().isEmpty());
    }

    private static Entry of(WirelessNet net) {
        IWirelessMatrix mat = net.getMatrix();
        BlockPos p = mat == null ? BlockPos.ZERO : ((BlockEntity) mat).getBlockPos();
        return new Entry(p, net.getSSID(), !net.getPassword().isEmpty());
    }

    private static final Entry NONE = new Entry(BlockPos.ZERO, "", false);

    public static WirelessInfoMessage gather(IWirelessTile tile) {
        if (tile instanceof IWirelessNode node) {
            return gatherNode(node);
        }
        return gatherUser((IWirelessUser) tile);
    }

    private static WirelessInfoMessage gatherUser(IWirelessUser user) {
        BlockEntity te = (BlockEntity) user;
        NodeConn conn = WirelessHelper.getNodeConn(user);
        IWirelessNode linkedNode = conn == null ? null : conn.getNode();

        List<Entry> avail = new ArrayList<>();
        for (IWirelessNode n : WirelessHelper.getNodesInRange(te.getLevel(), te.getBlockPos())) {
            if (linkedNode != null && ((BlockEntity) n).getBlockPos().equals(((BlockEntity) linkedNode).getBlockPos())) {
                continue;
            }
            avail.add(of(n));
        }
        return new WirelessInfoMessage(linkedNode != null,
                linkedNode != null ? of(linkedNode) : NONE, avail);
    }

    private static WirelessInfoMessage gatherNode(IWirelessNode node) {
        BlockEntity te = (BlockEntity) node;
        BlockPos p = te.getBlockPos();
        WirelessNet linked = WirelessHelper.getWirelessNet(node);

        List<Entry> avail = new ArrayList<>();
        for (WirelessNet net : WirelessHelper.getNetInRange(te.getLevel(),
                p.getX(), p.getY(), p.getZ(), node.getRange(), 20)) {
            if (net == linked) continue;
            avail.add(of(net));
        }
        return new WirelessInfoMessage(linked != null, linked != null ? of(linked) : NONE, avail);
    }

    public static void encode(WirelessInfoMessage m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.linked);
        writeEntry(buf, m.linkedNode);
        buf.writeVarInt(m.avail.size());
        for (Entry e : m.avail) writeEntry(buf, e);
    }

    public static WirelessInfoMessage decode(FriendlyByteBuf buf) {
        boolean linked = buf.readBoolean();
        Entry l = readEntry(buf);
        int n = buf.readVarInt();
        List<Entry> avail = new ArrayList<>(n);
        for (int i = 0; i < n; i++) avail.add(readEntry(buf));
        return new WirelessInfoMessage(linked, l, avail);
    }

    private static void writeEntry(FriendlyByteBuf buf, Entry e) {
        buf.writeBlockPos(e.pos());
        buf.writeUtf(e.name(), 64);
        buf.writeBoolean(e.encrypted());
    }

    private static Entry readEntry(FriendlyByteBuf buf) {
        return new Entry(buf.readBlockPos(), buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(WirelessInfoMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.academy.client.gui.WirelessInfoClient.accept(m)));
        ctx.setPacketHandled(true);
    }
}
