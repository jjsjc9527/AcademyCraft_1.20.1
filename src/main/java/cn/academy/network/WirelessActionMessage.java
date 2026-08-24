package cn.academy.network;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.event.energy.LinkNodeEvent;
import cn.academy.event.energy.LinkUserEvent;
import cn.academy.event.energy.UnlinkNodeEvent;
import cn.academy.event.energy.UnlinkUserEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class WirelessActionMessage {

    public static final byte GATHER = 0;
    public static final byte CONNECT = 1;
    public static final byte DISCONNECT = 2;

    private final BlockPos pos;
    private final byte action;
    private final BlockPos target;
    private final String password;

    public WirelessActionMessage(BlockPos pos, byte action, BlockPos target, String password) {
        this.pos = pos;
        this.action = action;
        this.target = target == null ? BlockPos.ZERO : target;
        this.password = password == null ? "" : password;
    }

    public static void encode(WirelessActionMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeByte(m.action);
        buf.writeBlockPos(m.target);
        buf.writeUtf(m.password, 64);
    }

    public static WirelessActionMessage decode(FriendlyByteBuf buf) {
        return new WirelessActionMessage(buf.readBlockPos(), buf.readByte(), buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(WirelessActionMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(m.pos) instanceof IWirelessTile tile)) return;

            if (tile instanceof IWirelessNode node) {
                switch (m.action) {
                    case CONNECT -> {
                        if (player.level().getBlockEntity(m.target) instanceof IWirelessMatrix mat) {
                            MinecraftForge.EVENT_BUS.post(new LinkNodeEvent(node, mat, m.password));
                        }
                    }
                    case DISCONNECT -> MinecraftForge.EVENT_BUS.post(new UnlinkNodeEvent(node));
                    default -> {  }
                }
            } else if (tile instanceof IWirelessUser user) {
                switch (m.action) {
                    case CONNECT -> {
                        if (player.level().getBlockEntity(m.target) instanceof IWirelessNode node) {
                            MinecraftForge.EVENT_BUS.post(new LinkUserEvent(user, node, m.password));
                        }
                    }
                    case DISCONNECT -> MinecraftForge.EVENT_BUS.post(new UnlinkUserEvent(user));
                    default -> {  }
                }
            }

            ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    WirelessInfoMessage.gather(tile));
        });
        ctx.setPacketHandled(true);
    }

    public static void send(BlockPos pos, byte action, BlockPos target, String password) {
        ACNetwork.CHANNEL.sendToServer(new WirelessActionMessage(pos, action, target, password));
    }
}
