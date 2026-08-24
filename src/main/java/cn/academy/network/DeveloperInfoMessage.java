package cn.academy.network;

import cn.academy.client.gui.developer.DeveloperInfoClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class DeveloperInfoMessage {

    @Nullable
    private final String nodeName;

    public DeveloperInfoMessage(@Nullable String nodeName) {
        this.nodeName = nodeName;
    }

    public static void encode(DeveloperInfoMessage m, FriendlyByteBuf buf) {
        buf.writeBoolean(m.nodeName != null);
        if (m.nodeName != null) {
            buf.writeUtf(m.nodeName);
        }
    }

    public static DeveloperInfoMessage decode(FriendlyByteBuf buf) {
        return new DeveloperInfoMessage(buf.readBoolean() ? buf.readUtf() : null);
    }

    public static void handle(DeveloperInfoMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DeveloperInfoClient.acceptNodeName(m.nodeName)));
        ctx.setPacketHandled(true);
    }

    public static void send(ServerPlayer player, @Nullable String nodeName) {
        ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DeveloperInfoMessage(nodeName));
    }
}
