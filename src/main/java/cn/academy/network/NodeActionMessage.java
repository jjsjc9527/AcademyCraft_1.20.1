package cn.academy.network;

import cn.academy.block.tileentity.WirelessNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class NodeActionMessage {

    public static final byte GATHER = 0;
    public static final byte RENAME = 1;
    public static final byte CHANGE_PASS = 2;

    private final BlockPos pos;
    private final byte action;
    private final String text;

    public NodeActionMessage(BlockPos pos, byte action, String text) {
        this.pos = pos;
        this.action = action;
        this.text = text == null ? "" : text;
    }

    public static void encode(NodeActionMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeByte(m.action);
        buf.writeUtf(m.text, 64);
    }

    public static NodeActionMessage decode(FriendlyByteBuf buf) {
        return new NodeActionMessage(buf.readBlockPos(), buf.readByte(), buf.readUtf(64));
    }

    public static void handle(NodeActionMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(m.pos) instanceof WirelessNodeBlockEntity be)) return;

            boolean isPlacer = be.getPlacerName().equals(player.getName().getString());
            switch (m.action) {
                case RENAME -> { if (isPlacer) be.setNodeName(m.text); }
                case CHANGE_PASS -> { if (isPlacer) be.setPassword(m.text); }
                default -> {  }
            }

            ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), NodeInfoMessage.gather(be));
        });
        ctx.setPacketHandled(true);
    }

    public static void send(BlockPos pos, byte action, String text) {
        ACNetwork.CHANNEL.sendToServer(new NodeActionMessage(pos, action, text));
    }
}
