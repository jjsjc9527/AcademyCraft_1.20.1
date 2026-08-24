package cn.academy.network;

import cn.academy.block.tileentity.AbilityInterfererBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.function.Supplier;

public class InterfererActionMessage {

    public static final byte GATHER = 0;
    public static final byte SET_ENABLED = 1;
    public static final byte SET_RANGE = 2;
    public static final byte SET_WHITELIST = 3;

    private final BlockPos pos;
    private final byte action;
    private final boolean enabled;
    private final double range;
    private final String[] names;

    public InterfererActionMessage(BlockPos pos, byte action, boolean enabled, double range, String[] names) {
        this.pos = pos;
        this.action = action;
        this.enabled = enabled;
        this.range = range;
        this.names = names == null ? new String[0] : names;
    }

    public static void encode(InterfererActionMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeByte(m.action);
        buf.writeBoolean(m.enabled);
        buf.writeDouble(m.range);
        buf.writeVarInt(m.names.length);
        for (String s : m.names) buf.writeUtf(s, 40);
    }

    public static InterfererActionMessage decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        byte action = buf.readByte();
        boolean enabled = buf.readBoolean();
        double range = buf.readDouble();
        int n = buf.readVarInt();
        String[] names = new String[n];
        for (int i = 0; i < n; i++) names[i] = buf.readUtf(40);
        return new InterfererActionMessage(pos, action, enabled, range, names);
    }

    public static void handle(InterfererActionMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(m.pos) instanceof AbilityInterfererBlockEntity be)) return;

            switch (m.action) {
                case SET_ENABLED -> be.setEnabled(m.enabled);
                case SET_RANGE -> be.setRange(m.range);
                case SET_WHITELIST -> be.setWhitelist(Arrays.asList(m.names));
                default -> {  }
            }

            InterfererInfoMessage.sendToPlayer(be, player);
        });
        ctx.setPacketHandled(true);
    }

    public static void send(BlockPos pos, byte action, boolean enabled, double range, String[] names) {
        ACNetwork.CHANNEL.sendToServer(new InterfererActionMessage(pos, action, enabled, range, names));
    }
}
