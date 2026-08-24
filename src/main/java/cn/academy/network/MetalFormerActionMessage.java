package cn.academy.network;

import cn.academy.block.tileentity.MetalFormerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MetalFormerActionMessage {

    private final BlockPos pos;

    private final int delta;

    public MetalFormerActionMessage(BlockPos pos, int delta) {
        this.pos = pos;
        this.delta = delta;
    }

    public static void encode(MetalFormerActionMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeByte(m.delta);
    }

    public static MetalFormerActionMessage decode(FriendlyByteBuf buf) {
        return new MetalFormerActionMessage(buf.readBlockPos(), buf.readByte());
    }

    public static void handle(MetalFormerActionMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (player.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY() + 0.5, m.pos.getZ() + 0.5) > 64.0) return;
            if (player.level().getBlockEntity(m.pos) instanceof MetalFormerBlockEntity be) {
                be.cycleMode(Integer.signum(m.delta));
            }
        });
        ctx.setPacketHandled(true);
    }

    public static void send(BlockPos pos, int delta) {
        ACNetwork.CHANNEL.sendToServer(new MetalFormerActionMessage(pos, delta));
    }
}
