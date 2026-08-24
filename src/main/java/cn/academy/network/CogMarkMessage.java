package cn.academy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class CogMarkMessage {

    private final UUID target;

    public CogMarkMessage(UUID target) {
        this.target = target;
    }

    public static void encode(CogMarkMessage m, FriendlyByteBuf buf) {
        buf.writeUUID(m.target);
    }

    public static CogMarkMessage decode(FriendlyByteBuf buf) {
        return new CogMarkMessage(buf.readUUID());
    }

    public static void handle(CogMarkMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.academy.client.render.AllyMarkRender.begin(m.target)));
        ctx.setPacketHandled(true);
    }

    public static void send(ServerPlayer caster, Entity target) {
        if (caster == null || target == null) {
            return;
        }
        ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> caster),
                new CogMarkMessage(target.getUUID()));
    }
}
