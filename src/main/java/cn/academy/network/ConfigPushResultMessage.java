package cn.academy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class ConfigPushResultMessage {

    private final String file;
    private final int applied;
    private final int rejected;
    private final String badPath;
    private final byte reason;

    public ConfigPushResultMessage(String file, int applied, int rejected,
                                   String badPath, byte reason) {
        this.file = file;
        this.applied = applied;
        this.rejected = rejected;
        this.badPath = badPath;
        this.reason = reason;
    }

    public static void encode(ConfigPushResultMessage m, FriendlyByteBuf buf) {
        buf.writeUtf(m.file, 64);
        buf.writeVarInt(m.applied);
        buf.writeVarInt(m.rejected);
        buf.writeUtf(m.badPath, 128);
        buf.writeByte(m.reason);
    }

    public static ConfigPushResultMessage decode(FriendlyByteBuf buf) {
        return new ConfigPushResultMessage(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(),
                buf.readUtf(128), buf.readByte());
    }

    public static void handle(ConfigPushResultMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.academy.client.gui.config.ACConfigSources.acceptPushResult(
                        m.file, m.applied, m.rejected, m.badPath, m.reason)));
        ctx.setPacketHandled(true);
    }

    public static void send(ServerPlayer player, String file, int applied, int rejected,
                            String badPath, byte reason) {
        ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ConfigPushResultMessage(file, applied, rejected, badPath, reason));
    }
}
