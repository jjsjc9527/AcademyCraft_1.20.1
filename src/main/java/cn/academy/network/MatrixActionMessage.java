package cn.academy.network;

import cn.academy.block.tileentity.WirelessMatrixBlockEntity;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.impl.WirelessNet;
import cn.academy.event.energy.ChangePassEvent;
import cn.academy.event.energy.CreateNetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class MatrixActionMessage {

    public static final byte GATHER = 0;
    public static final byte INIT = 1;
    public static final byte CHANGE_SSID = 2;
    public static final byte CHANGE_PASS = 3;

    private final BlockPos pos;
    private final byte action;
    private final String ssid;
    private final String pass;

    public MatrixActionMessage(BlockPos pos, byte action, String ssid, String pass) {
        this.pos = pos;
        this.action = action;
        this.ssid = ssid == null ? "" : ssid;
        this.pass = pass == null ? "" : pass;
    }

    public static void encode(MatrixActionMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeByte(m.action);
        buf.writeUtf(m.ssid, 64);
        buf.writeUtf(m.pass, 64);
    }

    public static MatrixActionMessage decode(FriendlyByteBuf buf) {
        return new MatrixActionMessage(buf.readBlockPos(), buf.readByte(), buf.readUtf(64), buf.readUtf(64));
    }

    public static void handle(MatrixActionMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(m.pos) instanceof WirelessMatrixBlockEntity be)) return;

            boolean isPlacer = be.getPlacerName().equals(player.getName().getString());

            switch (m.action) {
                case INIT -> {
                    if (isPlacer && !WirelessHelper.isMatrixActive(be)) {
                        MinecraftForge.EVENT_BUS.post(new CreateNetworkEvent(be, m.ssid, m.pass));
                    }
                }
                case CHANGE_SSID -> {
                    if (isPlacer) {
                        WirelessNet net = WirelessHelper.getWirelessNet(be);
                        if (net != null) net.setSSID(m.ssid);
                    }
                }
                case CHANGE_PASS -> {
                    if (isPlacer) {
                        MinecraftForge.EVENT_BUS.post(new ChangePassEvent(be, m.pass));
                    }
                }
                default -> {  }
            }

            ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    MatrixInfoMessage.gather(be));
        });
        ctx.setPacketHandled(true);
    }

    public static void send(BlockPos pos, byte action, String ssid, String pass) {
        ACNetwork.CHANNEL.sendToServer(new MatrixActionMessage(pos, action, ssid, pass));
    }
}
