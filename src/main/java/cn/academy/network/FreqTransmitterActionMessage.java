package cn.academy.network;

import cn.academy.block.block.ACMultiBlock;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.impl.WirelessNet;
import cn.academy.event.energy.LinkNodeEvent;
import cn.academy.event.energy.LinkUserEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class FreqTransmitterActionMessage {

    public static final byte QUERY_SSID  = 0;
    public static final byte AUTH_MATRIX = 1;
    public static final byte AUTH_NODE   = 2;
    public static final byte LINK_NODE   = 3;
    public static final byte LINK_USER   = 4;

    private final int reqId;
    private final byte action;
    private final BlockPos pos;
    private final BlockPos target;
    private final String password;

    public FreqTransmitterActionMessage(int reqId, byte action, BlockPos pos, BlockPos target, String password) {
        this.reqId = reqId;
        this.action = action;
        this.pos = pos;
        this.target = target == null ? BlockPos.ZERO : target;
        this.password = password == null ? "" : password;
    }

    public static void encode(FreqTransmitterActionMessage m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.reqId);
        buf.writeByte(m.action);
        buf.writeBlockPos(m.pos);
        buf.writeBlockPos(m.target);
        buf.writeUtf(m.password, 64);
    }

    public static FreqTransmitterActionMessage decode(FriendlyByteBuf buf) {
        return new FreqTransmitterActionMessage(
                buf.readVarInt(), buf.readByte(), buf.readBlockPos(), buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(FreqTransmitterActionMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Level level = player.level();

            boolean success = false;
            String ssid = "";

            switch (m.action) {
                case QUERY_SSID -> {
                    if (resolve(level, m.pos) instanceof IWirelessMatrix mat) {
                        WirelessNet net = WirelessHelper.getWirelessNet(mat);
                        if (net != null) {
                            success = true;
                            ssid = net.getSSID();
                        }
                    }
                }
                case AUTH_MATRIX -> {
                    if (resolve(level, m.pos) instanceof IWirelessMatrix mat) {
                        WirelessNet net = WirelessHelper.getWirelessNet(mat);
                        success = net != null && net.getPassword().equals(m.password);
                    }
                }
                case AUTH_NODE -> {
                    if (resolve(level, m.pos) instanceof IWirelessNode node) {
                        success = node.getPassword().equals(m.password);
                    }
                }
                case LINK_NODE -> {
                    if (resolve(level, m.pos) instanceof IWirelessNode node
                            && resolve(level, m.target) instanceof IWirelessMatrix mat) {
                        WirelessNet net = WirelessHelper.getWirelessNet(mat);
                        if (net != null) {
                            success = !MinecraftForge.EVENT_BUS.post(
                                    new LinkNodeEvent(node, net.getMatrix(), m.password));
                        }
                    }
                }
                case LINK_USER -> {
                    if (resolve(level, m.pos) instanceof IWirelessUser user
                            && resolve(level, m.target) instanceof IWirelessNode node) {
                        success = !MinecraftForge.EVENT_BUS.post(new LinkUserEvent(user, node));
                    }
                }
                default -> {  }
            }

            ACNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new FreqTransmitterResultMessage(m.reqId, success, ssid));
        });
        ctx.setPacketHandled(true);
    }

    private static BlockEntity resolve(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) return be;
        if (level.getBlockState(pos).getBlock() instanceof ACMultiBlock) {
            return ACMultiBlock.getOriginTile(level, pos);
        }
        return null;
    }

    public static void send(byte action, BlockPos pos, BlockPos target, String password,
                            FreqTransmitterResultMessage.Callback cb) {
        int reqId = FreqTransmitterResultMessage.register(cb);
        ACNetwork.CHANNEL.sendToServer(new FreqTransmitterActionMessage(reqId, action, pos, target, password));
    }
}
