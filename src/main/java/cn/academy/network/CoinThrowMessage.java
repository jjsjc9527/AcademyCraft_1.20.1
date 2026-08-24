package cn.academy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class CoinThrowMessage {

    private final int throwerId;

    private final int handOrdinal;

    private final ItemStack stack;

    public CoinThrowMessage(int throwerId, InteractionHand hand, ItemStack stack) {
        this.throwerId = throwerId;
        this.handOrdinal = hand.ordinal();
        this.stack = stack;
    }

    private CoinThrowMessage(int throwerId, int handOrdinal, ItemStack stack) {
        this.throwerId = throwerId;
        this.handOrdinal = handOrdinal;
        this.stack = stack;
    }

    public static void encode(CoinThrowMessage m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.throwerId);
        buf.writeVarInt(m.handOrdinal);
        buf.writeItem(m.stack);
    }

    public static CoinThrowMessage decode(FriendlyByteBuf buf) {
        return new CoinThrowMessage(buf.readVarInt(), buf.readVarInt(), buf.readItem());
    }

    public static void handle(CoinThrowMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CoinThrowClient.spawn(m.throwerId, m.handOrdinal, m.stack)));
        ctx.setPacketHandled(true);
    }

    public static void broadcast(ServerPlayer thrower, InteractionHand hand, ItemStack stack) {
        ACNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> thrower),
                new CoinThrowMessage(thrower.getId(), hand, stack));
    }
}
