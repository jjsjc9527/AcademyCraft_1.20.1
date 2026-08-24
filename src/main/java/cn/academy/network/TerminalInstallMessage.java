package cn.academy.network;

import cn.academy.client.auxgui.TerminalInstallClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class TerminalInstallMessage {

    public TerminalInstallMessage() {}

    public static void encode(TerminalInstallMessage m, FriendlyByteBuf buf) {}

    public static TerminalInstallMessage decode(FriendlyByteBuf buf) {
        return new TerminalInstallMessage();
    }

    public static void handle(TerminalInstallMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> TerminalInstallClient::play));
        ctx.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player) {
        ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new TerminalInstallMessage());
    }
}
