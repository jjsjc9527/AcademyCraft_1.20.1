package cn.academy.network;

import cn.academy.block.tileentity.AbilityInterfererBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class InterfererInfoMessage {

    private final BlockPos pos;
    private final boolean enabled;
    private final float range;
    private final String[] names;

    public InterfererInfoMessage(BlockPos pos, boolean enabled, float range, String[] names) {
        this.pos = pos;
        this.enabled = enabled;
        this.range = range;
        this.names = names == null ? new String[0] : names;
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public float getRange() {
        return range;
    }

    public String[] getNames() {
        return names;
    }

    public static void encode(InterfererInfoMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeBoolean(m.enabled);
        buf.writeFloat(m.range);
        buf.writeVarInt(m.names.length);
        for (String s : m.names) buf.writeUtf(s, 40);
    }

    public static InterfererInfoMessage decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean enabled = buf.readBoolean();
        float range = buf.readFloat();
        int n = buf.readVarInt();
        String[] names = new String[n];
        for (int i = 0; i < n; i++) names[i] = buf.readUtf(40);
        return new InterfererInfoMessage(pos, enabled, range, names);
    }

    public static void handle(InterfererInfoMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> cn.academy.client.gui.InterfererInfoClient.accept(m)));
        ctx.setPacketHandled(true);
    }

    public static InterfererInfoMessage gather(AbilityInterfererBlockEntity be) {
        return new InterfererInfoMessage(be.getBlockPos(), be.isEnabled(),
                (float) be.getRange(), be.getWhitelist());
    }

    public static void sendToPlayer(AbilityInterfererBlockEntity be, ServerPlayer player) {
        ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), gather(be));
    }

    public static void sendTracking(AbilityInterfererBlockEntity be) {
        if (be.getLevel() == null) return;
        ACNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> be.getLevel().getChunkAt(be.getBlockPos())),
                gather(be));
    }
}
