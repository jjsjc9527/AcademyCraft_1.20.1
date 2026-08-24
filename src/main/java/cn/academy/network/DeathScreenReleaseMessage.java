package cn.academy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class DeathScreenReleaseMessage {

    public DeathScreenReleaseMessage() {}

    public static void encode(DeathScreenReleaseMessage m, FriendlyByteBuf buf) {}

    public static DeathScreenReleaseMessage decode(FriendlyByteBuf buf) {
        return new DeathScreenReleaseMessage();
    }

    public static void handle(DeathScreenReleaseMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.academy.client.ClientDeathScreenFix.release()));
        ctx.setPacketHandled(true);
    }

    public static void send(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DeathScreenReleaseMessage());
    }
}
