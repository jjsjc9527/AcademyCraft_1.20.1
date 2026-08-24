package cn.academy.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GroundHeaveMessage {

    private static final double RADIUS = 64.0;

    private final int lifetime;
    private final List<BlockPos> positions;

    private final List<Integer> heights;

    private GroundHeaveMessage(int lifetime, List<BlockPos> positions, List<Integer> heights) {
        this.lifetime = lifetime;
        this.positions = positions;
        this.heights = heights;
    }

    public static void encode(GroundHeaveMessage m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.lifetime);
        buf.writeVarInt(m.positions.size());
        for (int i = 0; i < m.positions.size(); i++) {
            buf.writeBlockPos(m.positions.get(i));
            buf.writeByte(m.heights.get(i));
        }
    }

    public static GroundHeaveMessage decode(FriendlyByteBuf buf) {
        int lifetime = buf.readVarInt();
        int n = buf.readVarInt();
        List<BlockPos> pos = new ArrayList<>(n);
        List<Integer> hs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            pos.add(buf.readBlockPos());
            hs.add((int) buf.readByte());
        }
        return new GroundHeaveMessage(lifetime, pos, hs);
    }

    public static void handle(GroundHeaveMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> cn.academy.client.render.GroundHeaveClient.accept(
                        m.lifetime, m.positions, m.heights)));
        ctx.setPacketHandled(true);
    }

    public static void broadcast(ServerLevel level, Vec3 center,
                                 List<BlockPos> positions, List<Double> heights) {
        if (positions.isEmpty()) {
            return;
        }

        List<Integer> bytes = new ArrayList<>(heights.size());
        for (double h : heights) {
            bytes.add(Math.max(0, Math.min(127, (int) Math.round(h * 100))));
        }
        ACNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, RADIUS, level.dimension())),
                new GroundHeaveMessage(cn.academy.ability.vanilla.vecmanip.skill.GroundHeave.LIFETIME,
                        positions, bytes));
    }
}
