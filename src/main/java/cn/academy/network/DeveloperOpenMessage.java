package cn.academy.network;

import cn.academy.client.gui.developer.DeveloperClientOpener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class DeveloperOpenMessage {

    private final BlockPos pos;

    public DeveloperOpenMessage(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(DeveloperOpenMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
    }

    public static DeveloperOpenMessage decode(FriendlyByteBuf buf) {
        return new DeveloperOpenMessage(buf.readBlockPos());
    }

    public static void handle(DeveloperOpenMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DeveloperClientOpener.open(m.pos)));
        ctx.setPacketHandled(true);
    }

    public static void send(ServerPlayer player, BlockPos pos) {
        ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DeveloperOpenMessage(pos));
    }
}
